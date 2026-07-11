package com.zerog.neoessentials.webdashboard.util;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/**
 * Safely parses a dashboard API request body into a {@link JsonObject}.
 *
 * <p>Several HTTP client libraries (notably Laravel's, used by the standalone
 * NeoEssentials dashboard) serialise an empty parameter list as the JSON array
 * {@code []} rather than the empty object {@code {}} — PHP has no distinct
 * empty-array/empty-object literal. A bare {@code JsonParser.parseString(body)
 * .getAsJsonObject()} throws {@code IllegalStateException: Not a JSON Object: []}
 * on that input, 500ing every parameterless POST route (e.g. reload, test-connection,
 * enable/disable) whenever the caller sends {@code []} instead of {@code {}} or an
 * empty body. Blank bodies and arrays are both treated as "no parameters" here.
 */
public final class RequestBodyUtil {
    private RequestBodyUtil() {
    }

    public static JsonObject readJsonObject(HttpExchange exchange) throws IOException {
        try (InputStream is = exchange.getRequestBody()) {
            String body = new String(is.readAllBytes(), StandardCharsets.UTF_8).trim();
            return parseJsonObject(body);
        }
    }

    public static JsonObject parseJsonObject(String body) {
        if (body == null || body.isBlank()) {
            return new JsonObject();
        }

        JsonElement parsed = JsonParser.parseString(body);
        return parsed.isJsonObject() ? parsed.getAsJsonObject() : new JsonObject();
    }
}
