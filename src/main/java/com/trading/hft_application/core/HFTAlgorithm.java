package com.trading.hft_application.core;


import com.trading.hft_application.core.execution.OrderManager;
import com.trading.hft_application.core.risk.RiskManager;
import com.trading.hft_application.core.signal.SignalGenerator;
import com.trading.hft_application.model.*;

import java.util.Map;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Core High Frequency Trading Algorithm
 */
public class HFTAlgorithm {

    // Configuration parameters
    private final double MAX_POSITION_SIZE;
    private final double MAX_ORDER_SIZE;
    private final double MAX_DAILY_LOSS;
    private final int LOOKBACK_PERIOD;
    private final double SIGNAL_THRESHOLD;

    // Market state
    private final Map<String, OrderBook> orderBooks = new ConcurrentHashMap<>();
    private final Map<String, Queue<MarketTick>> marketHistory = new ConcurrentHashMap<>();

    // Trading state
    private final Map<String, Position> positions = new ConcurrentHashMap<>();
    private final Map<Long, Order> activeOrders = new ConcurrentHashMap<>();

    // Performance tracking
    private final AtomicLong latencySum = new AtomicLong(0);
    private final AtomicLong messageCount = new AtomicLong(0);
    private final AtomicLong orderCount = new AtomicLong(0);

    // System state
    private final AtomicBoolean isRunning = new AtomicBoolean(false);
    private final ExecutorService executorService;

    // Components
    private final SignalGenerator signalGenerator;
    private final RiskManager riskManager;
    private final OrderManager orderManager;

    /**
     * Constructor with default parameters
     */
    public HFTAlgorithm() {
        this(1000000.0, 100000.0, 50000.0, 100, 0.0002);
    }

    /**
     * Constructor with custom parameters
     */
    public HFTAlgorithm(double maxPositionSize, double maxOrderSize, double maxDailyLoss,
                        int lookbackPeriod, double signalThreshold) {
        this.MAX_POSITION_SIZE = maxPositionSize;
        this.MAX_ORDER_SIZE = maxOrderSize;
        this.MAX_DAILY_LOSS = maxDailyLoss;
        this.LOOKBACK_PERIOD = lookbackPeriod;
        this.SIGNAL_THRESHOLD = signalThreshold;

        // Initialize components
        this.signalGenerator = new SignalGenerator(SIGNAL_THRESHOLD);
        this.riskManager = new RiskManager(MAX_POSITION_SIZE, MAX_ORDER_SIZE, MAX_DAILY_LOSS);
        this.orderManager = new OrderManager();

        // Create thread pool
        this.executorService = Executors.newFixedThreadPool(4);
    }

    /**
     * Initializes and starts the HFT algorithm
     */
    public void start() {
        if (isRunning.compareAndSet(false, true)) {
            System.out.println("Starting HFT Algorithm...");

            executorService.submit(this::marketDataProcessor);
            executorService.submit(this::signalGenerator);
            executorService.submit(this::orderManager);
            executorService.submit(this::riskManager);

            System.out.println("HFT Algorithm started successfully.");
        } else {
            System.out.println("HFT Algorithm is already running.");
        }
    }

    /**
     * Stops the HFT algorithm
     */
    public void stop() {
        if (isRunning.compareAndSet(true, false)) {
            System.out.println("Stopping HFT Algorithm...");

            // Cancel all active orders
            for (long orderId : activeOrders.keySet()) {
                orderManager.cancelOrder(orderId, activeOrders);
            }

            // Shutdown thread pool
            executorService.shutdown();

            System.out.println("HFT Algorithm stopped successfully.");
        } else {
            System.out.println("HFT Algorithm is not running.");
        }
    }

    /**
     * Emergency shutdown - cancels all orders and closes positions
     */
    public void emergencyShutdown() {
        System.out.println("EMERGENCY SHUTDOWN INITIATED");

        isRunning.set(false);

        // Cancel all active orders
        for (long orderId : activeOrders.keySet()) {
            orderManager.cancelOrder(orderId, activeOrders);
        }

        // Close all positions
        for (String symbol : positions.keySet()) {
            Position position = positions.get(symbol);
            OrderBook book = orderBooks.get(symbol);

            if (position != null && book != null && position.getSize() != 0) {
                Order order = riskManager.createClosePositionOrder(symbol, position.getSize(), book);
                orderManager.submitOrder(order, activeOrders);
            }
        }

        // Shutdown thread pool
        executorService.shutdown();

        System.out.println("Emergency shutdown completed.");
    }

