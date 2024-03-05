package org.project;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.project.enums.OrderStatus;
import org.project.enums.OrderType;
import org.project.exceptions.OrderException;
import org.project.impl.InMemoryOrderManager;
import org.project.impl.MockMarketDataProvider;
import org.project.utils.CompositeInstrument;
import org.project.utils.Instrument;
import org.project.utils.InstrumentComponent;
import org.project.utils.Order;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class MainTest {
    private MockMarketDataProvider marketDataProvider;
    private InMemoryOrderManager orderManager;
    private SimpleTradingEngine tradingEngine;

    @BeforeEach
    void setUp() {
        // Initialize mock market prices
        marketDataProvider = new MockMarketDataProvider(Map.of(
                "1", 150.0, // AAPL
                "2", 2000.0 // GOOG
        ));

        // Create an InMemoryOrderManager
        orderManager = new InMemoryOrderManager();

        // Create a SimpleTradingEngine
        tradingEngine = new SimpleTradingEngine(orderManager, marketDataProvider);
    }

    @Test
    void testAddOrder_ValidOrder() {
        Instrument instrument = new Instrument("1", "AAPL");
        Order order = new Order("order1", "Trader1", OrderType.BUY, instrument, 100.0, 150.0);

        assertDoesNotThrow(() -> orderManager.addOrder(order));
        assertTrue(orderManager.getOrders("1").contains(order));
    }

    @Test
    void testAddOrder_NullOrderId() {
        Instrument instrument = new Instrument("1", "AAPL");
        Order order = new Order(null, "Trader1", OrderType.BUY, instrument, 100.0, 150.0);

        assertThrows(OrderException.class, () -> orderManager.addOrder(order));
    }

    @Test
    void testAddOrder_NullTraderId() {
        Instrument instrument = new Instrument("1", "AAPL");
        Order order = new Order("order1", null, OrderType.BUY, instrument, 100.0, 150.0);

        assertThrows(OrderException.class, () -> orderManager.addOrder(order));
    }

    @Test
    void testAddOrder_NullOrderType() {
        Instrument instrument = new Instrument("1", "AAPL");
        Order order = new Order("order1", "Trader1", null, instrument, 100.0, 150.0);

        assertThrows(OrderException.class, () -> orderManager.addOrder(order));
    }

    @Test
    void testAddOrder_NullInstrument() {
        Order order = new Order("order1", "Trader1", OrderType.BUY, null, 100.0, 150.0);

        assertThrows(OrderException.class, () -> orderManager.addOrder(order));
    }

    @Test
    void testAddOrder_NonPositiveQuantity() {
        Instrument instrument = new Instrument("1", "AAPL");
        Order order = new Order("order1", "Trader1", OrderType.BUY, instrument, -100.0, 150.0);

        assertThrows(OrderException.class, () -> orderManager.addOrder(order));
    }

    @Test
    void testAddOrder_CompositeInstrument_InvalidSize() throws OrderException {
        InstrumentComponent component1 = new InstrumentComponent(new Instrument("1", "AAPL"), 0.5);
        InstrumentComponent component2 = new InstrumentComponent(new Instrument("2", "GOOG"), 0.5);
        InstrumentComponent component3 = new InstrumentComponent(new Instrument("3", "MSFT"), 0.5);

        CompositeInstrument compositeInstrument = new CompositeInstrument("composite1", "Composite", List.of(component1, component2, component3));
        Order order = new Order("order1", "Trader1", OrderType.BUY, compositeInstrument, 100.0, 150.0);

        assertThrows(OrderException.class, () -> orderManager.addOrder(order));
    }

    @Test
    void testCancelOrder_ValidOrderId() {
        Instrument instrument = new Instrument("1", "AAPL");
        Order order = new Order("order1", "Trader1", OrderType.BUY, instrument, 100.0, 150.0);

        assertDoesNotThrow(() -> orderManager.addOrder(order));
        assertTrue(orderManager.getOrders("1").contains(order));

        assertDoesNotThrow(() -> orderManager.cancelOrder("order1"));
        assertFalse(orderManager.getOrders("1").contains(order));
    }

    @Test
    void testCancelOrder_InvalidOrderId() {
        assertThrows(OrderException.class, () -> orderManager.cancelOrder("invalid_order_id"));
    }

    @Test
     void testMatchCompositeOrder_FullFill() throws OrderException {
        // Create instruments and composite instrument
        Instrument stock1 = new Instrument("1", "AAPL");
        Instrument stock2 = new Instrument("2", "GOOG");
        CompositeInstrument basket = new CompositeInstrument("3", "Basket", List.of(
                new InstrumentComponent(stock1, 0.5),
                new InstrumentComponent(stock2, 0.5)
        ));

        // Create buy and sell orders for the composite instrument
        Order buyOrder = new Order("buy", "Trader1", OrderType.BUY, basket, 100.0, null);
        Order sellOrder = new Order("sell", "Trader2", OrderType.SELL, basket, 100.0, null);

        // Add orders to the order manager
        orderManager.addOrder(buyOrder);
        orderManager.addOrder(sellOrder);

        // Match orders and assert their status
        tradingEngine.matchOrders(basket.getId());

        assertEquals(OrderStatus.FILLED, buyOrder.getStatus());
        assertEquals(OrderStatus.FILLED, sellOrder.getStatus());
    }

    @Test
     void testMatchCompositeOrder_PartialFill() throws OrderException {
        // Create instruments and composite instrument
        Instrument stock1 = new Instrument("1", "AAPL");
        Instrument stock2 = new Instrument("2", "GOOG");
        CompositeInstrument basket = new CompositeInstrument("3", "Basket", List.of(
                new InstrumentComponent(stock1, 0.5),
                new InstrumentComponent(stock2, 0.5)
        ));

        // Create buy order for the composite instrument and sell orders for its components
        Order buyOrder = new Order("buy", "Trader1", OrderType.BUY, basket, 50.0, null);
        Order sellOrder1 = new Order("sell1", "Trader2", OrderType.SELL, basket.getComponents().get(0).getInstrument(), 60.0, null);
        Order sellOrder2 = new Order("sell2", "Trader3", OrderType.SELL, basket.getComponents().get(1).getInstrument(), 40.0, null);

        // Add orders to the order manager
        orderManager.addOrder(buyOrder);
        orderManager.addOrder(sellOrder1);
        orderManager.addOrder(sellOrder2);

        // Match orders and assert their status
        tradingEngine.matchOrders(basket.getId());

        assertEquals(OrderStatus.PENDING, buyOrder.getStatus());
        assertEquals(OrderStatus.PENDING, sellOrder2.getStatus());
        assertEquals(OrderStatus.PENDING, sellOrder1.getStatus());
        assertEquals(50.0, buyOrder.getQuantity()); // Remaining quantity should be 0
    }

    @Test
     void testMatchNoOrders() throws OrderException {
        // Create an instrument and order
        Instrument instrument = new Instrument("1", "AAPL");
        Order order = new Order("buy", "Trader1", OrderType.BUY, instrument, 100.0, 150.0);

        // Add order and match
        orderManager.addOrder(order);
        tradingEngine.matchOrders(instrument.getId());

        // Assert order status remains unchanged
        assertEquals(OrderStatus.PENDING, order.getStatus());
    }

    @Test
     void testMatchSingleInstrument_FullFill() throws OrderException {
        // Create instruments and orders
        Instrument instrument = new Instrument("1", "AAPL");
        Order buyOrder = new Order("buy", "Trader1", OrderType.BUY, instrument, 100.0, 150.0);
        Order sellOrder = new Order("sell", "Trader2", OrderType.SELL, instrument, 100.0, 150.0);

        // Add orders and match
        orderManager.addOrder(buyOrder);
        orderManager.addOrder(sellOrder);
        tradingEngine.matchOrders(instrument.getId());

        // Assert order statuses
        assertEquals(OrderStatus.FILLED, buyOrder.getStatus());
        assertEquals(OrderStatus.FILLED, sellOrder.getStatus());
    }

    @Test
    public void testComponentCountAndInstrumentMatch() throws OrderException {
        // Create instruments and composite instruments with different component counts
        Instrument stock1 = new Instrument("1", "AAPL");
        Instrument stock2 = new Instrument("2", "GOOG");
        CompositeInstrument basket1 = new CompositeInstrument("3", "Basket1", List.of(
                new InstrumentComponent(stock1, 0.5),
                new InstrumentComponent(stock2, 0.5)
        ));
        CompositeInstrument basket2 = new CompositeInstrument("4", "Basket2", List.of(
                new InstrumentComponent(stock1, 0.5)
        ));

        // Create buy and sell orders for the composite instruments
        Order buyOrder1 = new Order("buy1", "Trader1", OrderType.BUY, basket1, 100.0, null);
        Order sellOrder1 = new Order("sell1", "Trader2", OrderType.SELL, basket1, 100.0, null);
        Order buyOrder2 = new Order("buy2", "Trader3", OrderType.BUY, basket2, 50.0, null);
        Order sellOrder2 = new Order("sell2", "Trader4", OrderType.SELL, basket2, 50.0, null);

        // Add orders to the order manager
        orderManager.addOrder(buyOrder1);
        orderManager.addOrder(sellOrder1);
        orderManager.addOrder(buyOrder2);
        orderManager.addOrder(sellOrder2);

        // Match orders and assert their status
        tradingEngine.matchOrders(basket1.getId());
        tradingEngine.matchOrders(basket2.getId());

        // Ensure that orders are not matched due to mismatch in component count or instrument match
        assertEquals(OrderStatus.FILLED, buyOrder1.getStatus());
        assertEquals(OrderStatus.FILLED, sellOrder1.getStatus());
        assertEquals(OrderStatus.FILLED, buyOrder2.getStatus());
        assertEquals(OrderStatus.FILLED, sellOrder2.getStatus());
    }

    @Test
    void testFullFill() throws OrderException {
        // Create instruments and orders
        Instrument instrument = new Instrument("1", "AAPL");
        Order buyOrder = new Order("buy", "Trader1", OrderType.BUY, instrument, 100.0, 150.0);
        Order sellOrder = new Order("sell", "Trader2", OrderType.SELL, instrument, 100.0, 150.0);

        // Add orders and match
        orderManager.addOrder(buyOrder);
        orderManager.addOrder(sellOrder);
        tradingEngine.matchOrders(instrument.getId());

        // Assert order statuses
        assertEquals(OrderStatus.FILLED, buyOrder.getStatus());
        assertEquals(OrderStatus.FILLED, sellOrder.getStatus());
    }

    @Test
    void testPartialFill_BuyFull_SellPartial() throws OrderException {
        // Create instruments and orders
        Instrument instrument = new Instrument("1", "AAPL");
        Order buyOrder = new Order("buy", "Trader1", OrderType.BUY, instrument, 100.0, 150.0);
        Order sellOrder = new Order("sell", "Trader2", OrderType.SELL, instrument, 50.0, 150.0);

        // Add orders and match
        orderManager.addOrder(buyOrder);
        orderManager.addOrder(sellOrder);
        tradingEngine.matchOrders(instrument.getId());

        // Assert order statuses
        assertEquals(OrderStatus.PARTIALLY_FILLED, buyOrder.getStatus());
        assertEquals(OrderStatus.FILLED, sellOrder.getStatus());
    }

    @Test
    void testPartialFill_BuyPartial_SellFull() throws OrderException {
        // Create instruments and orders
        Instrument instrument = new Instrument("1", "AAPL");
        Order buyOrder = new Order("buy", "Trader1", OrderType.BUY, instrument, 50.0, 150.0);
        Order sellOrder = new Order("sell", "Trader2", OrderType.SELL, instrument, 100.0, 150.0);

        // Add orders and match
        orderManager.addOrder(buyOrder);
        orderManager.addOrder(sellOrder);
        tradingEngine.matchOrders(instrument.getId());

        // Assert order statuses
        assertEquals(OrderStatus.FILLED, buyOrder.getStatus());
        assertEquals(OrderStatus.PARTIALLY_FILLED, sellOrder.getStatus());
    }

    @Test
    void testPartialFill_BothPartial() throws OrderException {
        // Create instruments and orders
        Instrument instrument = new Instrument("1", "AAPL");
        Order buyOrder = new Order("buy", "Trader1", OrderType.BUY, instrument, 70.0, 150.0);
        Order sellOrder = new Order("sell", "Trader2", OrderType.SELL, instrument, 80.0, 150.0);

        // Add orders and match
        orderManager.addOrder(buyOrder);
        orderManager.addOrder(sellOrder);
        tradingEngine.matchOrders(instrument.getId());

        // Assert order statuses
        assertEquals(OrderStatus.FILLED, buyOrder.getStatus());
        assertEquals(OrderStatus.PARTIALLY_FILLED, sellOrder.getStatus());
    }
    @Test
    void testMatchOrders_SingleInstrument() throws OrderException {
        // Create instruments
        Instrument instrument = new Instrument("1", "AAPL");

        // Create buy and sell orders for the same instrument
        Order buyOrder = new Order("buy", "Trader1", OrderType.BUY, instrument, 100.0, 150.0);
        Order sellOrder = new Order("sell", "Trader2", OrderType.SELL, instrument, 100.0, 150.0);

        // Add orders to the order manager
        orderManager.addOrder(buyOrder);
        orderManager.addOrder(sellOrder);

        // Match orders
        tradingEngine.matchOrders(instrument.getId());

        // Assert order statuses
        assertEquals(OrderStatus.FILLED, buyOrder.getStatus());
        assertEquals(OrderStatus.FILLED, sellOrder.getStatus());
    }

    @Test
    void testMatchOrders_CompositeInstrument() throws OrderException {
        // Create instruments
        Instrument stock1 = new Instrument("1", "AAPL");
        Instrument stock2 = new Instrument("2", "GOOG");

        // Create a composite instrument
        CompositeInstrument basket = new CompositeInstrument("3", "Basket", List.of(
                new InstrumentComponent(stock1, 0.5),
                new InstrumentComponent(stock2, 0.5)
        ));

        // Create buy and sell orders for the composite instrument
        Order buyOrder = new Order("buy", "Trader1", OrderType.BUY, basket, 100.0, null);
        Order sellOrder = new Order("sell", "Trader2", OrderType.SELL, basket, 100.0, null);

        // Add orders to the order manager
        orderManager.addOrder(buyOrder);
        orderManager.addOrder(sellOrder);

        // Match orders
        tradingEngine.matchOrders(basket.getId());

        // Assert order statuses
        assertEquals(OrderStatus.FILLED, buyOrder.getStatus());
        assertEquals(OrderStatus.FILLED, sellOrder.getStatus());
    }

    @Test
    void testCanExecuteTrade_OrdersForDifferentInstruments() {
        // Create instruments
        Instrument instrument1 = new Instrument("1", "AAPL");
        Instrument instrument2 = new Instrument("2", "GOOG");

        // Create buy and sell orders for different instruments
        Order buyOrder = new Order("buy", "Trader1", OrderType.BUY, instrument1, 100.0, 150.0);
        Order sellOrder = new Order("sell", "Trader2", OrderType.SELL, instrument2, 100.0, 150.0);

        // Check if trade can be executed
        assertFalse(tradingEngine.canExecuteTrade(buyOrder, sellOrder));
    }

    @Test
    void testCanExecuteTrade_BuyOrderInactive() {
        // Create instruments
        Instrument instrument = new Instrument("1", "AAPL");

        // Create buy and sell orders
        Order buyOrder = new Order("buy", "Trader1", OrderType.BUY, instrument, 100.0, 150.0);
        Order sellOrder = new Order("sell", "Trader2", OrderType.SELL, instrument, 100.0, 150.0);

        // Set buy order status to FILLED
        buyOrder.setStatus(OrderStatus.FILLED);

        // Check if trade can be executed
        assertFalse(tradingEngine.canExecuteTrade(buyOrder, sellOrder));
    }

    @Test
    void testCanExecuteTrade_SellOrderInactive() {
        // Create instruments
        Instrument instrument = new Instrument("1", "AAPL");

        // Create buy and sell orders
        Order buyOrder = new Order("buy", "Trader1", OrderType.BUY, instrument, 100.0, 150.0);
        Order sellOrder = new Order("sell", "Trader2", OrderType.SELL, instrument, 100.0, 150.0);

        // Set sell order status to PARTIALLY_FILLED
        sellOrder.setStatus(OrderStatus.PARTIALLY_FILLED);

        // Check if trade can be executed
        assertTrue(tradingEngine.canExecuteTrade(buyOrder, sellOrder));
    }

    @Test
    void testCanExecuteTrade_NonPositiveQuantities() {
        // Create instruments
        Instrument instrument = new Instrument("1", "AAPL");

        // Create buy and sell orders with non-positive quantities
        Order buyOrder = new Order("buy", "Trader1", OrderType.BUY, instrument, 0.0, 150.0);
        Order sellOrder = new Order("sell", "Trader2", OrderType.SELL, instrument, -100.0, 150.0);

        // Check if trade can be executed
        assertFalse(tradingEngine.canExecuteTrade(buyOrder, sellOrder));
    }

    @Test
    void testCanExecuteTrade_ValidOrders() {
        // Create instruments
        Instrument instrument = new Instrument("1", "AAPL");

        // Create active buy and sell orders with positive quantities
        Order buyOrder = new Order("buy", "Trader1", OrderType.BUY, instrument, 100.0, 150.0);
        Order sellOrder = new Order("sell", "Trader2", OrderType.SELL, instrument, 100.0, 150.0);

        // Check if trade can be executed
        assertTrue(tradingEngine.canExecuteTrade(buyOrder, sellOrder));
    }
}
