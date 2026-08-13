package com.progressoft.web;

import com.progressoft.domain.Order;
import com.progressoft.exceptions.*;
import com.progressoft.service.OrderService;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@WebServlet("/api/orders")
public class OrderCreateServlet extends HttpServlet {
    private OrderService orderService;

    @Override
    public void init() {
        orderService = ServiceFactory.getOrderService();
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setContentType("application/json");
        try (JsonReader reader = Json.createReader(req.getReader())) {
            JsonObject json = reader.readObject();
            Order order = new Order();
            order.setCustomerName(json.getString("customerName"));
            order.setAmount(json.getJsonNumber("amount").doubleValue());
            order.setCurrency(json.getString("currency"));

            Order placed = orderService.placeOrder(order);

            resp.setStatus(HttpServletResponse.SC_CREATED);
            resp.setHeader("Location", req.getRequestURI() + "/" + placed.getId());
            resp.getWriter().print(OrderListServlet.toJson(placed).build().toString());

        } catch (ValidationFailedException e) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            resp.getWriter().print("{\"error\":\"" + e.getMessage() + "\", \"field\":\"" + e.getFieldName() + "\"}");
        } catch (InsufficientFundsException e) {
            resp.setStatus(402); // Payment Required
            resp.getWriter().print("{\"error\":\"Insufficient funds\", \"required\":" + e.getRequiredAmount() + ", \"available\":" + e.getAvailableAmount() + "}");
        } catch (GatewayTimeoutException e) {
            resp.setStatus(HttpServletResponse.SC_GATEWAY_TIMEOUT);
            resp.getWriter().print("{\"error\":\"Payment gateway timeout\"}");
        } catch (ReconciliationRequiredException e) {
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            resp.getWriter().print("{\"error\":\"Order charged but not persisted, contact support\"}");
        } catch (Exception e) {
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            resp.getWriter().print("{\"error\":\"Internal server error\"}");
        }
    }
}