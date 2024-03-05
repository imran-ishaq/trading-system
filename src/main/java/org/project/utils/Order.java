package org.project.utils;

import org.project.enums.OrderStatus;
import org.project.enums.OrderType;

// Order (extended to handle composite instruments)
public class Order {

    private final String id;
    private final String traderId;
    private final OrderType type;
    private Instrument instrument;
    private Double quantity;
    private final Double price;

    private OrderStatus status;

    public OrderStatus getStatus() {
        return status;
    }

    public void setStatus(OrderStatus status) {
        this.status = status;
    }

    public Order(String id, String traderId, OrderType type, Instrument instrument, Double quantity, Double price) {
        this.id = id;
        this.traderId = traderId;
        this.type = type;
        this.instrument = instrument;
        this.quantity = quantity;
        this.price = price;
        this.status = OrderStatus.PENDING;
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

    public Double getQuantity() {
        return quantity;
    }

    public void setQuantity(Double quantity) {
        this.quantity = quantity;
    }

    public Double getPrice() {
        return price;
    }

    public boolean isCompositeOrder() {
        return instrument instanceof CompositeInstrument;
    }

    public boolean isFilled(){
        return this.status == OrderStatus.FILLED;
    }

    public void setInstrument(Instrument instrument) {
        this.instrument = instrument;
    }
}
