package org.project.interfaces;

import org.project.enums.OrderType;
import org.project.utils.Order;
import org.project.exceptions.OrderException;

import java.util.List;

public interface OrderManager {
    String addOrder(Order order) throws OrderException;
    void cancelOrder(String orderId) throws OrderException;
    List<Order> getOrders(String instrumentId);
    List<Order> getOrders(String instrumentId, OrderType orderType);
}