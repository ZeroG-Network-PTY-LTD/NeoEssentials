package com.zerog.neoessentials.webdashboard.map;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Waypoint management system for map viewer
 * Allows creation, editing, deletion of waypoints with colors, icons, and descriptions
 */
public class WaypointManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(WaypointManager.class);
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static WaypointManager INSTANCE;
    
    private static final Path WAYPOINTS_DIR = Paths.get("neoessentials", "map", "waypoints");
    private static final Path WAYPOINTS_FILE = WAYPOINTS_DIR.resolve("waypoints.json");
    
    // Waypoints storage: ID -> Waypoint
    private final Map<String, Waypoint> waypoints = new ConcurrentHashMap<>();
    
    private WaypointManager() {
        try {
            if (!Files.exists(WAYPOINTS_DIR)) {
                Files.createDirectories(WAYPOINTS_DIR);
            }
            loadWaypoints();
        } catch (IOException e) {
            LOGGER.error("Failed to initialize waypoints directory", e);
        }
    }
    
    public static WaypointManager getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new WaypointManager();
        }
        return INSTANCE;
    }
    
    /**
     * Create a new waypoint
     */
    public Waypoint createWaypoint(String name, String dimension, double x, double y, double z, 
                                   String color, String icon, String description, String createdBy) {
        Waypoint waypoint = new Waypoint();
        waypoint.id = UUID.randomUUID().toString();
        waypoint.name = name;
        waypoint.dimension = dimension;
        waypoint.x = x;
        waypoint.y = y;
        waypoint.z = z;
        waypoint.color = color != null ? color : "#FF5733";
        waypoint.icon = icon != null ? icon : "marker";
        waypoint.description = description != null ? description : "";
        waypoint.createdBy = createdBy;
        waypoint.createdAt = System.currentTimeMillis();
        waypoint.visible = true;
        
        waypoints.put(waypoint.id, waypoint);
        saveWaypoints();
        
        LOGGER.info("Created waypoint: {} at ({}, {}, {}) in {}", name, x, y, z, dimension);
        return waypoint;
    }
    
    /**
     * Update existing waypoint
     */
    public boolean updateWaypoint(String id, String name, String dimension, Double x, Double y, Double z,
                                  String color, String icon, String description, Boolean visible) {
        Waypoint waypoint = waypoints.get(id);
        if (waypoint == null) {
            return false;
        }
        
        if (name != null) waypoint.name = name;
        if (dimension != null) waypoint.dimension = dimension;
        if (x != null) waypoint.x = x;
        if (y != null) waypoint.y = y;
        if (z != null) waypoint.z = z;
        if (color != null) waypoint.color = color;
        if (icon != null) waypoint.icon = icon;
        if (description != null) waypoint.description = description;
        if (visible != null) waypoint.visible = visible;
        
        waypoint.updatedAt = System.currentTimeMillis();
        
        saveWaypoints();
        LOGGER.info("Updated waypoint: {} ({})", waypoint.name, id);
        return true;
    }
    
    /**
     * Delete waypoint
     */
    public boolean deleteWaypoint(String id) {
        Waypoint removed = waypoints.remove(id);
        if (removed != null) {
            saveWaypoints();
            LOGGER.info("Deleted waypoint: {} ({})", removed.name, id);
            return true;
        }
        return false;
    }
    
    /**
     * Get waypoint by ID
     */
    public Waypoint getWaypoint(String id) {
        return waypoints.get(id);
    }
    
    /**
     * Get all waypoints
     */
    public Collection<Waypoint> getAllWaypoints() {
        return new ArrayList<>(waypoints.values());
    }
    
    /**
     * Get waypoints for specific dimension
     */
    public List<Waypoint> getWaypointsInDimension(String dimension) {
        List<Waypoint> result = new ArrayList<>();
        for (Waypoint wp : waypoints.values()) {
            if (wp.dimension.equals(dimension) && wp.visible) {
                result.add(wp);
            }
        }
        return result;
    }
    
    /**
     * Get visible waypoints only
     */
    public List<Waypoint> getVisibleWaypoints() {
        List<Waypoint> result = new ArrayList<>();
        for (Waypoint wp : waypoints.values()) {
            if (wp.visible) {
                result.add(wp);
            }
        }
        return result;
    }
    
    /**
     * Convert waypoints to JSON
     */
    public JsonObject getWaypointsJson() {
        JsonObject response = new JsonObject();
        response.addProperty("timestamp", System.currentTimeMillis());
        response.addProperty("waypointCount", waypoints.size());
        
        JsonArray waypointsArray = new JsonArray();
        for (Waypoint wp : waypoints.values()) {
            waypointsArray.add(waypointToJson(wp));
        }
        
        response.add("waypoints", waypointsArray);
        return response;
    }
    
    /**
     * Get waypoints JSON filtered by dimension
     */
    public JsonObject getWaypointsJson(String dimension) {
        JsonObject response = new JsonObject();
        response.addProperty("timestamp", System.currentTimeMillis());
        response.addProperty("dimension", dimension);
        
        List<Waypoint> filtered = getWaypointsInDimension(dimension);
        response.addProperty("waypointCount", filtered.size());
        
        JsonArray waypointsArray = new JsonArray();
        for (Waypoint wp : filtered) {
            waypointsArray.add(waypointToJson(wp));
        }
        
        response.add("waypoints", waypointsArray);
        return response;
    }
    
    /**
     * Convert single waypoint to JSON
     */
    private JsonObject waypointToJson(Waypoint wp) {
        JsonObject obj = new JsonObject();
        obj.addProperty("id", wp.id);
        obj.addProperty("name", wp.name);
        obj.addProperty("dimension", wp.dimension);
        obj.addProperty("x", wp.x);
        obj.addProperty("y", wp.y);
        obj.addProperty("z", wp.z);
        obj.addProperty("color", wp.color);
        obj.addProperty("icon", wp.icon);
        obj.addProperty("description", wp.description);
        obj.addProperty("visible", wp.visible);
        obj.addProperty("createdBy", wp.createdBy);
        obj.addProperty("createdAt", wp.createdAt);
        if (wp.updatedAt > 0) {
            obj.addProperty("updatedAt", wp.updatedAt);
        }
        return obj;
    }
    
    /**
     * Load waypoints from disk
     */
    private void loadWaypoints() {
        try {
            if (Files.exists(WAYPOINTS_FILE)) {
                String json = Files.readString(WAYPOINTS_FILE, StandardCharsets.UTF_8);
                JsonObject data = JsonParser.parseString(json).getAsJsonObject();
                
                if (data.has("waypoints")) {
                    JsonArray waypointsArray = data.getAsJsonArray("waypoints");
                    for (int i = 0; i < waypointsArray.size(); i++) {
                        JsonObject wpObj = waypointsArray.get(i).getAsJsonObject();
                        Waypoint wp = new Waypoint();
                        wp.id = wpObj.get("id").getAsString();
                        wp.name = wpObj.get("name").getAsString();
                        wp.dimension = wpObj.get("dimension").getAsString();
                        wp.x = wpObj.get("x").getAsDouble();
                        wp.y = wpObj.get("y").getAsDouble();
                        wp.z = wpObj.get("z").getAsDouble();
                        wp.color = wpObj.has("color") ? wpObj.get("color").getAsString() : "#FF5733";
                        wp.icon = wpObj.has("icon") ? wpObj.get("icon").getAsString() : "marker";
                        wp.description = wpObj.has("description") ? wpObj.get("description").getAsString() : "";
                        wp.visible = wpObj.has("visible") ? wpObj.get("visible").getAsBoolean() : true;
                        wp.createdBy = wpObj.has("createdBy") ? wpObj.get("createdBy").getAsString() : "unknown";
                        wp.createdAt = wpObj.has("createdAt") ? wpObj.get("createdAt").getAsLong() : System.currentTimeMillis();
                        wp.updatedAt = wpObj.has("updatedAt") ? wpObj.get("updatedAt").getAsLong() : 0;
                        
                        waypoints.put(wp.id, wp);
                    }
                }
                
                LOGGER.info("Loaded {} waypoints from disk", waypoints.size());
            }
        } catch (Exception e) {
            LOGGER.error("Failed to load waypoints", e);
        }
    }
    
    /**
     * Save waypoints to disk
     */
    private void saveWaypoints() {
        try {
            JsonObject data = new JsonObject();
            data.addProperty("lastUpdated", System.currentTimeMillis());
            data.addProperty("version", "1.0");
            
            JsonArray waypointsArray = new JsonArray();
            for (Waypoint wp : waypoints.values()) {
                waypointsArray.add(waypointToJson(wp));
            }
            data.add("waypoints", waypointsArray);
            
            Files.writeString(WAYPOINTS_FILE, GSON.toJson(data), StandardCharsets.UTF_8,
                StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
                
        } catch (Exception e) {
            LOGGER.error("Failed to save waypoints", e);
        }
    }
    
    /**
     * Waypoint data structure
     */
    public static class Waypoint {
        public String id;
        public String name;
        public String dimension;
        public double x;
        public double y;
        public double z;
        public String color;
        public String icon;
        public String description;
        public boolean visible;
        public String createdBy;
        public long createdAt;
        public long updatedAt;
    }
}
