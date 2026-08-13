package com.progressoft.web;

import com.progressoft.domain.Order;
import com.progressoft.service.OrderService;

import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObjectBuilder;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;

@WebServlet("/api/orders")
public class OrderListServlet extends HttpServlet {
    private OrderService orderService;

    @Override
    public void init() {
        orderService = ServiceFactory.getOrderService();
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
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

    static JsonObjectBuilder toJson(Order order) {
        return Json.createObjectBuilder()
                .add("id", order.getId())
                .add("customerName", order.getCustomerName())
                .add("amount", order.getAmount())
                .add("currency", order.getCurrency());
    }

    private String errorJson(String msg) {
        return Json.createObjectBuilder().add("error", msg).build().toString();
    }
}