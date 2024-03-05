package org.project;

import org.project.enums.OrderStatus;
import org.project.enums.OrderType;
import org.project.interfaces.MarketDataProvider;
import org.project.interfaces.OrderManager;
import org.project.interfaces.TradingEngine;
import org.project.utils.CompositeInstrument;
import org.project.utils.Instrument;
import org.project.utils.InstrumentComponent;
import org.project.utils.Order;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class SimpleTradingEngine implements TradingEngine {

    private static final Logger LOGGER = Logger.getLogger(SimpleTradingEngine.class.getName());

    private final OrderManager orderManager;
    private final MarketDataProvider marketDataProvider;

    public SimpleTradingEngine(OrderManager orderManager, MarketDataProvider marketDataProvider) {
        this.orderManager = orderManager;
        this.marketDataProvider = marketDataProvider;
    }

    @Override
    public void matchOrders(String instrumentId) {
        List<Order> buyOrders = orderManager.getOrders(instrumentId, OrderType.BUY);
        List<Order> sellOrders = orderManager.getOrders(instrumentId, OrderType.SELL);

        for (Order buyOrder : buyOrders) {
            for (Order sellOrder : sellOrders) {
                if (canExecuteTrade(buyOrder, sellOrder)) {
                    // Handle composite orders
                    if (buyOrder.isCompositeOrder() || sellOrder.isCompositeOrder()) {
                        handleCompositeOrderTrade(buyOrder, sellOrder);
                    } else {
                        executeTrade(buyOrder, sellOrder);
                    }
                }
            }
        }
    }

    private void handleCompositeOrderTrade(Order buyOrder, Order sellOrder) {
        // Extract underlying instruments from composite orders
        List<Instrument> buyComponents = ((CompositeInstrument) buyOrder.getInstrument()).getComponents().stream().map(InstrumentComponent::getInstrument)
                .collect(Collectors.toList());
        List<Instrument> sellComponents = ((CompositeInstrument) sellOrder.getInstrument()).getComponents().stream().map(InstrumentComponent::getInstrument)
                .toList();

        // Validate component count and instrument match
        if (buyComponents.size() != sellComponents.size() || !buyComponents.stream().allMatch(component -> sellComponents.stream().anyMatch(c -> c.getId().equals(component.getId())))) {
            return;
        }

        // Create and match sub-orders for each underlying instrument
        createExecuteSubOrders(buyOrder, buyComponents, sellOrder);

    }

    private void createExecuteSubOrders(Order parentOrder, List<Instrument> components, Order counterOrder) {
        List<Order> sellSubOrders = new ArrayList<>();
        List<Order> buySubOrders = new ArrayList<>();


        for (Instrument instrument : components) {
            double parentQuantity = calculateSubOrderQuantity(parentOrder, instrument);
            double counterQuantity = calculateSubOrderQuantity(counterOrder, instrument);

            // Create sub-orders for both parent and counter orders
            Order parentSubOrder = createSubOrder(parentOrder, instrument, parentQuantity);
            Order counterSubOrder = createSubOrder(counterOrder, instrument, counterQuantity);

            // Execute the trades for both sub-orders
            executeTrade(parentSubOrder, counterSubOrder);

            // Add the sub-orders to the list of sub-orders
            buySubOrders.add(parentSubOrder);
            sellSubOrders.add(counterSubOrder);
        }

        // Update the status of the parent order based on the status of sub-orders
        updateParentOrderStatus(parentOrder, buySubOrders);
        updateParentOrderStatus(counterOrder, sellSubOrders);
    }

    private double calculateSubOrderQuantity(Order order, Instrument instrument) {
        double quantity = 0.0;

        if (order.getInstrument() instanceof CompositeInstrument compositeInstrument) {

            // Get the weight of the component instrument in the composite instrument
            double componentWeight = compositeInstrument.getComponentWeight(instrument.getId());

            // Calculate the quantity based on the weight and the total quantity of the parent order
            quantity = componentWeight * order.getQuantity();
        }

        return quantity;
    }


    private Order createSubOrder(Order order, Instrument instrument, double quantity) {
        // Create a sub-order with appropriate properties
        Order subOrder = new Order(order.getId() + "_" + instrument.getId(), order.getTraderId(), order.getType(), order.getInstrument(), quantity, order.getPrice());
        subOrder.setInstrument(instrument);
        return subOrder;
    }

    private void updateParentOrderStatus(Order parentOrder, List<Order> subOrders) {
        // Check if all sub-orders are filled, partially filled, or pending
        boolean allFilled = subOrders.stream().allMatch(o -> o.getStatus() == OrderStatus.FILLED);
        boolean allPartiallyFilled = subOrders.stream().anyMatch(o -> o.getStatus() == OrderStatus.PARTIALLY_FILLED);

        // Update the status of the parent order
        if (allFilled) {
            parentOrder.setStatus(OrderStatus.FILLED);
        } else if (allPartiallyFilled) {
            parentOrder.setStatus(OrderStatus.PARTIALLY_FILLED);
        } else {
            parentOrder.setStatus(OrderStatus.PENDING);
        }
    }

    public boolean canExecuteTrade(Order buyOrder, Order sellOrder) {
        // Check if buy and sell orders are for the same instrument
        if (!buyOrder.getInstrument().getId().equals(sellOrder.getInstrument().getId())) {
            return false;
        }

        // Check if both orders are active (status is PENDING or PARTIALLY_FILLED) and have positive quantities
        if ((buyOrder.getStatus() != OrderStatus.PENDING && buyOrder.getStatus() != OrderStatus.PARTIALLY_FILLED) ||
                (sellOrder.getStatus() != OrderStatus.PENDING && sellOrder.getStatus() != OrderStatus.PARTIALLY_FILLED) ||
                buyOrder.getQuantity() <= 0 || sellOrder.getQuantity() <= 0) {
            return false;
        }

        // Check if buy price is greater than or equal to sell price
        double buyPrice = buyOrder.getPrice() != null ? buyOrder.getPrice() : marketDataProvider.getMarketPrice(buyOrder.getInstrument().getId());
        double sellPrice = sellOrder.getPrice() != null ? sellOrder.getPrice() : marketDataProvider.getMarketPrice(sellOrder.getInstrument().getId());
        return !(buyPrice < sellPrice);
    }


    @Override
    public void executeTrade(Order buyOrder, Order sellOrder) {
        // Check if the orders are for the same instrument
        if (!buyOrder.getInstrument().getId().equals(sellOrder.getInstrument().getId())) {
            return;
        }

        // Determine the trade quantity based on the minimum of the buy and sell order quantities
        double tradeQuantity = Math.min(buyOrder.getQuantity(), sellOrder.getQuantity());
        // Update the status of buy and sell orders
        updateOrderStatus(buyOrder, sellOrder, tradeQuantity);
        // Adjust the order quantities
        adjustOrderQuantities(buyOrder, sellOrder, tradeQuantity);
        LOGGER.log(Level.INFO, "Buy order status after matching: " + buyOrder.getStatus());
        LOGGER.log(Level.INFO, "Sell order status after matching: " + sellOrder.getStatus());

    }

    private void updateOrderStatus(Order buyOrder, Order sellOrder, double tradeQuantity) {
        if (tradeQuantity == buyOrder.getQuantity() && tradeQuantity == sellOrder.getQuantity()) {
            buyOrder.setStatus(OrderStatus.FILLED);
            sellOrder.setStatus(OrderStatus.FILLED);
        } else if (tradeQuantity < buyOrder.getQuantity() && tradeQuantity == sellOrder.getQuantity()) {
            buyOrder.setStatus(OrderStatus.PARTIALLY_FILLED);
            sellOrder.setStatus(OrderStatus.FILLED);
        } else if (tradeQuantity == buyOrder.getQuantity() && tradeQuantity < sellOrder.getQuantity()) {
            buyOrder.setStatus(OrderStatus.FILLED);
            sellOrder.setStatus(OrderStatus.PARTIALLY_FILLED);
        } else if (tradeQuantity < buyOrder.getQuantity() && tradeQuantity < sellOrder.getQuantity()) {
            buyOrder.setStatus(OrderStatus.PARTIALLY_FILLED);
            sellOrder.setStatus(OrderStatus.PARTIALLY_FILLED);
        }
    }

    private void adjustOrderQuantities(Order buyOrder, Order sellOrder, double tradeQuantity) {
        buyOrder.setQuantity(buyOrder.getQuantity() - tradeQuantity);
        sellOrder.setQuantity(sellOrder.getQuantity() - tradeQuantity);
    }
}
