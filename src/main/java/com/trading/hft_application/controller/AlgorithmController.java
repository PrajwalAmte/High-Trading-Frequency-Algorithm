package com.trading.hft_application.controller;

import com.trading.hft_application.service.AlgorithmService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * REST controller for the HFT Algorithm
 */
@RestController
@RequestMapping("/api/algorithm")
public class AlgorithmController {

    private final AlgorithmService algorithmService;

    @Autowired
    public AlgorithmController(AlgorithmService algorithmService) {
        this.algorithmService = algorithmService;
    }

    /**
     * Start the algorithm
     */
    @PostMapping("/start")
    public ResponseEntity<Map<String, Object>> startAlgorithm() {
        Map<String, Object> response = new HashMap<>();

        try {
            algorithmService.startAlgorithm();
            response.put("status", "success");
            response.put("message", "Algorithm started successfully");
        } catch (Exception e) {
            response.put("status", "error");
            response.put("message", "Failed to start algorithm: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }

        return ResponseEntity.ok(response);
    }

    /**
     * Stop the algorithm
     */
    @PostMapping("/stop")
    public ResponseEntity<Map<String, Object>> stopAlgorithm() {
        Map<String, Object> response = new HashMap<>();

        try {
            algorithmService.stopAlgorithm();
            response.put("status", "success");
            response.put("message", "Algorithm stopped successfully");
        } catch (Exception e) {
            response.put("status", "error");
            response.put("message", "Failed to stop algorithm: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }

        return ResponseEntity.ok(response);
    }

    /**
     * Get algorithm status
     */
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getStatus() {
        Map<String, Object> response = new HashMap<>();

        try {
            boolean isRunning = algorithmService.isAlgorithmRunning();
            response.put("status", "success");
            response.put("running", isRunning);
        } catch (Exception e) {
            response.put("status", "error");
            response.put("message", "Failed to get algorithm status: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }

        return ResponseEntity.ok(response);
    }

    /**
     * Get positions
     */
    @GetMapping("/positions")
    public ResponseEntity<Map<String, Object>> getPositions() {
        Map<String, Object> response = new HashMap<>();

        try {
            Map<String, Map<String, Object>> positions = algorithmService.getPositions();
            response.put("status", "success");
            response.put("positions", positions);
        } catch (Exception e) {
            response.put("status", "error");
            response.put("message", "Failed to get positions: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }

        return ResponseEntity.ok(response);
    }

    /**
     * Get order books
     */
    @GetMapping("/orderbooks")
    public ResponseEntity<Map<String, Object>> getOrderBooks() {
        Map<String, Object> response = new HashMap<>();

        try {
            Map<String, Map<String, Object>> orderBooks = algorithmService.getOrderBooks();
            response.put("status", "success");
            response.put("orderBooks", orderBooks);
        } catch (Exception e) {
            response.put("status", "error");
            response.put("message", "Failed to get order books: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }

        return ResponseEntity.ok(response);
    }

    /**
     * Get performance metrics
     */
    @GetMapping("/performance")
    public ResponseEntity<Map<String, Object>> getPerformance() {
        Map<String, Object> response = new HashMap<>();

        try {
            Map<String, Object> metrics = algorithmService.getPerformanceMetrics();
            response.put("status", "success");
            response.put("metrics", metrics);
        } catch (Exception e) {
            response.put("status", "error");
            response.put("message", "Failed to get performance metrics: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }

        return ResponseEntity.ok(response);
    }

    /**
     * Update market data (for testing)
     */
    @PostMapping("/updatemarket")
    public ResponseEntity<Map<String, Object>> updateMarketData(
            @RequestParam String symbol,
            @RequestParam double bid,
            @RequestParam double ask) {

        Map<String, Object> response = new HashMap<>();

        try {
            algorithmService.updateMarketData(symbol, bid, ask);
            response.put("status", "success");
            response.put("message", "Market data updated successfully");
        } catch (Exception e) {
            response.put("status", "error");
            response.put("message", "Failed to update market data: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }

        return ResponseEntity.ok(response);
    }

    /**
     * Add a new symbol
     */
    @PostMapping("/addsymbol")
    public ResponseEntity<Map<String, Object>> addSymbol(
            @RequestParam String symbol,
            @RequestParam double bid,
            @RequestParam double ask) {

        Map<String, Object> response = new HashMap<>();

        try {
            algorithmService.addSymbol(symbol, bid, ask);
            response.put("status", "success");
            response.put("message", "Symbol added successfully");
        } catch (Exception e) {
            response.put("status", "error");
            response.put("message", "Failed to add symbol: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }

        return ResponseEntity.ok(response);
    }
}