    /**
     * Processes incoming market data
     */
    private void marketDataProcessor() {
        System.out.println("Market data processor started");

        while (isRunning.get()) {
            try {
                // In a real system, this would receive data from exchange
                MarketTick tick = receiveMarketData();
                if (tick != null) {
                    long startTime = System.nanoTime();

                    // Update order book
                    String symbol = tick.getSymbol();
                    OrderBook book = orderBooks.computeIfAbsent(symbol, s -> new OrderBook(s));
                    book.update(tick);

                    // Store historical data with fixed size
                    Queue<MarketTick> history = marketHistory.computeIfAbsent(
                            symbol, s -> new PriorityQueue<>((a, b) -> a.getTimestamp().compareTo(b.getTimestamp()))
                    );
                    history.add(tick);
                    while (history.size() > LOOKBACK_PERIOD) {
                        history.poll();
                    }

                    // Calculate latency for performance monitoring
                    long latency = System.nanoTime() - startTime;
                    latencySum.addAndGet(latency);
                    messageCount.incrementAndGet();
                }

                // Prevent CPU spinning
                Thread.sleep(1);
            } catch (Exception e) {
                System.err.println("Error in market data processor: " + e.getMessage());
            }
        }

        System.out.println("Market data processor stopped");
    }

    /**
     * Generates trading signals and places orders
     */
    private void signalGenerator() {
        System.out.println("Signal generator started");

        while (isRunning.get()) {
            try {
                for (String symbol : orderBooks.keySet()) {
                    OrderBook book = orderBooks.get(symbol);
                    Queue<MarketTick> history = marketHistory.get(symbol);

                    if (book != null && history != null && history.size() >= LOOKBACK_PERIOD) {
                        // Calculate market metrics
                        double midPrice = (book.getAsk() + book.getBid()) / 2.0;
                        double vol = signalGenerator.calculateVolatility(history);

                        // Update volatility tracking
                        riskManager.updateVolatility(symbol, vol);

                        // Calculate combined signal
                        double combinedSignal = signalGenerator.calculateCombinedSignal(history, book);

                        // Execute trades if signal exceeds threshold
                        if (signalGenerator.isSignalActionable(combinedSignal)) {
                            // Determine trade direction and size
                            boolean isBuy = combinedSignal > 0;

                            Position position = positions.get(symbol);
                            double orderSize = riskManager.calculateOrderSize(
                                    symbol, combinedSignal, vol, midPrice, position
                            );

                            // Create and submit order
                            if (orderSize > 0) {
                                Order order = new Order(
                                        System.nanoTime(),
                                        symbol,
                                        isBuy ? OrderType.BUY : OrderType.SELL,
                                        isBuy ? book.getAsk() : book.getBid(),
                                        orderSize
                                );
                                orderManager.submitOrder(order, activeOrders);
                                orderCount.incrementAndGet();

                                // Update position (in real system would happen after execution)
                                double profit = orderManager.updatePosition(order, positions);

                                // Update P&L tracking
                                riskManager.updatePnL(profit);
                            }
                        }
                    }
                }

                // Run signal generation at appropriate frequency
                Thread.sleep(5);
            } catch (Exception e) {
                System.err.println("Error in signal generator: " + e.getMessage());
            }
        }

        System.out.println("Signal generator stopped");
    }

    /**
     * Manages existing orders (cancellations, modifications)
     */
    private void orderManager() {
        System.out.println("Order manager started");

        while (isRunning.get()) {
            try {
                // Process order updates
                orderManager.manageActiveOrders(activeOrders, orderBooks);

                // Run at appropriate frequency
                Thread.sleep(10);
            } catch (Exception e) {
                System.err.println("Error in order manager: " + e.getMessage());
            }
        }

        System.out.println("Order manager stopped");
    }

