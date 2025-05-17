package com.trading.hft_application.model;

/**
 * Represents a trading position for a specific symbol
 */
public class Position {
    private final String symbol;
    private double size;
    private double avgPrice;
    private double lastTradeProfit;
    private double totalProfit;

    public Position(String symbol, double size, double avgPrice) {
        this.symbol = symbol;
        this.size = size;
        this.avgPrice = avgPrice;
        this.lastTradeProfit = 0.0;
        this.totalProfit = 0.0;
    }

    public void updatePosition(double newSize, double price) {
        double oldValue = size * avgPrice;

        if ((size > 0 && newSize < 0) || (size < 0 && newSize > 0)) {
            // Position flipping - calculate profit
            lastTradeProfit = size * (price - avgPrice);
            totalProfit += lastTradeProfit;

            // Reset position
            size = newSize;
            avgPrice = price;
        } else {
            // Adding to position - update average price
            double newValue = newSize * price;
            size += newSize;

            if (size != 0) {
                avgPrice = (oldValue + newValue) / size;
            }

            lastTradeProfit = 0;
        }
    }

    public String getSymbol() {
        return symbol;
    }

    public double getSize() {
        return size;
    }

    public double getAvgPrice() {
        return avgPrice;
    }

    public double getLastTradeProfit() {
        return lastTradeProfit;
    }

    public double getTotalProfit() {
        return totalProfit;
    }

    public double getCurrentValue(double currentPrice) {
        return size * currentPrice;
    }

    public double getUnrealizedPnL(double currentPrice) {
        return size * (currentPrice - avgPrice);
    }

    @Override
    public String toString() {
        return "Position{" +
                "symbol='" + symbol + '\'' +
                ", size=" + size +
                ", avgPrice=" + avgPrice +
                ", totalProfit=" + totalProfit +
                '}';
    }
}
