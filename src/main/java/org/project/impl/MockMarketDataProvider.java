package org.project.impl;

import org.project.interfaces.MarketDataProvider;

import java.util.HashMap;
import java.util.Map;

// Market Data Provider (mock implementation)
public class MockMarketDataProvider implements MarketDataProvider {

    private final Map<String, Double> marketPrices = new HashMap<>();

    public MockMarketDataProvider(Map<String, Double> marketPrices) {
        this.marketPrices.putAll(marketPrices);
    }

    @Override
    public double getMarketPrice(String instrumentId) {
        return marketPrices.getOrDefault(instrumentId, 10.0);
    }
}