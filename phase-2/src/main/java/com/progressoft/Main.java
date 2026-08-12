package com.progressoft;

import com.progressoft.domain.Order;
import com.progressoft.exception.GatewayTimeoutException;
import com.progressoft.exception.InsufficientFundsException;
import com.progressoft.exception.ReconciliationRequiredException;
import com.progressoft.exception.ValidationFailedException;
import com.progressoft.payment.PaymentGateway;
import com.progressoft.repository.OrderRepository;
import com.progressoft.repository.Repository;
import com.progressoft.repository.inmemory.InMemoryOrderRepository;
import com.progressoft.repository.jdbc.JdbcOrderRepository;
import com.progressoft.service.OrderFileImporter;
import com.progressoft.service.OrderService;
import com.progressoft.validation.OrderEnricher;
import com.progressoft.validation.PaymentValidator;
import com.progressoft.validation.Validators;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import javax.sql.DataSource;
import java.io.IOException;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.Scanner;

public class Main {

    public static void main(String[] args) {
        // --- 1. Wire Dependencies ---
        DataSource dataSource = createHikariDataSource();
        // Ensure schema exists
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute("CREATE TABLE IF NOT EXISTS orders (\n" +
                         "    id BIGINT AUTO_INCREMENT PRIMARY KEY,\n" +
                         "    customer_name VARCHAR(255) NOT NULL,\n" +
                         "    amount DECIMAL(19,4) NOT NULL,\n" +
                         "    currency VARCHAR(10) NOT NULL\n" +
                         ")\n");
            System.out.println("    Database schema ready.");
        } catch (SQLException e) {
            System.err.println("    Failed to create schema: " + e.getMessage());
            // Fallback to in‑memory (but we'll just exit for clarity)
            System.exit(1);
        }

        // --- 2. Wire Dependencies ---
        Repository<Order, Long> repository = new JdbcOrderRepository(dataSource);

        PaymentGateway paymentGateway = order -> {
            if (order.getAmount() > 800) {
                throw new GatewayTimeoutException("api.payments.com", 3000L, "charge");
            }
            if (order.getAmount() > 500) {
                throw new InsufficientFundsException(order.getAmount(), order.getAmount() - 200);
            }
            System.out.println("    Charging " + order.getAmount() +
                    " in " + order.getCurrency() +
                    " for " + order.getCustomerName());
        };

        OrderEnricher enricher = Validators.defaultCurrency("OMR")
                .andThen(Validators.timestampEnricher());

        PaymentValidator composedValidator = Validators.positiveAmount()
                .and(Validators.maxLimit(10000.0))
                .and(Validators.currencyCheck("OMR", "EUR", "USD", "GBP"));

        OrderService service = new OrderService(
                repository,
                paymentGateway,
                composedValidator,
                enricher,
                dataSource
        );

        // --- 2. Interactive Menu ---
        Scanner scanner = new Scanner(System.in);
        boolean running = true;

        System.out.println("========================");
        System.out.println("    ORDER SERVICE ");
        System.out.println("========================");
        System.out.println("Validators: Positive Amount | Max 10000 | Currencies: OMR, EUR, USD, GBP");
        System.out.println("Payment Gateway: InsufficientFunds if amount > 500");
        System.out.println("                 GatewayTimeout if amount > 800");
        System.out.println();

