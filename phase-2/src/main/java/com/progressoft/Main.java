package com.progressoft;

import com.progressoft.domain.Order;
import com.progressoft.payment.PaymentGateway;
import com.progressoft.repository.OrderRepository;
import com.progressoft.repository.inmemory.InMemoryOrderRepository;
import com.progressoft.service.OrderService;
import com.progressoft.validation.OrderEnricher;
import com.progressoft.validation.PaymentValidator;
import com.progressoft.validation.Validators;

public class Main {

    public static void main(String[] args) {
        // 1. Setup Repository
        OrderRepository repository = new InMemoryOrderRepository();

        // 2. Setup Payment Gateway (Dummy)
        PaymentGateway paymentGateway = order ->
                System.out.println("💰 Charging $" + order.getAmount() +
                        " in " + order.getCurrency());

        // 3. Build Composers (OrderEnricher composition)
        OrderEnricher enricher = Validators.defaultCurrency("USD")
                .andThen(Validators.timestampEnricher());

        // 4. Build the Composed Validator (No if-chain!)
        PaymentValidator composedValidator = Validators.positiveAmount()
                .and(Validators.maxLimit(10000.0))
                .and(Validators.currencyCheck("USD", "EUR", "GBP"));

        // 5. Wire Service
        OrderService service = new OrderService(
                repository,
                paymentGateway,
                composedValidator, // Single composed rule
                enricher
        );

        // --- Test 1: Valid Order (should pass) ---
        Order validOrder = new Order();
        validOrder.setCustomerName("Alice");
        validOrder.setAmount(500.0);
        validOrder.setCurrency("USD"); // Valid currency

        Order placed = service.placeOrder(validOrder);
        System.out.println("Valid order placed. ID: " + placed.getId());

        // --- Test 2: Invalid Amount (Negative) ---
        try {
            Order invalidOrder = new Order();
            invalidOrder.setCustomerName("Bob");
            invalidOrder.setAmount(-10.0);
            invalidOrder.setCurrency("USD");
            service.placeOrder(invalidOrder);
        } catch (RuntimeException e) {
            System.out.println("Caught expected exception: " + e.getMessage());
        }

        // --- Test 3: Invalid Currency ---
        try {
            Order invalidCurrencyOrder = new Order();
            invalidCurrencyOrder.setCustomerName("Charlie");
            invalidCurrencyOrder.setAmount(100.0);
            invalidCurrencyOrder.setCurrency("JPY"); // Not allowed
            service.placeOrder(invalidCurrencyOrder);
        } catch (RuntimeException e) {
            System.out.println("Caught expected exception: " + e.getMessage());
        }
    }
}