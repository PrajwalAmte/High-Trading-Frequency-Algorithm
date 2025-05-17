package com.trading.hft_application.service;

import com.trading.hft_application.core.HFTAlgorithm;
import com.trading.hft_application.model.OrderBook;
import com.trading.hft_application.model.Position;
import jakarta.annotation.PreDestroy;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * Service layer for interacting with the HFT Algorithm
 */
@Service
public class AlgorithmService {

    private final HFTAlgorithm algorithm;

    public AlgorithmService() {
        // Initialize with default parameters
        this.algorithm = new HFTAlgorithm();

        // Add some test symbols for demonstration
        addTestSymbols();
    }

    /**
     * Start the algorithm
     */
    public void startAlgorithm() {
        algorithm.start();
    }

    /**
     * Stop the algorithm
     */
    public void stopAlgorithm() {
        algorithm.stop();
    }

    /**
     * Get algorithm status
     */
    public boolean isAlgorithmRunning() {
        return algorithm.isRunning();
    }

    /**
     * Get current positions
     */
    public Map<String, Map<String, Object>> getPositions() {
        Map<String, Map<String, Object>> result = new HashMap<>();
        Map<String, Position> positions = algorithm.getPositions();
        Map<String, OrderBook> orderBooks = algorithm.getOrderBooks();

        for (Map.Entry<String, Position> entry : positions.entrySet()) {
            String symbol = entry.getKey();
            Position position = entry.getValue();
            Map<String, Object> positionData = new HashMap<>();

            positionData.put("symbol", symbol);
            positionData.put("size", position.getSize());
            positionData.put("avgPrice", position.getAvgPrice());
            positionData.put("totalProfit", position.getTotalProfit());

            // Calculate unrealized P&L if we have market data
            OrderBook book = orderBooks.get(symbol);
            if (book != null) {
                double midPrice = (book.getAsk() + book.getBid()) / 2.0;
                positionData.put("currentPrice", midPrice);
                positionData.put("currentValue", position.getCurrentValue(midPrice));
                positionData.put("unrealizedPnL", position.getUnrealizedPnL(midPrice));
            }

            result.put(symbol, positionData);
        }

        return result;
    }

    /**
     * Get order books
     */
    public Map<String, Map<String, Object>> getOrderBooks() {
        Map<String, Map<String, Object>> result = new HashMap<>();
        Map<String, OrderBook> orderBooks = algorithm.getOrderBooks();

        for (Map.Entry<String, OrderBook> entry : orderBooks.entrySet()) {
            String symbol = entry.getKey();
            OrderBook book = entry.getValue();
            Map<String, Object> bookData = new HashMap<>();

            bookData.put("symbol", symbol);
            bookData.put("bid", book.getBid());
            bookData.put("ask", book.getAsk());
            bookData.put("bidSize", book.getBidSize());
            bookData.put("askSize", book.getAskSize());
            bookData.put("spread", book.getAsk() - book.getBid());

            result.put(symbol, bookData);
        }

        return result;
    }

    /**
     * Get performance metrics
     */
    public Map<String, Object> getPerformanceMetrics() {
        return algorithm.getPerformanceMetrics();
    }

    /**
     * Update market data for testing
     */
    public void updateMarketData(String symbol, double bid, double ask) {
        algorithm.updateMarketData(symbol, bid, ask, 1.0, 1.0);
    }

    /**
     * Add a new symbol
     */
    public void addSymbol(String symbol, double initialBid, double initialAsk) {
        algorithm.addMarketDataFeed(symbol, initialBid, initialAsk);
    }

    /**
     * Add test symbols for demonstration
     */
    private void addTestSymbols() {
        // Add some test symbols with initial prices
        algorithm.addMarketDataFeed("BTC-USD", 50000.0, 50001.0);
        algorithm.addMarketDataFeed("ETH-USD", 3000.0, 3001.0);
        algorithm.addMarketDataFeed("SOL-USD", 100.0, 100.1);
    }

    /**
     * Ensure algorithm is stopped when service is destroyed
     */
    @PreDestroy
    public void cleanup() {
        if (algorithm.isRunning()) {
            algorithm.stop();
        }
    }
}
