package com.trading.hft_application.core.signal;



import com.trading.hft_application.model.MarketTick;
import com.trading.hft_application.model.OrderBook;

import java.util.Queue;

/**
 * Responsible for generating trading signals based on market data
 */
public class SignalGenerator {
    private final double SIGNAL_THRESHOLD;

    public SignalGenerator(double signalThreshold) {
        this.SIGNAL_THRESHOLD = signalThreshold;
    }

    /**
     * Calculates combined trading signal from multiple strategies
     */
    public double calculateCombinedSignal(Queue<MarketTick> history, OrderBook book) {
        if (history == null || history.size() < 2 || book == null) {
            return 0.0;
        }

        double midPrice = (book.getAsk() + book.getBid()) / 2.0;

        // Statistical arbitrage signal
        double statArbSignal = calculateStatArbSignal(history, midPrice);

        // Mean reversion signal
        double meanReversionSignal = calculateMeanReversionSignal(history, midPrice);

        // Momentum signal
        double momentumSignal = calculateMomentumSignal(history);

        // Combine signals with weights
        return 0.4 * statArbSignal + 0.4 * meanReversionSignal + 0.2 * momentumSignal;
    }

    /**
     * Determines if signal is strong enough to trade
     */
    public boolean isSignalActionable(double signal) {
        return Math.abs(signal) > SIGNAL_THRESHOLD;
    }

    /**
     * Calculates statistical arbitrage signal
     */
    public double calculateStatArbSignal(Queue<MarketTick> history, double currentPrice) {
        // Simple moving average calculation
        double sum = history.stream()
                .mapToDouble(tick -> (tick.getBid() + tick.getAsk()) / 2.0)
                .sum();

        double movingAverage = sum / history.size();

        // Deviation from moving average as signal
        return (movingAverage - currentPrice) / currentPrice;
    }

    /**
     * Calculates mean reversion signal
     */
    public double calculateMeanReversionSignal(Queue<MarketTick> history, double currentPrice) {
        // Z-score based mean reversion
        double[] prices = history.stream()
                .mapToDouble(tick -> (tick.getBid() + tick.getAsk()) / 2.0)
                .toArray();

        double mean = 0.0;
        for (double price : prices) {
            mean += price;
        }
        mean /= prices.length;

        double stdDev = 0.0;
        for (double price : prices) {
            stdDev += Math.pow(price - mean, 2);
        }
        stdDev = Math.sqrt(stdDev / (prices.length - 1));

        if (stdDev == 0) {
            return 0;
        }

        // Z-score
        return (mean - currentPrice) / stdDev;
    }

    /**
     * Calculates momentum signal
     */
    public double calculateMomentumSignal(Queue<MarketTick> history) {
        // Momentum based on recent price changes
        MarketTick[] ticks = history.toArray(new MarketTick[0]);
        if (ticks.length < 10) {
            return 0;
        }

        double recentAvg = 0;
        double olderAvg = 0;

        // Recent average (last 10%)
        for (int i = ticks.length - 1; i >= ticks.length * 0.9; i--) {
            recentAvg += (ticks[i].getBid() + ticks[i].getAsk()) / 2.0;
        }
        recentAvg /= (ticks.length * 0.1);

        // Older average (first 10%)
        for (int i = 0; i < ticks.length * 0.1; i++) {
            olderAvg += (ticks[i].getBid() + ticks[i].getAsk()) / 2.0;
        }
        olderAvg /= (ticks.length * 0.1);

        // Return normalized momentum
        return (recentAvg - olderAvg) / olderAvg;
    }

    /**
     * Calculates volatility for a specific symbol
     */
    public double calculateVolatility(Queue<MarketTick> history) {
        if (history == null || history.size() < 2) {
            return 0.001;
        }

        double[] prices = history.stream()
                .mapToDouble(tick -> (tick.getBid() + tick.getAsk()) / 2.0)
                .toArray();

        double sum = 0.0;
        double mean = 0.0;

        // Calculate mean
        for (double price : prices) {
            mean += price;
        }
        mean /= prices.length;

        // Calculate variance
        for (double price : prices) {
            sum += Math.pow(price - mean, 2);
        }

        double vol = Math.sqrt(sum / (prices.length - 1));

        // Return normalized volatility
        return vol / mean;
    }
}
