package com.progressoft;

import com.progressoft.domain.Order;
import com.progressoft.payment.PaymentGateway;
import com.progressoft.repository.OrderRepository;
import com.progressoft.repository.inmemory.InMemoryOrderRepository;
import com.progressoft.service.OrderService;

public class Main {

    public static void main(String[] args) {
        // 1. Wire dependencies
        OrderRepository repository = new InMemoryOrderRepository();

        // Dummy PaymentGateway implementation
        PaymentGateway paymentGateway = order -> {
            System.out.println("Charging $" + order.getAmount() +
                    " for customer: " + order.getCustomerName());
        };

        OrderService service = new OrderService(repository, paymentGateway);

        // 2. Create new order (No ID assigned yet)
        Order order = new Order();
        order.setCustomerName("Sara Raisi");
        order.setAmount(150.50);

        // 3. Place order (Repository generates ID = 1 automatically)
        Order placed = service.placeOrder(order);
        System.out.println("   Order placed successfully!");
        System.out.println("   Order ID: " + placed.getId());
        System.out.println("   Customer: " + placed.getCustomerName());

        // 4. Retrieve by ID
        Order found = service.findOrder(1L);
        System.out.println("\n Order retrieved:");
        System.out.println("   Amount: $" + found.getAmount());

        // 5. Check total count
        System.out.println("\n Total orders in system: " + service.getTotalOrders());
    }
}