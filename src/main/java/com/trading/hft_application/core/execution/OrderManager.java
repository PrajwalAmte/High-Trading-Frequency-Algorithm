package com.trading.hft_application.core.execution;

import com.trading.hft_application.model.Order;
import com.trading.hft_application.model.OrderBook;
import com.trading.hft_application.model.Position;

import java.util.Map;

/**
 * Responsible for managing orders (submission, cancellation, modification)
 */
public class OrderManager {

    /**
     * Submits a new order to the market
     */
    public void submitOrder(Order order, Map<Long, Order> activeOrders) {
        try {
            // In a real system, this would connect to exchange API
            System.out.println("Submitting order: " + order);

            // Store order in active orders map
            activeOrders.put(order.getOrderId(), order);
        } catch (Exception e) {
            System.err.println("Error submitting order: " + e.getMessage());
        }
    }

    /**
     * Cancels an existing order
     */
    public void cancelOrder(long orderId, Map<Long, Order> activeOrders) {
        try {
            // In a real system, this would connect to exchange API
            System.out.println("Cancelling order: " + orderId);

            // Remove from active orders
            activeOrders.remove(orderId);
        } catch (Exception e) {
            System.err.println("Error cancelling order: " + e.getMessage());
        }
    }

    /**
     * Updates an existing order
     */
    public void updateOrder(long orderId, double newPrice, double newSize, Map<Long, Order> activeOrders) {
        try {
            // In a real system, this would connect to exchange API
            System.out.println("Updating order: " + orderId + " to price: " + newPrice + " size: " + newSize);

            // Update order in active orders map
            Order existing = activeOrders.get(orderId);
            if (existing != null) {
                Order updated = new Order(
                        existing.getOrderId(),
                        existing.getSymbol(),
                        existing.getType(),
                        newPrice,
                        newSize
                );
                activeOrders.put(orderId, updated);
            }
        } catch (Exception e) {
            System.err.println("Error updating order: " + e.getMessage());
        }
    }

    /**
     * Updates position after order execution
     */
    public double updatePosition(Order order, Map<String, Position> positions) {
        String symbol = order.getSymbol();
        double size = order.getSize();
        double profit = 0.0;

        if (order.getType() == com.trading.hft_application.model.OrderType.SELL) {
            size = -size;
        }

        // Update position
        Position position = positions.computeIfAbsent(symbol, s -> new Position(s, 0.0, 0.0));
        position.updatePosition(size, order.getPrice());

        // Return profit from this trade
        return position.getLastTradeProfit();
    }

    /**
     * Checks if orders need adjustment based on market conditions
     */
    public void manageActiveOrders(Map<Long, Order> activeOrders, Map<String, OrderBook> orderBooks) {
        // Process order updates
        for (Order order : activeOrders.values()) {
            // Check if order is stale (latency-sensitive)
            if (order.isStale()) {
                cancelOrder(order.getOrderId(), activeOrders);
                continue;
            }

            // Check if market moved away from order
            String symbol = order.getSymbol();
            OrderBook book = orderBooks.get(symbol);

            if (book != null) {
                boolean needsUpdate = false;
                double newPrice = order.getPrice();

                // Adjust limit orders based on new market data
                if (order.getType() == com.trading.hft_application.model.OrderType.BUY && order.getPrice() < book.getBid()) {
                    newPrice = book.getBid();
                    needsUpdate = true;
                } else if (order.getType() == com.trading.hft_application.model.OrderType.SELL && order.getPrice() > book.getAsk()) {
                    newPrice = book.getAsk();
                    needsUpdate = true;
                }

                if (needsUpdate) {
                    updateOrder(order.getOrderId(), newPrice, order.getSize(), activeOrders);
                }
            }
        }
    }
}
