package com.progressoft.web;

import com.progressoft.domain.Order;
import com.progressoft.exceptions.*;
import com.progressoft.service.OrderService;

import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonReader;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;

/**
 * Single servlet handling all order REST endpoints.
 * <p>
 * GET    /api/orders        → list all orders
 * GET    /api/orders/{id}   → get one order by ID
 * POST   /api/orders        → create a new order
 */
@WebServlet("/api/orders/*")
public class OrderApiServlet extends HttpServlet {

    private OrderService orderService;

    @Override
    public void init() {
        orderService = ServiceFactory.getOrderService();
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String pathInfo = req.getPathInfo();

        // If no path info or just "/", list all orders
        if (pathInfo == null || pathInfo.equals("/")) {
            listAllOrders(resp);
            return;
        }

        // Otherwise try to parse an ID and fetch a single order
        try {
            String idStr = pathInfo.substring(1); // remove leading '/'
            Long id = Long.parseLong(idStr);
            getOrderById(id, resp);
        } catch (NumberFormatException e) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            resp.setContentType("application/json");
            resp.getWriter().print(errorJson("Invalid order ID format"));
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        // POST is only allowed on the base path "/api/orders" (no extra path info)
        String pathInfo = req.getPathInfo();
        if (pathInfo != null && !pathInfo.equals("/")) {
            resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
            resp.setContentType("application/json");
            resp.getWriter().print(errorJson("Not found"));
            return;
        }

        createOrder(req, resp);
    }

    // ---------- Private methods ----------

    private void listAllOrders(HttpServletResponse resp) throws IOException {
        resp.setContentType("application/json");
        try {
            List<Order> orders = orderService.getAllOrders();
            JsonArrayBuilder arrayBuilder = Json.createArrayBuilder();
            for (Order o : orders) {
                arrayBuilder.add(toJson(o));
            }
            try (PrintWriter out = resp.getWriter()) {
                out.print(arrayBuilder.build().toString());
            }
            resp.setStatus(HttpServletResponse.SC_OK);
        } catch (Exception e) {
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            resp.getWriter().print(errorJson("Internal server error: " + e.getMessage()));
        }
    }

    private void getOrderById(Long id, HttpServletResponse resp) throws IOException {
        resp.setContentType("application/json");
        try {
            Order order = orderService.findOrder(id);
            resp.getWriter().print(toJson(order).build().toString());
            resp.setStatus(HttpServletResponse.SC_OK);
        } catch (OrderNotFoundException e) {
            resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
            resp.getWriter().print(errorJson(e.getMessage()));
        } catch (Exception e) {
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            resp.getWriter().print(errorJson("Internal server error: " + e.getMessage()));
        }
    }

    private void createOrder(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setContentType("application/json");
        try (JsonReader reader = Json.createReader(req.getReader())) {
            JsonObject json = reader.readObject();

            Order order = new Order();
            order.setCustomerName(json.getString("customerName"));
            order.setAmount(json.getJsonNumber("amount").doubleValue());
            order.setCurrency(json.getString("currency"));

            Order placed = orderService.placeOrder(order);

            // 201 Created with the persisted order
            resp.setStatus(HttpServletResponse.SC_CREATED);
            resp.setHeader("Location", req.getRequestURI() + "/" + placed.getId());
            resp.getWriter().print(toJson(placed).build().toString());

        } catch (ValidationFailedException e) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            resp.getWriter().print(errorJson(e.getMessage() + " (field: " + e.getFieldName() + ")"));
        } catch (InsufficientFundsException e) {
            resp.setStatus(402); // Payment Required
            resp.getWriter().print(errorJson("Insufficient funds: required " + e.getRequiredAmount() +
                    ", available " + e.getAvailableAmount()));
        } catch (GatewayTimeoutException e) {
            resp.setStatus(HttpServletResponse.SC_GATEWAY_TIMEOUT);
            resp.getWriter().print(errorJson("Payment gateway timeout"));
        } catch (ReconciliationRequiredException e) {
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            resp.getWriter().print(errorJson("Order charged but not persisted – contact support"));
        } catch (Exception e) {
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            resp.getWriter().print(errorJson("Internal server error: " + e.getMessage()));
        }
    }

    // ---------- JSON helpers ----------

    static JsonObjectBuilder toJson(Order order) {
        return Json.createObjectBuilder()
                .add("id", order.getId())
                .add("customerName", order.getCustomerName())
                .add("amount", order.getAmount())
                .add("currency", order.getCurrency());
    }

    private String errorJson(String message) {
        return Json.createObjectBuilder()
                .add("error", message)
                .build()
                .toString();
    }
}