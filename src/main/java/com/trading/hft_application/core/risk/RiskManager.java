package com.trading.hft_application.core.risk;

import com.trading.hft_application.model.OrderBook;
import com.trading.hft_application.model.Position;
import com.trading.hft_application.model.Order;
import com.trading.hft_application.model.OrderType;
import lombok.Getter;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Responsible for monitoring and enforcing risk limits
 */
public class RiskManager {
    private final double MAX_POSITION_SIZE;
    private final double MAX_ORDER_SIZE;
    private final double MAX_DAILY_LOSS;

    @Getter
    private double pnlToday = 0.0;
    private final Map<String, Double> symbolVolatility = new ConcurrentHashMap<>();

    public RiskManager(double maxPositionSize, double maxOrderSize, double maxDailyLoss) {
        this.MAX_POSITION_SIZE = maxPositionSize;
        this.MAX_ORDER_SIZE = maxOrderSize;
        this.MAX_DAILY_LOSS = maxDailyLoss;
    }

    /**
     * Checks if risk limits are exceeded
     */
    public boolean areLimitsExceeded(Map<String, Position> positions, Map<String, OrderBook> orderBooks) {
        double totalExposure = 0.0;

        // Calculate total exposure across all positions
        for (Position position : positions.values()) {
            String symbol = position.getSymbol();
            OrderBook book = orderBooks.get(symbol);

            if (book != null) {
                double midPrice = (book.getAsk() + book.getBid()) / 2.0;
                double positionValue = Math.abs(position.getSize() * midPrice);
                totalExposure += positionValue;
            }
        }

        // Check daily loss limit
        return pnlToday < -MAX_DAILY_LOSS;
    }

    /**
     * Updates PnL tracking
     */
    public void updatePnL(double profit) {
        pnlToday += profit;
    }

    /**
     * Updates volatility for a symbol
     */
    public void updateVolatility(String symbol, double volatility) {
        symbolVolatility.put(symbol, volatility);
    }

    /**
     * Calculates appropriate order size based on signal and risk parameters
     */
    public double calculateOrderSize(String symbol, double signal, double volatility, double price, Position position) {
        // Base size on signal strength
        double signalStrength = Math.min(1.0, Math.abs(signal) / 0.001);
        double baseSize = MAX_ORDER_SIZE * signalStrength;

        // Adjust for volatility - reduce size in volatile markets
        double volAdjustment = Math.max(0.1, 1.0 - (volatility * 10));

        // Adjust for existing position - reduce size if adding to position
        double positionAdjustment = 1.0;

        if (position != null) {
            double positionSize = position.getSize();
            boolean sameDirection = (signal > 0 && positionSize > 0) || (signal < 0 && positionSize < 0);

            if (sameDirection) {
                double currentExposure = Math.abs(positionSize * price);
                positionAdjustment = Math.max(0.0, 1.0 - (currentExposure / MAX_POSITION_SIZE));
            }
        }

        // Calculate final size
        double finalSize = baseSize * volAdjustment * positionAdjustment;

        // Convert to quantity and ensure we don't exceed MAX_ORDER_SIZE
        return Math.min(MAX_ORDER_SIZE / price, finalSize / price);
    }

    /**
     * Creates an order to reduce position size
     */
    public Order createReducePositionOrder(String symbol, double currentSize, double currentPrice) {
        double targetSize = currentSize * 0.8; // Reduce by 20%
        double reduceAmount = currentSize - targetSize;

        return new Order(
                System.nanoTime(),
                symbol,
                currentSize > 0 ? OrderType.SELL : OrderType.BUY,
                currentSize > 0 ? currentPrice * 0.999 : currentPrice * 1.001, // Aggressive pricing
                Math.abs(reduceAmount)
        );
    }

    /**
     * Creates an order to close a position
     */
    public Order createClosePositionOrder(String symbol, double size, OrderBook book) {
        return new Order(
                System.nanoTime(),
                symbol,
                size > 0 ? OrderType.SELL : OrderType.BUY,
                size > 0 ? book.getBid() * 0.99 : book.getAsk() * 1.01, // Aggressive pricing
                Math.abs(size)
        );
    }

    public double getMaxPositionSize() {
        return MAX_POSITION_SIZE;
    }

    public double getMaxOrderSize() {
        return MAX_ORDER_SIZE;
    }

    public double getMaxDailyLoss() {
        return MAX_DAILY_LOSS;
    }
}