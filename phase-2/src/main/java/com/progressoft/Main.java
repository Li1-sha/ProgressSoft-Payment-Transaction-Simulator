package com.progressoft;

import com.progressoft.domain.Order;
import com.progressoft.exception.GatewayTimeoutException;
import com.progressoft.exception.InsufficientFundsException;
import com.progressoft.exception.ValidationFailedException;
import com.progressoft.payment.PaymentGateway;
import com.progressoft.repository.OrderRepository;
import com.progressoft.repository.inmemory.InMemoryOrderRepository;
import com.progressoft.service.OrderService;
import com.progressoft.validation.OrderEnricher;
import com.progressoft.validation.PaymentValidator;
import com.progressoft.validation.Validators;

import java.util.List;
import java.util.Scanner;

public class Main {

    public static void main(String[] args) {
        // --- 1. Wire Dependencies ---
        OrderRepository repository = new InMemoryOrderRepository();

        // Dummy PaymentGateway with realistic failure scenarios
        PaymentGateway paymentGateway = order -> {
            // Simulate different failures based on amount
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
                enricher
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
                case "2" : findOrderById(scanner, service);
                case "3" : showAllOrders(service);
                case "4" : {
                    running = false;
                    System.out.println("\n Exiting. Goodbye!");
                }
                default : System.out.println("Invalid option. Please enter 1, 2, 3, or 4.");
            }
            System.out.println();
        }
        scanner.close();
    }

    private static void printMenu() {
        System.out.println("--- Main Menu ---");
        System.out.println("1. Place a new order");
        System.out.println("2. Find order by ID");
        System.out.println("3. Show all orders");
        System.out.println("4. Exit");
        System.out.print("Choose an option: ");
    }

    private static void placeNewOrder(Scanner scanner, OrderService service) {
        System.out.println("\n--- Place New Order ---");

        // 1. Customer Name
        System.out.print("Enter customer name: ");
        String name = scanner.nextLine().trim();
        if (name.isEmpty()) {
            System.out.println(" Customer name cannot be empty.");
            return;
        }

        // 2. Amount (with validation loop)
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

        // 3. Currency
        System.out.print("Enter currency (e.g., OMR, EUR, USD, GBP) [Press Enter for OMR]: ");
        String currency = scanner.nextLine().trim().toUpperCase();
        if (currency.isEmpty()) {
            currency = null; // Let enricher set default (OMR)
        }

        // 4. Build the order
        Order order = new Order();
        order.setCustomerName(name);
        order.setAmount(amount);
        order.setCurrency(currency);

        // 5. place it (this triggers validators + payment + save)
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
}