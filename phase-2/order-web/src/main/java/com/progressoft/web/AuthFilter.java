package com.progressoft.web;

import com.progressoft.web.auth.ApiKeyAuthStrategy;
import com.progressoft.web.auth.AuthStrategy;
import com.progressoft.web.auth.SessionAuthStrategy;

import javax.servlet.*;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;

@WebFilter("/api/orders/*")
public class AuthFilter implements Filter {

    private final List<AuthStrategy> strategies = List.of(
            new SessionAuthStrategy(),
            new ApiKeyAuthStrategy()
    );

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse res = (HttpServletResponse) response;

        if ("POST".equalsIgnoreCase(req.getMethod())) {
            boolean authenticated = strategies.stream()
                    .anyMatch(s -> s.isAuthenticated(req));
            if (!authenticated) {
                res.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                res.setContentType("application/json");
                res.getWriter().print("{\"error\":\"Authentication required\"}");
                return;
            }
        }
        chain.doFilter(request, response);
    }
}