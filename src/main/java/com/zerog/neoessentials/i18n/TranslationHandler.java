package com.zerog.neoessentials.i18n;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.OutputStream;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.format.FormatStyle;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * REST API handler for internationalization and localization.
 * Provides language selection and translation retrieval.
 */
public class TranslationHandler implements HttpHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(TranslationHandler.class);
    private final Gson gson = new Gson();
    
    // Store user language preferences by session — HttpServer dispatches concurrent
    // requests to this single shared handler instance, so this must be concurrent-safe.
    private final Map<String, String> userLanguages = new ConcurrentHashMap<>();
    
    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String method = exchange.getRequestMethod();
        String path = exchange.getRequestURI().getPath();
        
        try {
            // Remove /api/i18n prefix
            String endpoint = path.replace("/api/i18n", "");
            
            if (endpoint.isEmpty() || endpoint.equals("/")) {
                endpoint = "/languages";
            }
            
            switch (endpoint) {
                case "/languages":
                    if ("GET".equals(method)) {
                        handleGetLanguages(exchange);
                    } else {
                        sendMethodNotAllowed(exchange);
                    }
                    break;
                    
                case "/translations":
                    if ("GET".equals(method)) {
                        handleGetTranslations(exchange);
                    } else {
                        sendMethodNotAllowed(exchange);
                    }
                    break;
                    
                case "/set":
                    if ("POST".equals(method)) {
                        handleSetLanguage(exchange);
                    } else {
                        sendMethodNotAllowed(exchange);
                    }
                    break;
                    
                case "/get":
                    if ("GET".equals(method)) {
                        handleGetLanguage(exchange);
                    } else {
                        sendMethodNotAllowed(exchange);
                    }
                    break;
                    
                case "/translate":
                    if ("GET".equals(method)) {
                        handleTranslate(exchange);
                    } else {
                        sendMethodNotAllowed(exchange);
                    }
                    break;
                    
                case "/format":
                    if ("GET".equals(method)) {
                        handleFormat(exchange);
                    } else {
                        sendMethodNotAllowed(exchange);
                    }
                    break;
                    
                case "/reload":
                    if ("POST".equals(method)) {
                        handleReload(exchange);
                    } else {
                        sendMethodNotAllowed(exchange);
                    }
                    break;
                    
                default:
                    sendNotFound(exchange);
                    break;
            }
        } catch (Exception e) {
            LOGGER.error("Error handling translation request", e);
            sendError(exchange, "Internal server error: " + e.getMessage());
        }
    }
    
    /**
     * GET /api/i18n/languages
     * Get list of available languages
     */
    private void handleGetLanguages(HttpExchange exchange) throws IOException {
        List<LocalizationManager.LanguageInfo> languages = 
            LocalizationManager.getInstance().getAvailableLanguages();
        
        JsonObject response = new JsonObject();
        response.addProperty("success", true);
        response.addProperty("count", languages.size());
        
        JsonArray languagesArray = new JsonArray();
        for (LocalizationManager.LanguageInfo lang : languages) {
            JsonObject langObj = new JsonObject();
            langObj.addProperty("code", lang.getCode());
            langObj.addProperty("nativeName", lang.getNativeName());
            langObj.addProperty("englishName", lang.getEnglishName());
            langObj.addProperty("countryCode", lang.getCountryCode());
            langObj.addProperty("rtl", lang.isRtl());
            languagesArray.add(langObj);
        }
        response.add("languages", languagesArray);
        
        sendJsonResponse(exchange, 200, response);
    }
    
    /**
     * GET /api/i18n/translations?language=en_us
     * Get all translations for a language
     */
    private void handleGetTranslations(HttpExchange exchange) throws IOException {
        Map<String, String> params = parseQueryParams(exchange.getRequestURI().getQuery());
        String language = params.getOrDefault("language", "en_us");
        
        if (!LocalizationManager.getInstance().isLanguageSupported(language)) {
            sendBadRequest(exchange, "Unsupported language: " + language);
            return;
        }
        
        Map<String, String> translations = 
            LocalizationManager.getInstance().getTranslations(language);
        
        JsonObject response = new JsonObject();
        response.addProperty("success", true);
        response.addProperty("language", language);
        response.addProperty("count", translations.size());
        response.addProperty("rtl", LocalizationManager.getInstance().isRTL(language));
        
        JsonObject translationsObj = new JsonObject();
        translations.forEach(translationsObj::addProperty);
        response.add("translations", translationsObj);
        
        sendJsonResponse(exchange, 200, response);
    }
    
    /**
     * POST /api/i18n/set
     * Set user language preference
     * Body: {"session": "sessionId", "language": "en_us"}
     */
    private void handleSetLanguage(HttpExchange exchange) throws IOException {
        try {
            String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            JsonObject request = gson.fromJson(body, JsonObject.class);
            
            String session = request.has("session") ? request.get("session").getAsString() : null;
            String language = request.has("language") ? request.get("language").getAsString() : null;
            
            if (session == null || session.isEmpty()) {
                sendBadRequest(exchange, "Missing 'session' field");
                return;
            }
            if (language == null || language.isEmpty()) {
                sendBadRequest(exchange, "Missing 'language' field");
                return;
            }
            
            if (!LocalizationManager.getInstance().isLanguageSupported(language)) {
                sendBadRequest(exchange, "Unsupported language: " + language);
                return;
            }
            
            userLanguages.put(session, language);
            
            JsonObject response = new JsonObject();
            response.addProperty("success", true);
            response.addProperty("message", "Language preference saved");
            response.addProperty("session", session);
            response.addProperty("language", language);
            response.addProperty("rtl", LocalizationManager.getInstance().isRTL(language));
            
            sendJsonResponse(exchange, 200, response);
            
        } catch (Exception e) {
            LOGGER.error("Failed to set language", e);
            sendError(exchange, "Failed to set language: " + e.getMessage());
        }
    }
    
    /**
     * GET /api/i18n/get?session=sessionId
     * Get user language preference
     */
    private void handleGetLanguage(HttpExchange exchange) throws IOException {
        Map<String, String> params = parseQueryParams(exchange.getRequestURI().getQuery());
        String session = params.get("session");
        
        if (session == null || session.isEmpty()) {
            sendBadRequest(exchange, "Missing 'session' parameter");
            return;
        }
        
        String language = userLanguages.getOrDefault(session, "en_us");
        
        JsonObject response = new JsonObject();
        response.addProperty("success", true);
        response.addProperty("session", session);
        response.addProperty("language", language);
        response.addProperty("rtl", LocalizationManager.getInstance().isRTL(language));
        
        sendJsonResponse(exchange, 200, response);
    }
    
    /**
     * GET /api/i18n/translate?key=dashboard.title&language=en_us
     * Translate a single key
     */
    private void handleTranslate(HttpExchange exchange) throws IOException {
        Map<String, String> params = parseQueryParams(exchange.getRequestURI().getQuery());
        String key = params.get("key");
        String language = params.getOrDefault("language", "en_us");
        
        if (key == null || key.isEmpty()) {
            sendBadRequest(exchange, "Missing 'key' parameter");
            return;
        }
        
        String translation = LocalizationManager.getInstance().translate(key, language);
        
        JsonObject response = new JsonObject();
        response.addProperty("success", true);
        response.addProperty("key", key);
        response.addProperty("language", language);
        response.addProperty("translation", translation);
        
        sendJsonResponse(exchange, 200, response);
    }
    
    /**
     * GET /api/i18n/format?type=datetime&value=timestamp&language=en_us&style=MEDIUM
     * Format date/time or number according to locale
     */
    private void handleFormat(HttpExchange exchange) throws IOException {
        Map<String, String> params = parseQueryParams(exchange.getRequestURI().getQuery());
        String type = params.get("type");
        String value = params.get("value");
        String language = params.getOrDefault("language", "en_us");
        String style = params.getOrDefault("style", "MEDIUM");
        
        if (type == null || type.isEmpty()) {
            sendBadRequest(exchange, "Missing 'type' parameter");
            return;
        }
        if (value == null || value.isEmpty()) {
            sendBadRequest(exchange, "Missing 'value' parameter");
            return;
        }
        
        String formatted;
        
        try {
            FormatStyle formatStyle = FormatStyle.valueOf(style.toUpperCase());
            
            switch (type.toLowerCase()) {
                case "datetime":
                    Instant instant = Instant.parse(value);
                    formatted = LocalizationManager.getInstance()
                        .formatDateTime(instant, language, formatStyle);
                    break;
                    
                case "date":
                    instant = Instant.parse(value);
                    formatted = LocalizationManager.getInstance()
                        .formatDate(instant, language, formatStyle);
                    break;
                    
                case "time":
                    instant = Instant.parse(value);
                    formatted = LocalizationManager.getInstance()
                        .formatTime(instant, language, formatStyle);
                    break;
                    
                case "number":
                    Number number = Double.parseDouble(value);
                    formatted = LocalizationManager.getInstance()
                        .formatNumber(number, language);
                    break;
                    
                case "currency":
                    number = Double.parseDouble(value);
                    formatted = LocalizationManager.getInstance()
                        .formatCurrency(number, language);
                    break;
                    
                case "percent":
                    number = Double.parseDouble(value);
                    formatted = LocalizationManager.getInstance()
                        .formatPercent(number, language);
                    break;
                    
                default:
                    sendBadRequest(exchange, "Invalid format type: " + type);
                    return;
            }
            
            JsonObject response = new JsonObject();
            response.addProperty("success", true);
            response.addProperty("type", type);
            response.addProperty("value", value);
            response.addProperty("language", language);
            response.addProperty("formatted", formatted);
            
            sendJsonResponse(exchange, 200, response);
            
        } catch (Exception e) {
            LOGGER.error("Failed to format value", e);
            sendError(exchange, "Failed to format value: " + e.getMessage());
        }
    }
    
    /**
     * POST /api/i18n/reload
     * Reload language files
     */
    private void handleReload(HttpExchange exchange) throws IOException {
        // MessageUtil.translations (mutated by reload()) is read from the main thread on
        // every chat message/command response, so the clear+repopulate must not run on this
        // HTTP thread concurrently with that. This handler has no MinecraftServer reference
        // of its own, so marshal via ServerLifecycleHooks like ShopTransaction's low-stock
        // notification does; if the server isn't up yet (e.g. reload called during early
        // boot), there's no main thread contending for the map, so run inline.
        net.minecraft.server.MinecraftServer server =
                net.neoforged.neoforge.server.ServerLifecycleHooks.getCurrentServer();
        if (server != null) {
            try {
                server.submit(() -> LocalizationManager.getInstance().reload()).get();
            } catch (Exception e) {
                LOGGER.error("Failed to reload translations on main thread", e);
                sendError(exchange, "Failed to reload translations: " + e.getMessage());
                return;
            }
        } else {
            LocalizationManager.getInstance().reload();
        }

        JsonObject response = new JsonObject();
        response.addProperty("success", true);
        response.addProperty("message", "Language files reloaded");
        response.addProperty("timestamp", Instant.now().toString());

        sendJsonResponse(exchange, 200, response);
    }
    
    /**
     * Parse query parameters from URL
     */
    private Map<String, String> parseQueryParams(String query) {
        Map<String, String> params = new HashMap<>();
        if (query == null || query.isEmpty()) {
            return params;
        }
        
        for (String param : query.split("&")) {
            String[] pair = param.split("=", 2);
            if (pair.length == 2) {
                try {
                    String key = URLDecoder.decode(pair[0], StandardCharsets.UTF_8);
                    String value = URLDecoder.decode(pair[1], StandardCharsets.UTF_8);
                    params.put(key, value);
                } catch (Exception e) {
                    LOGGER.warn("Failed to decode parameter: {}", param);
                }
            }
        }
        
        return params;
    }
    
    private void sendJsonResponse(HttpExchange exchange, int statusCode, JsonObject response) throws IOException {
        String jsonResponse = gson.toJson(response);
        byte[] bytes = jsonResponse.getBytes(StandardCharsets.UTF_8);
        
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        exchange.sendResponseHeaders(statusCode, bytes.length);
        
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }
    
    private void sendMethodNotAllowed(HttpExchange exchange) throws IOException {
        JsonObject response = new JsonObject();
        response.addProperty("success", false);
        response.addProperty("error", "Method not allowed");
        response.addProperty("timestamp", Instant.now().toString());
        sendJsonResponse(exchange, 405, response);
    }
    
    private void sendNotFound(HttpExchange exchange) throws IOException {
        JsonObject response = new JsonObject();
        response.addProperty("success", false);
        response.addProperty("error", "Resource not found");
        response.addProperty("timestamp", Instant.now().toString());
        sendJsonResponse(exchange, 404, response);
    }
    
    private void sendBadRequest(HttpExchange exchange, String message) throws IOException {
        JsonObject response = new JsonObject();
        response.addProperty("success", false);
        response.addProperty("error", message);
        response.addProperty("timestamp", Instant.now().toString());
        sendJsonResponse(exchange, 400, response);
    }
    
    private void sendError(HttpExchange exchange, String message) throws IOException {
        JsonObject response = new JsonObject();
        response.addProperty("success", false);
        response.addProperty("error", message);
        response.addProperty("timestamp", Instant.now().toString());
        sendJsonResponse(exchange, 500, response);
    }
}
