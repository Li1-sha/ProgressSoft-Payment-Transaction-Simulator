package com.progressoft.service; // or your preferred package

import com.progressoft.domain.Order;
import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class OrderFileImporter {

    public ImportResult importOrders(Path filePath) throws IOException {
        List<Order> orders = new ArrayList<>();
        List<SkippedLine> skipped = new ArrayList<>();

        try (BufferedReader reader = Files.newBufferedReader(filePath)) {
            String line;
            while ((line = reader.readLine()) != null) {
                try {
                    Order order = parseLine(line);
                    orders.add(order);
                } catch (Exception e) {
                    skipped.add(new SkippedLine(line, e.getMessage()));
                }
            }
        }
        return new ImportResult(orders, skipped);
    }

    private Order parseLine(String line) {
        String[] parts = line.split(",");
        if (parts.length < 4) {
            throw new IllegalArgumentException("Expected at least 4 fields: id, customerName, amount, currency");
        }

        String idStr = parts[0].trim();
        String customerName = parts[1].trim();
        String amountStr = parts[2].trim();
        String currency = parts[3].trim();

        if (customerName.isEmpty()) {
            throw new IllegalArgumentException("Customer name cannot be empty");
        }
        if (currency.isEmpty()) {
            throw new IllegalArgumentException("Currency cannot be empty");
        }

        double amount;
        try {
            amount = Double.parseDouble(amountStr);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid amount: " + amountStr, e);
        }

        Order order = new Order();
        if (!idStr.isEmpty()) {
            try {
                order.setId(Long.parseLong(idStr));
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Invalid id: " + idStr, e);
            }
        }
        order.setCustomerName(customerName);
        order.setAmount(amount);
        order.setCurrency(currency);
        return order;
    }

    // Result containers
    public static class ImportResult {
        private final List<Order> orders;
        private final List<SkippedLine> skipped;

        public ImportResult(List<Order> orders, List<SkippedLine> skipped) {
            this.orders = orders;
            this.skipped = skipped;
        }

        public List<Order> getOrders() { return orders; }
        public List<SkippedLine> getSkipped() { return skipped; }
        public int getValidCount() { return orders.size(); }
        public int getSkippedCount() { return skipped.size(); }
    }

    public static class SkippedLine {
        private final String line;
        private final String reason;

        public SkippedLine(String line, String reason) {
            this.line = line;
            this.reason = reason;
        }

        public String getLine() { return line; }
        public String getReason() { return reason; }
    }
}