        while (running) {
            printMenu();
            String choice = scanner.nextLine().trim();

            switch (choice) {
                case "1": placeNewOrder(scanner, service);
                    break;
                case "2": findOrderById(scanner, service);
                    break;
                case "3": showAllOrders(service);
                    break;
                case "4": running = false;
                    System.out.println("\n Exiting. Goodbye!");
                    break;
                case "5": importOrdersFromFile(scanner, service);
                    break;
                default: System.out.println("Invalid option. Please enter 1, 2, 3, 4, or 5.");
                    break;
            }
            System.out.println();
        }
        service.shutdownExecutor();
        scanner.close();
    }

    private static void printMenu() {
        System.out.println("--- Main Menu ---");
        System.out.println("1. Place a new order");
        System.out.println("2. Find order by ID");
        System.out.println("3. Show all orders");
        System.out.println("4. Exit");
        System.out.println("5. Import orders from CSV file");
        System.out.print("Choose an option: ");
    }

    private static void placeNewOrder(Scanner scanner, OrderService service) {
        System.out.println("\n--- Place New Order ---");

        System.out.print("Enter customer name: ");
        String name = scanner.nextLine().trim();
        if (name.isEmpty()) {
            System.out.println(" Customer name cannot be empty.");
            return;
        }

        double amount = 0;
        boolean validAmount = false;
        while (!validAmount) {
            System.out.print("Enter amount (e.g., 99.99): ");
            String input = scanner.nextLine().trim();
            try {
                amount = Double.parseDouble(input);
                validAmount = true;
            } catch (NumberFormatException e) {
                System.out.println(" Invalid number. Please enter a numeric value.");
            }
        }

        System.out.print("Enter currency (e.g., OMR, EUR, USD, GBP) [Press Enter for OMR]: ");
        String currency = scanner.nextLine().trim().toUpperCase();
        if (currency.isEmpty()) {
            currency = null;
        }

        Order order = new Order();
        order.setCustomerName(name);
        order.setAmount(amount);
        order.setCurrency(currency);

        try {
            Order placed = service.placeOrder(order);
            System.out.println("    Order placed successfully!");
            System.out.println("    Assigned ID: " + placed.getId());
            System.out.println("    Currency used: " + placed.getCurrency());
            System.out.println("    Amount: " + placed.getAmount());
        } catch (ValidationFailedException e) {
            System.out.println("    Validation failed: " + e.getMessage());
            System.out.println("      Field: " + e.getFieldName() + " | Rejected: " + e.getRejectedValue());
        } catch (InsufficientFundsException e) {
            System.out.println("    Insufficient funds!");
            System.out.println("      Required: " + e.getRequiredAmount());
            System.out.println("      Available: " + e.getAvailableAmount());
        } catch (GatewayTimeoutException e) {
            System.out.println("    Gateway timeout!");
            System.out.println("      Endpoint: " + e.getEndpoint());
            System.out.println("      Timeout: " + e.getTimeoutMillis() + "ms");
            System.out.println("      Operation: " + e.getOperation());
        } catch (ReconciliationRequiredException e) {
            // This means the order was charged but failed to persist – manual review needed.
            System.out.println("    CRITICAL");
            System.out.println("    Order was charged but could not be saved to the database.");
            System.out.println("    Please check the order and reconcile manually.");
            System.out.println("    Order details: " + e.getOrder().getCustomerName() +
                    ", amount " + e.getOrder().getAmount() +
                    " " + e.getOrder().getCurrency());
        }
    }

    private static void findOrderById(Scanner scanner, OrderService service) {
        System.out.println("\n--- Find Order by ID ---");
        System.out.print("Enter Order ID: ");
        String input = scanner.nextLine().trim();

        try {
            Long id = Long.parseLong(input);
            Order found = service.findOrder(id);
            System.out.println("      Order found:");
            System.out.println("      ID: " + found.getId());
            System.out.println("      Customer: " + found.getCustomerName());
            System.out.println("      Amount: " + found.getAmount());
            System.out.println("      Currency: " + found.getCurrency());
        } catch (NumberFormatException e) {
            System.out.println("    Invalid ID. Please enter a number.");
        } catch (RuntimeException e) {
            if (e.getMessage().contains("not found")) {
                System.out.println("    " + e.getMessage());
            } else {
                throw e;
            }
        }
    }

    private static void showAllOrders(OrderService service) {
        System.out.println("\n--- All Orders ---");
        List<Order> orders = service.getAllOrders();
        if (orders.isEmpty()) {
            System.out.println("    No orders placed yet.");
            return;
        }
        System.out.println("   Total orders: " + orders.size());
        for (Order o : orders) {
            System.out.println("      [ID: " + o.getId() + "] " +
                    o.getCustomerName() + " - " + o.getAmount() +
                    " (" + o.getCurrency() + ")");
        }
    }

    private static void importOrdersFromFile(Scanner scanner, OrderService service) {
        System.out.println("\n--- Import Orders from CSV ---");
        System.out.print("Enter the CSV file path: ");
        String filePath = scanner.nextLine().trim();

        if (filePath.isEmpty()) {
            System.out.println("File path cannot be empty.");
            return;
        }

        OrderFileImporter importer = new OrderFileImporter();
        try {
            OrderFileImporter.ImportResult result = importer.importOrders(Path.of(filePath));

            System.out.println("    Valid orders: " + result.getValidCount());
            System.out.println("    Skipped lines: " + result.getSkippedCount());

            if (result.getSkippedCount() > 0) {
                System.out.println("    --- Skipped lines ---");
                result.getSkipped().forEach(s ->
                        System.out.println("      Line: " + s.getLine() + " | Reason: " + s.getReason())
                );
            }

            if (result.getValidCount() > 0) {
                OrderService.ProcessingResult processingResult =
                        service.processBatchWithStreams(result.getOrders());

                System.out.println("    --- Pipeline results ---");
                System.out.println("      Approved: " + processingResult.getPartitioned().get(true).size());
                System.out.println("      Rejected: " + processingResult.getPartitioned().get(false).size());
                System.out.println("      Totals per currency:");
                processingResult.getTotalsByCurrency().forEach((currency, total) ->
                        System.out.println("        " + currency + ": " + total)
                );
            } else {
                System.out.println("    No valid orders to process.");
            }

        } catch (IOException e) {
            System.out.println("    Error reading file: " + e.getMessage());
        }
    }
    public static DataSource createHikariDataSource() {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:h2:~/orders;DB_CLOSE_DELAY=-1");
        config.setUsername("sa");
        config.setPassword("");
        config.setDriverClassName("org.h2.Driver");
        config.setMaximumPoolSize(10);
        return new HikariDataSource(config);
    }
}