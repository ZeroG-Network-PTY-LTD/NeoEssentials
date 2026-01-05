package com.zerog.neoessentials.webdashboard.api.endpoints;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.OutputStream;

/**
 * Texture endpoint - Currently disabled
 * Returns 501 Not Implemented for all texture requests
 */
public class TextureEndpoint implements HttpHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(TextureEndpoint.class);

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        LOGGER.debug("TextureEndpoint handling request: {} {}", exchange.getRequestMethod(), exchange.getRequestURI().getPath());

        try {
            // Texture functionality has been removed
            sendError(exchange, 501, "Texture endpoint is disabled");
        } catch (Exception e) {
            LOGGER.error("Error handling texture request", e);
            try {
                sendError(exchange, 500, "Internal server error");
            } catch (IOException ignored) {
            }
        } finally {
            try {
                exchange.close();
            } catch (Exception ignored) {
            }
        }
    }

    private void sendError(HttpExchange exchange, int statusCode, String message) throws IOException {
        byte[] response = message.getBytes();
        exchange.getResponseHeaders().set("Content-Type", "text/plain");
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.sendResponseHeaders(statusCode, response.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(response);
        }
    }
}
