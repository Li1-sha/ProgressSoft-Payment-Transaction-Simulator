package com.progressoft.web.auth;

import javax.servlet.http.HttpServletRequest;

public interface AuthStrategy {
    boolean isAuthenticated(HttpServletRequest request);
}