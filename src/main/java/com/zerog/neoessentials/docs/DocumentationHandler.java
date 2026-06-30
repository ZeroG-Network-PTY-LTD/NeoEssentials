package com.zerog.neoessentials.docs;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.OutputStream;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * REST API handler for documentation endpoints
 */
public class DocumentationHandler implements HttpHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(DocumentationHandler.class);
    private final Gson gson = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    private final DocumentationManager docManager = DocumentationManager.getInstance();
    
    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String method = exchange.getRequestMethod();
        String path = exchange.getRequestURI().getPath();
        
        try {
            // Route requests
            if (path.equals("/api/docs/sections") && method.equals("GET")) {
                handleGetSections(exchange);
            } else if (path.startsWith("/api/docs/sections/") && method.equals("GET")) {
                handleGetSection(exchange, path);
            } else if (path.equals("/api/docs/api") && method.equals("GET")) {
                handleGetApiEndpoints(exchange);
            } else if (path.startsWith("/api/docs/api/") && method.equals("GET")) {
                handleGetApiEndpoint(exchange, path);
            } else if (path.equals("/api/docs/tutorials") && method.equals("GET")) {
                handleGetTutorials(exchange);
            } else if (path.startsWith("/api/docs/tutorials/") && method.equals("GET")) {
                handleGetTutorial(exchange, path);
            } else if (path.equals("/api/docs/faq") && method.equals("GET")) {
                handleGetFaq(exchange);
            } else if (path.equals("/api/docs/faq/search") && method.equals("GET")) {
                handleSearchFaq(exchange);
            } else if (path.equals("/api/docs/videos") && method.equals("GET")) {
                handleGetVideos(exchange);
            } else if (path.startsWith("/api/docs/videos/") && method.equals("GET")) {
                handleGetVideo(exchange, path);
            } else if (path.equals("/api/docs/search") && method.equals("GET")) {
                handleSearchAll(exchange);
            } else {
                sendResponse(exchange, 404, Map.of(
                        "success", false,
                        "error", "Endpoint not found"
                ));
            }
        } catch (Exception e) {
            LOGGER.error("Error handling documentation request", e);
            sendResponse(exchange, 500, Map.of(
                    "success", false,
                    "error", "Internal server error: " + e.getMessage()
            ));
        }
    }
    
    /**
     * GET /api/docs/sections - Get all documentation sections
     */
    private void handleGetSections(HttpExchange exchange) throws IOException {
        Map<String, DocumentationManager.DocumentationSection> sections = docManager.getAllSections();
        
        sendResponse(exchange, 200, Map.of(
                "success", true,
                "count", sections.size(),
                "sections", sections.values()
        ));
    }
    
    /**
     * GET /api/docs/sections/{id} - Get specific documentation section
     */
    private void handleGetSection(HttpExchange exchange, String path) throws IOException {
        String sectionId = path.substring("/api/docs/sections/".length());
        DocumentationManager.DocumentationSection section = docManager.getSection(sectionId);
        
        if (section == null) {
            sendResponse(exchange, 404, Map.of(
                    "success", false,
                    "error", "Section not found: " + sectionId
            ));
            return;
        }
        
        sendResponse(exchange, 200, Map.of(
                "success", true,
                "section", section
        ));
    }
    
    /**
     * GET /api/docs/api - Get all API endpoint documentation
     */
    private void handleGetApiEndpoints(HttpExchange exchange) throws IOException {
        Map<String, DocumentationManager.ApiEndpoint> endpoints = docManager.getAllApiEndpoints();
        
        sendResponse(exchange, 200, Map.of(
                "success", true,
                "count", endpoints.size(),
                "endpoints", endpoints.values()
        ));
    }
    
    /**
     * GET /api/docs/api/{endpoint} - Get specific API endpoint documentation
     */
    private void handleGetApiEndpoint(HttpExchange exchange, String path) throws IOException {
        String endpoint = path.substring("/api/docs/api".length());
        if (!endpoint.startsWith("/")) {
            endpoint = "/" + endpoint;
        }
        
        DocumentationManager.ApiEndpoint apiEndpoint = docManager.getApiEndpoint(endpoint);
        
        if (apiEndpoint == null) {
            sendResponse(exchange, 404, Map.of(
                    "success", false,
                    "error", "API endpoint documentation not found: " + endpoint
            ));
            return;
        }
        
        sendResponse(exchange, 200, Map.of(
                "success", true,
                "endpoint", apiEndpoint
        ));
    }
    
    /**
     * GET /api/docs/tutorials - Get all tutorials
     */
    private void handleGetTutorials(HttpExchange exchange) throws IOException {
        List<DocumentationManager.Tutorial> tutorials = docManager.getAllTutorials();
        
        sendResponse(exchange, 200, Map.of(
                "success", true,
                "count", tutorials.size(),
                "tutorials", tutorials
        ));
    }
    
    /**
     * GET /api/docs/tutorials/{id} - Get specific tutorial
     */
    private void handleGetTutorial(HttpExchange exchange, String path) throws IOException {
        String tutorialId = path.substring("/api/docs/tutorials/".length());
        DocumentationManager.Tutorial tutorial = docManager.getTutorial(tutorialId);
        
        if (tutorial == null) {
            sendResponse(exchange, 404, Map.of(
                    "success", false,
                    "error", "Tutorial not found: " + tutorialId
            ));
            return;
        }
        
        sendResponse(exchange, 200, Map.of(
                "success", true,
                "tutorial", tutorial
        ));
    }
    
    /**
     * GET /api/docs/faq - Get all FAQ items
     */
    private void handleGetFaq(HttpExchange exchange) throws IOException {
        List<DocumentationManager.FaqItem> faqItems = docManager.getAllFaqItems();
        
        // Group by tags
        Map<String, List<DocumentationManager.FaqItem>> byTag = new HashMap<>();
        Set<String> allTags = new HashSet<>();
        
        for (DocumentationManager.FaqItem item : faqItems) {
            for (String tag : item.tags) {
                allTags.add(tag);
                byTag.computeIfAbsent(tag, k -> new ArrayList<>()).add(item);
            }
        }
        
        sendResponse(exchange, 200, Map.of(
                "success", true,
                "count", faqItems.size(),
                "items", faqItems,
                "tags", allTags,
                "byTag", byTag
        ));
    }
    
    /**
     * GET /api/docs/faq/search?q=query - Search FAQ
     */
    private void handleSearchFaq(HttpExchange exchange) throws IOException {
        Map<String, String> params = parseQueryParams(exchange.getRequestURI().getQuery());
        String query = params.get("q");
        
        if (query == null || query.trim().isEmpty()) {
            sendResponse(exchange, 400, Map.of(
                    "success", false,
                    "error", "Missing query parameter 'q'"
            ));
            return;
        }
        
        List<DocumentationManager.FaqItem> results = docManager.searchFaq(query);
        
        sendResponse(exchange, 200, Map.of(
                "success", true,
                "query", query,
                "count", results.size(),
                "results", results
        ));
    }
    
    /**
     * GET /api/docs/videos - Get all video tutorials
     */
    private void handleGetVideos(HttpExchange exchange) throws IOException {
        List<DocumentationManager.VideoTutorial> videos = docManager.getAllVideoTutorials();
        
        sendResponse(exchange, 200, Map.of(
                "success", true,
                "count", videos.size(),
                "videos", videos
        ));
    }
    
    /**
     * GET /api/docs/videos/{id} - Get specific video tutorial
     */
    private void handleGetVideo(HttpExchange exchange, String path) throws IOException {
        String videoId = path.substring("/api/docs/videos/".length());
        DocumentationManager.VideoTutorial video = docManager.getVideoTutorial(videoId);
        
        if (video == null) {
            sendResponse(exchange, 404, Map.of(
                    "success", false,
                    "error", "Video tutorial not found: " + videoId
            ));
            return;
        }
        
        sendResponse(exchange, 200, Map.of(
                "success", true,
                "video", video
        ));
    }
    
    /**
     * GET /api/docs/search?q=query - Search all documentation
     */
    private void handleSearchAll(HttpExchange exchange) throws IOException {
        Map<String, String> params = parseQueryParams(exchange.getRequestURI().getQuery());
        String query = params.get("q");
        
        if (query == null || query.trim().isEmpty()) {
            sendResponse(exchange, 400, Map.of(
                    "success", false,
                    "error", "Missing query parameter 'q'"
            ));
            return;
        }
        
        String lowerQuery = query.toLowerCase();
        
        // Search sections
        List<DocumentationManager.DocumentationSection> sectionResults = docManager.getAllSections().values().stream()
                .filter(s -> s.title.toLowerCase().contains(lowerQuery) ||
                           s.description.toLowerCase().contains(lowerQuery) ||
                           s.content.toLowerCase().contains(lowerQuery))
                .toList();
        
        // Search API endpoints
        List<DocumentationManager.ApiEndpoint> apiResults = docManager.getAllApiEndpoints().values().stream()
                .filter(a -> a.name.toLowerCase().contains(lowerQuery) ||
                           a.description.toLowerCase().contains(lowerQuery) ||
                           a.endpoint.toLowerCase().contains(lowerQuery))
                .toList();
        
        // Search tutorials
        List<DocumentationManager.Tutorial> tutorialResults = docManager.getAllTutorials().stream()
                .filter(t -> t.title.toLowerCase().contains(lowerQuery) ||
                           t.description.toLowerCase().contains(lowerQuery))
                .toList();
        
        // Search FAQ
        List<DocumentationManager.FaqItem> faqResults = docManager.searchFaq(query);
        
        int totalResults = sectionResults.size() + apiResults.size() + 
                          tutorialResults.size() + faqResults.size();
        
        sendResponse(exchange, 200, Map.of(
                "success", true,
                "query", query,
                "totalResults", totalResults,
                "results", Map.of(
                        "sections", sectionResults,
                        "api", apiResults,
                        "tutorials", tutorialResults,
                        "faq", faqResults
                )
        ));
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
                    LOGGER.warn("Failed to decode parameter: {}", param, e);
                }
            }
        }
        
        return params;
    }
    
    /**
     * Send JSON response
     */
    private void sendResponse(HttpExchange exchange, int statusCode, Object data) throws IOException {
        String jsonResponse = gson.toJson(data);
        byte[] responseBytes = jsonResponse.getBytes(StandardCharsets.UTF_8);
        
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.sendResponseHeaders(statusCode, responseBytes.length);
        
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(responseBytes);
        }
    }
}
