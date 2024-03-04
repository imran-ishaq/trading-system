package org.project.impl;

import org.project.enums.OrderType;
import org.project.exceptions.OrderException;
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
                    executeTrade(buyOrder, sellOrder);
                }
            }
        }
    }

    private boolean canExecuteTrade(Order buyOrder, Order sellOrder) {
        double buyPrice = buyOrder.getPrice() != null ? buyOrder.getPrice() : marketDataProvider.getMarketPrice(buyOrder.getInstrument().getId());
        double sellPrice = sellOrder.getPrice() != null ? sellOrder.getPrice() : marketDataProvider.getMarketPrice(sellOrder.getInstrument().getId());
        return buyPrice >= sellPrice;
    }

    @Override
    public void executeTrade(Order buyOrder, Order sellOrder) {
        // Check if the orders are for the same instrument
        if (!buyOrder.getInstrument().getId().equals(sellOrder.getInstrument().getId())) {
            LOGGER.log(Level.INFO,String.format("Orders are not for the same instrument. Trade cannot be executed for order %s and %s.", buyOrder.getId(), sellOrder.getId()));
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

        // Update the order book or remove orders if fully filled
        try {
            orderManager.cancelOrder(buyOrder.getId());
            orderManager.cancelOrder(sellOrder.getId());
        } catch (OrderException e) {
            LOGGER.log(Level.INFO,"Error cancelling orders after trade execution: " + e.getMessage());
        }
    }
}
