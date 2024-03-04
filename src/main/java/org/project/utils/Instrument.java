package org.project.utils;


// Instrument class
// Instrument (base class)
public class Instrument {

    private final String id;
    private final String symbol;

    public Instrument(String id, String symbol) {
        this.id = id;
        this.symbol = symbol;
    }

    public String getId() {
        return id;
    }

    public String getSymbol() {
        return symbol;
    }
}
