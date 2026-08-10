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

public class Main {

    public static void main(String[] args) {
        // 1. Setup Repository
        OrderRepository repository = new InMemoryOrderRepository();

        // 2. Setup Payment Gateway (Dummy)
        PaymentGateway paymentGateway = order ->
                System.out.println("Charging " + order.getAmount() +
                        " in " + order.getCurrency());

        // 3. Build Composers (OrderEnricher composition)
        OrderEnricher enricher = Validators.defaultCurrency("OMR")
                .andThen(Validators.timestampEnricher());

        // 4. Build the Composed Validator
        PaymentValidator composedValidator = Validators.positiveAmount()
                .and(Validators.maxLimit(10000.0))
                .and(Validators.currencyCheck("OMR", "EUR", "USD"));

        // 5. Wire Service
        OrderService service = new OrderService(
                repository,
                paymentGateway,
                composedValidator,
                enricher
        );

        // --- Test 1: Valid Order ---
        try {
            Order validOrder = new Order();
            validOrder.setCustomerName("Ahmed");
            validOrder.setAmount(500.0);
            validOrder.setCurrency("OMR");

            Order placed = service.placeOrder(validOrder);
            System.out.println(" Valid order placed. ID: " + placed.getId());
        } catch (ValidationFailedException | InsufficientFundsException | GatewayTimeoutException e) {
            System.out.println(" Test 1 failed unexpectedly: " + e.getMessage());
        }

        // --- Test 2: Invalid Amount (Negative) ---
        try {
            Order invalidOrder = new Order();
            invalidOrder.setCustomerName("Ali");
            invalidOrder.setAmount(-10.0);
            invalidOrder.setCurrency("OMR");
            service.placeOrder(invalidOrder);
        } catch (ValidationFailedException e) {
            System.out.println(" Caught expected ValidationFailedException: " + e.getMessage());
            System.out.println("   Field: " + e.getFieldName() + " | Rejected: " + e.getRejectedValue());
        } catch (InsufficientFundsException | GatewayTimeoutException e) {
            System.out.println(" Unexpected payment exception: " + e.getMessage());
        }

        // --- Test 3: Invalid Currency ---
        try {
            Order invalidCurrencyOrder = new Order();
            invalidCurrencyOrder.setCustomerName("Sara");
            invalidCurrencyOrder.setAmount(100.0);
            invalidCurrencyOrder.setCurrency("JPY"); // Not allowed
            service.placeOrder(invalidCurrencyOrder);
        } catch (ValidationFailedException e) {
            System.out.println(" Caught expected ValidationFailedException: " + e.getMessage());
            System.out.println("   Field: " + e.getFieldName() + " | Rejected: " + e.getRejectedValue());
        } catch (InsufficientFundsException | GatewayTimeoutException e) {
            System.out.println(" Unexpected payment exception: " + e.getMessage());
        }
    }
}