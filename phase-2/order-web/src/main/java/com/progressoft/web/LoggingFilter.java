package com.progressoft.web;

import javax.servlet.*;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@WebFilter("/orders")
public class LoggingFilter implements Filter {

    @Override
    public void init(FilterConfig filterConfig) {
        System.out.println("LoggingFilter initialized.");
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response,
                         FilterChain chain) throws IOException, ServletException {
        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse res = (HttpServletResponse) response;

        long start = System.nanoTime();
        String method = req.getMethod();
        String path = req.getRequestURI();

        try {
            chain.doFilter(request, response);  // must call chain
        } finally {
            long duration = System.nanoTime() - start;
            int status = res.getStatus();
            System.out.printf(">>> %s %s -> %d in %d ms%n",
                    method, path, status, duration / 1_000_000);
        }
    }

    @Override
    public void destroy() {
        System.out.println("LoggingFilter destroyed.");
    }
}