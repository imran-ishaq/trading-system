package org.project.interfaces;

import org.project.utils.Order;

public interface TradingEngine {
    void matchOrders(String instrumentId);
    void executeTrade(Order buyOrder, Order sellOrder);
}