    /**
     * Monitors and enforces risk limits
     */
    private void riskManager() {
        System.out.println("Risk manager started");

        while (isRunning.get()) {
            try {
                // Check risk limits
                if (riskManager.areLimitsExceeded(positions, orderBooks)) {
                    System.out.println("Risk limits exceeded. Initiating emergency shutdown.");
                    emergencyShutdown();
                    break;
                }

                // Check individual position limits
                for (Position position : positions.values()) {
                    String symbol = position.getSymbol();
                    OrderBook book = orderBooks.get(symbol);

                    if (book != null) {
                        double midPrice = (book.getAsk() + book.getBid()) / 2.0;
                        double positionValue = Math.abs(position.getSize() * midPrice);

                        // If position too large, reduce it
                        if (positionValue > MAX_POSITION_SIZE) {
                            Order reduceOrder = riskManager.createReducePositionOrder(
                                    symbol, position.getSize(), midPrice
                            );
                            orderManager.submitOrder(reduceOrder, activeOrders);
                        }
                    }
                }

                // Run at appropriate frequency
                Thread.sleep(100);
            } catch (Exception e) {
                System.err.println("Error in risk manager: " + e.getMessage());
            }
        }

        System.out.println("Risk manager stopped");
    }

    /**
     * Simulated market data receiver - in real system would connect to exchange
     */
    private MarketTick receiveMarketData() {
        // This is a placeholder. In a real system, this would receive data from an exchange
        // For testing, we'll generate random market data if there are no market data feeds

        // Only generate test data if we have no symbols being tracked or we're explicitly in test mode
        if (!orderBooks.isEmpty()) {
            return null;
        }

        // Create a dummy market tick for "BTC-USD"
        double basePrice = 50000.0;
        double randomOffset = (Math.random() - 0.5) * 100;
        double bid = basePrice + randomOffset;
        double ask = bid + 1.0 + (Math.random() * 2.0);

        return new MarketTick("BTC-USD", bid, ask, 1.0, 1.0);
    }

    /**
     * Adds a market data feed for a symbol
     */
    public void addMarketDataFeed(String symbol, double initialBid, double initialAsk) {
        // Create initial market tick and order book
        MarketTick tick = new MarketTick(symbol, initialBid, initialAsk, 1.0, 1.0);
        OrderBook book = new OrderBook(symbol);
        book.update(tick);

        // Store in our maps
        orderBooks.put(symbol, book);

        // Initialize market history
        Queue<MarketTick> history = marketHistory.computeIfAbsent(
                symbol, s -> new PriorityQueue<>((a, b) -> a.getTimestamp().compareTo(b.getTimestamp()))
        );
        history.add(tick);
    }

    /**
     * Updates market data for a symbol (for testing and simulation)
     */
    public void updateMarketData(String symbol, double bid, double ask, double bidSize, double askSize) {
        MarketTick tick = new MarketTick(symbol, bid, ask, bidSize, askSize);

        // Update order book
        OrderBook book = orderBooks.computeIfAbsent(symbol, s -> new OrderBook(s));
        book.update(tick);

        // Store historical data with fixed size
        Queue<MarketTick> history = marketHistory.computeIfAbsent(
                symbol, s -> new PriorityQueue<>((a, b) -> a.getTimestamp().compareTo(b.getTimestamp()))
        );
        history.add(tick);
        while (history.size() > LOOKBACK_PERIOD) {
            history.poll();
        }
    }

    /**
     * Gets the current positions
     */
    public Map<String, Position> getPositions() {
        return new ConcurrentHashMap<>(positions);
    }

    /**
     * Gets the current order books
     */
    public Map<String, OrderBook> getOrderBooks() {
        return new ConcurrentHashMap<>(orderBooks);
    }

    /**
     * Gets the active orders
     */
    public Map<Long, Order> getActiveOrders() {
        return new ConcurrentHashMap<>(activeOrders);
    }

    /**
     * Gets algorithm status
     */
    public boolean isRunning() {
        return isRunning.get();
    }

    /**
     * Gets performance metrics
     */
    public Map<String, Object> getPerformanceMetrics() {
        Map<String, Object> metrics = new ConcurrentHashMap<>();

        // Calculate average latency
        long count = messageCount.get();
        double avgLatencyNs = count > 0 ? (double) latencySum.get() / count : 0;
        double avgLatencyMs = avgLatencyNs / 1_000_000.0;

        metrics.put("messageCount", count);
        metrics.put("orderCount", orderCount.get());
        metrics.put("avgLatencyMs", avgLatencyMs);
        metrics.put("pnlToday", riskManager.getPnlToday());

        // Calculate total position value
        double totalPositionValue = 0.0;
        for (Position position : positions.values()) {
            String symbol = position.getSymbol();
            OrderBook book = orderBooks.get(symbol);

            if (book != null) {
                double midPrice = (book.getAsk() + book.getBid()) / 2.0;
                totalPositionValue += Math.abs(position.getSize() * midPrice);
            }
        }
        metrics.put("totalPositionValue", totalPositionValue);

        return metrics;
    }
}
