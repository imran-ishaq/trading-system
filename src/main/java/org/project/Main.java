package org.project;

import org.project.enums.OrderType;
import org.project.exceptions.OrderException;
import org.project.impl.InMemoryOrderManager;
import org.project.impl.MockMarketDataProvider;
import org.project.impl.SimpleTradingEngine;
import org.project.utils.CompositeInstrument;
import org.project.utils.Instrument;
import org.project.utils.InstrumentComponent;
import org.project.utils.Order;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class Main {
    public static void main(String[] args) {
        // Initialize mock market prices
        MockMarketDataProvider marketDataProvider = new MockMarketDataProvider(Map.of(
                "1", 150.0, // AAPL
                "2", 2000.0 // GOOG
        ));

        // Create an InMemoryOrderManager
        InMemoryOrderManager orderManager = new InMemoryOrderManager();

        // Create a SimpleTradingEngine
        SimpleTradingEngine tradingEngine = new SimpleTradingEngine(orderManager, marketDataProvider);

        // Create some dummy instruments
        Instrument stock1 = new Instrument("1", "AAPL");
        Instrument stock2 = new Instrument("2", "GOOG");

        // Create a CompositeInstrument
        CompositeInstrument basket = new CompositeInstrument("3", "Basket", List.of(
                new InstrumentComponent(stock1, 0.5),
                new InstrumentComponent(stock2, 0.5)
        ));

        // Create some dummy orders
        Order buyOrder1 = new Order("1", "Trader1", OrderType.BUY, stock1, 100, 150.0);
//        Order sellOrder1 = new Order("2", "Trader2", OrderType.SELL, stock1, 50, 155.0);
//        Order buyOrder2 = new Order("3", "Trader3", OrderType.BUY, basket, 200, null);
        Order sellOrder2 = new Order("4", "Trader4", OrderType.SELL, basket, 100, null);

        // Add orders to the order manager
        try {
            orderManager.addOrder(buyOrder1);
//            orderManager.addOrder(sellOrder1);
//            orderManager.addOrder(buyOrder2);
            orderManager.addOrder(sellOrder2);
        } catch (OrderException e) {
            System.out.println("Error adding orders: " + e.getMessage());
        }

        // Match orders
        tradingEngine.matchOrders("1");
        tradingEngine.matchOrders("3");

        // Print remaining orders
        System.out.println("Remaining orders:");
        List<Order> remainingOrders = new ArrayList<>();
        remainingOrders.addAll(orderManager.getOrders("1"));
        remainingOrders.addAll(orderManager.getOrders("3"));
        for (Order order : remainingOrders) {
            System.out.println(order.getId() + " - " + order.getTraderId() + " - " + order.getType() +
                    " - " + order.getQuantity() + " - " + order.getPrice());
        }
    }
}
