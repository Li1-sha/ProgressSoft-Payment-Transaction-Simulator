package com.progressoft.web;

import javax.servlet.*;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@WebFilter("/api/orders/*")
public class RateLimiterFilter implements Filter {

    private static final int MAX_REQUESTS = 5;
    private static final long WINDOW_MS = 60_000; // 1 minute

    private RateLimiter rateLimiter;

    @Override
    public void init(FilterConfig filterConfig) {
        rateLimiter = new RateLimiter(MAX_REQUESTS, WINDOW_MS);
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest req = (HttpServletRequest) request;

        if ("POST".equalsIgnoreCase(req.getMethod())) {
            String clientId = getClientId(req); // IP or API key
            if (!rateLimiter.allowRequest(clientId)) {
                HttpServletResponse res = (HttpServletResponse) response;
                res.setStatus(429);
                res.setContentType("application/json");
                res.getWriter().print("{\"error\":\"Too many requests\"}");
                return;
            }
        }
        chain.doFilter(request, response);
    }

    private String getClientId(HttpServletRequest req) {
        // Prefer API key, fallback to IP
        String apiKey = req.getHeader("X-API-Key");
        if (apiKey != null) return "key:" + apiKey;
        return "ip:" + req.getRemoteAddr();
    }

    @Override
    public void destroy() {}
}