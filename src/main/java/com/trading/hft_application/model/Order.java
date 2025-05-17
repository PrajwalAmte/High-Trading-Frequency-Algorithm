package com.trading.hft_application.model;

/**
 * Represents an order in the trading system
 */
public class Order {
    private final long orderId;
    private final String symbol;
    private final OrderType type;
    private final double price;
    private final double size;
    private final long timestamp;

    public Order(long orderId, String symbol, OrderType type, double price, double size) {
        this.orderId = orderId;
        this.symbol = symbol;
        this.type = type;
        this.price = price;
        this.size = size;
        this.timestamp = System.currentTimeMillis();
    }

    public long getOrderId() {
        return orderId;
    }

    public String getSymbol() {
        return symbol;
    }

    public OrderType getType() {
        return type;
    }

    public double getPrice() {
        return price;
    }

    public double getSize() {
        return size;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public boolean isStale() {
        // Order is stale if older than 100ms
        return System.currentTimeMillis() - timestamp > 100;
    }

    @Override
    public String toString() {
        return "Order{" +
                "orderId=" + orderId +
                ", symbol='" + symbol + '\'' +
                ", type=" + type +
                ", price=" + price +
                ", size=" + size +
                '}';
    }
}