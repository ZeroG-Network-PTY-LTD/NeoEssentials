package com.zerog.neoessentials.webdashboard.api.endpoints;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.packs.resources.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Optional;

/**
 * Serves item/block textures from the server's loaded resource packs
 * This allows the dashboard to display textures for modded items without external APIs
 *
 * Endpoints:
 * - GET /api/textures/item/{namespace}/{path} - Get item texture
 * - GET /api/textures/block/{namespace}/{path} - Get block texture
 */
public class TextureEndpoint implements HttpHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(TextureEndpoint.class);
    private final MinecraftServer server;

    public TextureEndpoint(MinecraftServer server) {
        this.server = server;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String path = exchange.getRequestURI().getPath();
        String method = exchange.getRequestMethod();

        LOGGER.debug("TextureEndpoint handling request: {} {}", method, path);

        try {
            // Only allow GET requests
            if (!"GET".equals(method)) {
                sendError(exchange, 405, "Method not allowed");
                return;
            }

            // Parse path: /api/textures/item/{namespace}/{item_path}
            if (!path.startsWith("/api/textures/")) {
                sendError(exchange, 404, "Invalid texture path");
                return;
            }

            String[] parts = path.substring("/api/textures/".length()).split("/");
            if (parts.length < 3) {
                sendError(exchange, 400, "Invalid texture path format. Expected: /api/textures/{type}/{namespace}/{path}");
                return;
            }

            String type = parts[0]; // "item" or "block"
            String namespace = parts[1];
            String itemPath = parts[2];

            // Construct resource location for texture
            // Item textures are at: assets/{namespace}/textures/item/{path}.png
            // Block textures are at: assets/{namespace}/textures/block/{path}.png
            String textureFolder = type.equals("block") ? "block" : "item";
            ResourceLocation textureLocation = ResourceLocation.fromNamespaceAndPath(
                namespace,
                "textures/" + textureFolder + "/" + itemPath + ".png"
            );

            LOGGER.debug("Looking for texture: {}", textureLocation);

            // Try to get texture from server's resource manager
            Optional<Resource> resourceOpt = server.getResourceManager().getResource(textureLocation);

            if (resourceOpt.isPresent()) {
                Resource resource = resourceOpt.get();

                try (InputStream in = resource.open()) {
                    byte[] textureData = in.readAllBytes();

                    // Send texture as PNG image
                    exchange.getResponseHeaders().set("Content-Type", "image/png");
                    exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
                    exchange.getResponseHeaders().set("Cache-Control", "public, max-age=86400"); // Cache for 24 hours
                    exchange.sendResponseHeaders(200, textureData.length);

                    try (OutputStream os = exchange.getResponseBody()) {
                        os.write(textureData);
                    }

                    LOGGER.debug("Successfully served texture: {} ({} bytes)", textureLocation, textureData.length);
                }
            } else {
                LOGGER.debug("Texture not found: {}", textureLocation);
                sendError(exchange, 404, "Texture not found: " + textureLocation);
            }

        } catch (Exception e) {
            LOGGER.error("Error serving texture for path: {}", path, e);
            sendError(exchange, 500, "Internal server error: " + e.getMessage());
        } finally {
            try {
                exchange.close();
            } catch (Exception e) {
                // Ignore
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

