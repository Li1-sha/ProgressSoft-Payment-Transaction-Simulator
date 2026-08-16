package com.progressoft.web.auth;

import javax.servlet.http.HttpServletRequest;
import java.util.Set;

public class ApiKeyAuthStrategy implements AuthStrategy {
    private static final Set<String> VALID_KEYS = Set.of("key-123", "key-456", "key-789");

    @Override
    public boolean isAuthenticated(HttpServletRequest request) {
        String apiKey = request.getHeader("X-API-Key");
        return apiKey != null && VALID_KEYS.contains(apiKey);
    }
}