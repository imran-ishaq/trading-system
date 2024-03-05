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

import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Main {
    private static final Logger LOGGER = Logger.getLogger(Main.class.getName());

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
        CompositeInstrument basket = null;
        try {
             basket = new CompositeInstrument("3", "Basket", List.of(
                    new InstrumentComponent(stock1, 0.5),
                    new InstrumentComponent(stock2, 0.5)
            ));
        }
        catch (OrderException exception){
            LOGGER.log(Level.SEVERE, exception.getMessage());
        }
        // Create a CompositeInstrument


        // Create some dummy orders
        Order buyOrder1 = new Order("1", "Trader1", OrderType.BUY, stock1, 100.0, 155.0);
        Order sellOrder1 = new Order("2", "Trader2", OrderType.SELL, stock1, 100.0, 155.0);
        Order buyOrder2 = new Order("3", "Trader3", OrderType.BUY, basket, 200.0, null);
        Order sellOrder2 = new Order("4", "Trader4", OrderType.SELL, basket, 100.0, null);
        Order buyOrder3 = new Order("5", "Trader2", OrderType.BUY, stock2, 50.0, 155.0);

        // Add orders to the order manager
        makeOrders(orderManager, buyOrder1);
        makeOrders(orderManager, buyOrder2);
        makeOrders(orderManager, sellOrder1);
        makeOrders(orderManager, sellOrder2);
        makeOrders(orderManager, buyOrder3);

        // Match orders
        tradingEngine.matchOrders("1");
        tradingEngine.matchOrders("3");
        tradingEngine.matchOrders("2");
        // Print remaining orders
        LOGGER.log(Level.SEVERE, "Remaining orders:");
    }

    private static void makeOrders(InMemoryOrderManager orderManager, Order order) {
        try {
            orderManager.addOrder(order);
        } catch (OrderException e) {
            LOGGER.log(Level.SEVERE,String.format("Error adding orders: %s", e.getMessage()));
        }
    }
}
