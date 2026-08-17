package com.progressoft.web;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.webapp.WebAppContext;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AuthFilterIntegrationTest {

    private static Server server;
    private static int port = 8081; // avoid conflict

    @BeforeAll
    static void startJetty() throws Exception {
        server = new Server(port);
        WebAppContext context = new WebAppContext();
        context.setContextPath("/");
        context.setWar("order-web/target/order-web-1.0-SNAPSHOT.war");
        // or use the exploded directory
        context.setResourceBase("order-web/src/main/webapp");
        server.setHandler(context);
        server.start();
        // Wait for startup
        System.out.println("Jetty started on port " + port);
    }

    @AfterAll
    static void stopJetty() throws Exception {
        if (server != null) {
            server.stop();
        }
    }

    private HttpURLConnection sendRequest(String method, String path, String body, String apiKey) throws Exception {
        URL url = new URL("http://localhost:" + port + path);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod(method);
        if (apiKey != null) {
            conn.setRequestProperty("X-API-Key", apiKey);
        }
        conn.setDoOutput(true);
        if (body != null) {
            try (OutputStream os = conn.getOutputStream()) {
                os.write(body.getBytes());
                os.flush();
            }
        }
        conn.connect();
        return conn;
    }

    private HttpURLConnection sendRequest(String method, String path, String body) throws Exception {
        return sendRequest(method, path, body, null);
    }

    @Test
    void unauthenticatedPostReturns401() throws Exception {
        HttpURLConnection conn = sendRequest("POST", "/api/orders",
                "{\"customerName\":\"Test\",\"amount\":100,\"currency\":\"USD\"}");
        assertEquals(401, conn.getResponseCode());
        conn.disconnect();
    }

    @Test
    void authenticatedWithSessionReturnsNot401() throws Exception {
        // First login to get session
        HttpURLConnection loginConn = sendRequest("POST", "/api/login",
                "username=admin&password=secret", null);
        String sessionCookie = loginConn.getHeaderField("Set-Cookie");
        loginConn.disconnect();

        // Now POST with session cookie
        HttpURLConnection conn = sendRequest("POST", "/api/orders",
                "{\"customerName\":\"SessionTest\",\"amount\":150,\"currency\":\"OMR\"}", null);
        // Manually set the cookie
        conn.setRequestProperty("Cookie", sessionCookie);
        assertEquals(201, conn.getResponseCode()); // or 200 if validation fails, but should be created
        conn.disconnect();
    }

    @Test
    void authenticatedWithApiKeyReturnsNot401() throws Exception {
        HttpURLConnection conn = sendRequest("POST", "/api/orders",
                "{\"customerName\":\"KeyTest\",\"amount\":200,\"currency\":\"EUR\"}",
                "key-123");
        int status = conn.getResponseCode();
        assertTrue(status != 401, "Expected not 401, got " + status);
        conn.disconnect();
    }

    @Test
    void unauthenticatedGetWorks() throws Exception {
        HttpURLConnection conn = sendRequest("GET", "/api/orders", null);
        assertEquals(200, conn.getResponseCode());
        conn.disconnect();
    }
}