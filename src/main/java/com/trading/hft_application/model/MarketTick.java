package com.trading.hft_application.model;

import java.time.Instant;

/**
 * Represents a market data tick with bid/ask information
 */
public class MarketTick {
    private final String symbol;
    private final double bid;
    private final double ask;
    private final double bidSize;
    private final double askSize;
    private final Instant timestamp;

    public MarketTick(String symbol, double bid, double ask, double bidSize, double askSize) {
        this.symbol = symbol;
        this.bid = bid;
        this.ask = ask;
        this.bidSize = bidSize;
        this.askSize = askSize;
        this.timestamp = Instant.now();
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

    public Instant getTimestamp() {
        return timestamp;
    }

    @Override
    public String toString() {
        return "MarketTick{" +
                "symbol='" + symbol + '\'' +
                ", bid=" + bid +
                ", ask=" + ask +
                ", bidSize=" + bidSize +
                ", askSize=" + askSize +
                ", timestamp=" + timestamp +
                '}';
    }
}
