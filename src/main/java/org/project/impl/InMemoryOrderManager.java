package org.project.impl;

import org.project.enums.OrderType;
import org.project.utils.CompositeInstrument;
import org.project.utils.InstrumentComponent;
import org.project.utils.Order;
import org.project.exceptions.OrderException;
import org.project.interfaces.OrderManager;

import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

// Order Manager (simplified)
public class InMemoryOrderManager implements OrderManager {
    private static final Logger LOGGER = Logger.getLogger(InMemoryOrderManager.class.getName());

    private final Map<String, Order> orders = new LinkedHashMap<>();

    @Override
    public void addOrder(Order order) throws OrderException {
        if (!isValidOrder(order)) {
            throw new OrderException("Invalid order: " + order.getId());
        }
        String orderId = order.getId();
        orders.put(orderId, order);
    }

    @Override
    public void cancelOrder(String orderId) throws OrderException {
        if (!orders.containsKey(orderId)) {
            throw new OrderException("Order not found: " + orderId);
        }
        orders.remove(orderId);
    }

    @Override
    public List<Order> getOrders(String instrumentId) {
        return orders.values().stream()
                .filter(order -> order.getInstrument().getId().equals(instrumentId) ||
                        (order.isCompositeOrder() && ((CompositeInstrument) order.getInstrument()).getComponents().stream()
                                .anyMatch(component -> component.getInstrument().getId().equals(instrumentId))))
                .toList();
    }

    @Override
    public List<Order> getOrders(String instrumentId, OrderType orderType) {
        return orders.values().stream()
                .filter(order -> order.getInstrument().getId().equals(instrumentId) &&
                        order.getType() == orderType ||
                (order.isCompositeOrder() && ((CompositeInstrument) order.getInstrument()).getComponents().stream()
                        .anyMatch(component -> component.getInstrument().getId().equals(instrumentId))))
                .toList();
    }

    private boolean isValidOrder(Order order) {
        // Check if order ID is null or empty
        if (order.getId() == null || order.getId().isEmpty()) {
            LOGGER.log(Level.SEVERE,"Invalid order: Order ID is null or empty.");
            return false;
        }

        // Check if trader ID is null or empty
        if (order.getTraderId() == null || order.getTraderId().isEmpty()) {
            LOGGER.log(Level.SEVERE,"Invalid order: Trader ID is null or empty.");
            return false;
        }

        // Check if order type is null
        if (order.getType() == null) {
            LOGGER.log(Level.SEVERE,"Invalid order: Order type is null.");
            return false;
        }

        // Check if instrument is null
        if (order.getInstrument() == null) {
            LOGGER.log(Level.SEVERE,"Invalid order: Instrument is null.");
            return false;
        }

        // Check if quantity is non-positive
        if (order.getQuantity() <= 0) {
            LOGGER.log(Level.SEVERE,"Invalid order: Quantity is non-positive.");
            return false;
        }

        // Check if composite order instrment size is not between 1 and 3 and it's not adirect trade
        if(order.isCompositeOrder()){
            CompositeInstrument compositeInstrument = (CompositeInstrument) order.getInstrument();
            List<InstrumentComponent> instruments = compositeInstrument.getComponents();
            if (instruments.isEmpty() || instruments.size() >= 3) {
                LOGGER.log(Level.SEVERE, "Composite Instrument must contain 1 to 3 Instruments");
                return false;
            }
        }
        // If all conditions pass, the order is considered valid
        return true;
    }


}
