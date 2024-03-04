package org.project.utils;

import org.project.enums.OrderType;
import org.project.utils.Instrument;

// Order (extended to handle composite instruments)
public class Order {

    private final String id;
    private final String traderId;
    private final OrderType type;
    private final Instrument instrument;
    private final int quantity;
    private final Double price;

    public Order(String id, String traderId, OrderType type, Instrument instrument, int quantity, Double price) {
        this.id = id;
        this.traderId = traderId;
        this.type = type;
        this.instrument = instrument;
        this.quantity = quantity;
        this.price = price;
    }

    public String getId() {
        return id;
    }

    public String getTraderId() {
        return traderId;
    }

    public OrderType getType() {
        return type;
    }

    public Instrument getInstrument() {
        return instrument;
    }

    public int getQuantity() {
        return quantity;
    }

    public Double getPrice() {
        return price;
    }

    public boolean isCompositeOrder() {
        return instrument instanceof CompositeInstrument;
    }
}
