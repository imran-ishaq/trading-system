package org.project.impl;

import org.project.enums.OrderStatus;
import org.project.enums.OrderType;
import org.project.interfaces.MarketDataProvider;
import org.project.interfaces.OrderManager;
import org.project.interfaces.TradingEngine;
import org.project.utils.Order;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

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
        // Implement logic to decompose composite orders into individual orders for their underlying instruments
        // Match those individual orders with existing orders in the order book
        // If all underlying orders are filled, mark the composite order as filled
        LOGGER.log(Level.INFO, "Handling composite order trade for orders " + buyOrder.getId() + " and " + sellOrder.getId());
        // TODO: Implement composite order handling logic
    }
    private boolean canExecuteTrade(Order buyOrder, Order sellOrder) {
        // Check if buy and sell orders are for the same instrument
        if (!buyOrder.getInstrument().getId().equals(sellOrder.getInstrument().getId())) {
            LOGGER.log(Level.SEVERE,String.format("Orders are not for the same instrument. Trade cannot be executed for order %s and %s.", buyOrder.getId(), sellOrder.getId()));
            return false;
        }

//        if(buyOrder.isCompositeOrder()){
//            LOGGER.log(Level.SEVERE,"Composite orders cannot be traded directly");
//            return false;
//        }
        // Check if both orders are active (status is PENDING or PARTIALLY_FILLED) and have positive quantities
        if ((buyOrder.getStatus() != OrderStatus.PENDING && buyOrder.getStatus() != OrderStatus.PARTIALLY_FILLED) ||
                (sellOrder.getStatus() != OrderStatus.PENDING && sellOrder.getStatus() != OrderStatus.PARTIALLY_FILLED) ||
                buyOrder.getQuantity() <= 0 || sellOrder.getQuantity() <= 0) {
            LOGGER.log(Level.SEVERE,String.format("One of the order is already fulfilled %s and %s.", buyOrder.getId(), sellOrder.getId()));
            return false;
        }

        // Check if buy price is greater than or equal to sell price
        double buyPrice = buyOrder.getPrice() != null ? buyOrder.getPrice() : marketDataProvider.getMarketPrice(buyOrder.getInstrument().getId());
        double sellPrice = sellOrder.getPrice() != null ? sellOrder.getPrice() : marketDataProvider.getMarketPrice(sellOrder.getInstrument().getId());
        if (buyPrice < sellPrice) {
            LOGGER.log(Level.SEVERE,String.format("buy price: %s < and sell price: %s.", buyOrder.getPrice(), sellOrder.getPrice()));
            return false;
        }

        return true;
    }


    @Override
    public void executeTrade(Order buyOrder, Order sellOrder) {
        // Check if the orders are for the same instrument
        if (!buyOrder.getInstrument().getId().equals(sellOrder.getInstrument().getId())) {
            LOGGER.log(Level.SEVERE,String.format("Orders are not for the same instrument. Trade cannot be executed for order %s and %s.", buyOrder.getId(), sellOrder.getId()));
            return;
        }

        // Determine the trade quantity based on the minimum of the buy and sell order quantities
        int tradeQuantity = Math.min(buyOrder.getQuantity(), sellOrder.getQuantity());

        // Calculate the trade price as the best available market price
        double tradePrice = marketDataProvider.getMarketPrice(buyOrder.getInstrument().getId());

        // Execute the trade
        LOGGER.log(Level.INFO,"Trade executed between " + buyOrder.getTraderId() + " and " + sellOrder.getTraderId() +
                " for instrument " + buyOrder.getInstrument().getSymbol() +
                " at price " + tradePrice + " and quantity " + tradeQuantity);

        // Adjust the order quantities
        adjustOrderQuantities(buyOrder, sellOrder, tradeQuantity);

        // Update the status of buy and sell orders
        updateOrderStatus(buyOrder, sellOrder, tradeQuantity);
    }

    private void updateOrderStatus(Order buyOrder, Order sellOrder, int tradeQuantity) {
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

    private void adjustOrderQuantities(Order buyOrder, Order sellOrder, int tradeQuantity) {
        buyOrder.setQuantity(buyOrder.getQuantity() - tradeQuantity);
        sellOrder.setQuantity(sellOrder.getQuantity() - tradeQuantity);
    }
}
