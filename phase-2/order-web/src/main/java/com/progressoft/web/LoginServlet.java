package com.progressoft.web;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;

@WebServlet("/api/login")
public class LoginServlet extends HttpServlet {

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String user = req.getParameter("username");
        String pass = req.getParameter("password");

        // Hardcoded credentials (demo only)
        if ("admin".equals(user) && "secret".equals(pass)) {
            HttpSession session = req.getSession(true);
            session.setAttribute("authenticated", true);
            resp.setStatus(HttpServletResponse.SC_OK);
            resp.getWriter().print("{\"status\":\"ok\"}");
        } else {
            resp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            resp.getWriter().print("{\"error\":\"Invalid credentials\"}");
        }
    }
}