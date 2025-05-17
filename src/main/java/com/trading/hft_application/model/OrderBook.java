package com.trading.hft_application.model;

import java.util.HashMap;
import java.util.Map;

/**
 * Represents an order book for a specific symbol
 */
public class OrderBook {
    private final String symbol;
    private double bid;
    private double ask;
    private double bidSize;
    private double askSize;
    private final Map<Double, Double> bids = new HashMap<>();
    private final Map<Double, Double> asks = new HashMap<>();

    public OrderBook(String symbol) {
        this.symbol = symbol;
    }

    public void update(MarketTick tick) {
        this.bid = tick.getBid();
        this.ask = tick.getAsk();
        this.bidSize = tick.getBidSize();
        this.askSize = tick.getAskSize();

        // In a real system, would update full order book
        bids.put(tick.getBid(), tick.getBidSize());
        asks.put(tick.getAsk(), tick.getAskSize());
    }

    public String getSymbol() {
        return symbol;
    }

    public double getBid() {
        return bid;
    }

    public double getAsk() {
        return ask;
    }

    public double getBidSize() {
        return bidSize;
    }

    public double getAskSize() {
        return askSize;
    }

    public Map<Double, Double> getBids() {
        return new HashMap<>(bids);
    }

    public Map<Double, Double> getAsks() {
        return new HashMap<>(asks);
    }

    @Override
    public String toString() {
        return "OrderBook{" +
                "symbol='" + symbol + '\'' +
                ", bid=" + bid +
                ", ask=" + ask +
                ", spread=" + (ask - bid) +
                '}';
    }
}
