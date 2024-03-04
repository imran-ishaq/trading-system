package org.project.impl;

import org.project.enums.OrderType;
import org.project.utils.CompositeInstrument;
import org.project.utils.Order;
import org.project.exceptions.OrderException;
import org.project.interfaces.OrderManager;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

// Order Manager (simplified)
public class InMemoryOrderManager implements OrderManager {

    private final Map<String, Order> orders = new HashMap<>();

    @Override
    public void addOrder(Order order) throws OrderException {
        if (!isValidOrder(order)) {
            throw new OrderException("Invalid order: " + order);
        }
        String orderId = generateOrderId();
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
                        order.getType() == orderType)
                .toList();
    }

    private boolean isValidOrder(Order order) {
        // Check if order ID is null or empty
        if (order.getId() == null || order.getId().isEmpty()) {
            System.out.println("Invalid order: Order ID is null or empty.");
            return false;
        }

        // Check if trader ID is null or empty
        if (order.getTraderId() == null || order.getTraderId().isEmpty()) {
            System.out.println("Invalid order: Trader ID is null or empty.");
            return false;
        }

        // Check if order type is null
        if (order.getType() == null) {
            System.out.println("Invalid order: Order type is null.");
            return false;
        }

        // Check if instrument is null
        if (order.getInstrument() == null) {
            System.out.println("Invalid order: Instrument is null.");
            return false;
        }

        // Check if quantity is non-positive
        if (order.getQuantity() <= 0) {
            System.out.println("Invalid order: Quantity is non-positive.");
            return false;
        }

        // If all conditions pass, the order is considered valid
        return true;
    }


    private String generateOrderId() {
        // Implement your logic to generate order IDs here
        return UUID.randomUUID().toString(); // Placeholder logic, replace with actual ID generation
    }
}