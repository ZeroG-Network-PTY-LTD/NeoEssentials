package com.zerog.neoessentials.util;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.File;
import java.nio.file.Files;

public class DebugUtil {
    private static boolean debugEnabled = false;
    private static boolean loaded = false;

    public static boolean isDebugEnabled() {
        if (!loaded) reload();
        return debugEnabled;
    }

    public static void debug(String msg) {
        if (isDebugEnabled()) {
            System.out.println("[NeoEssentials:DEBUG] " + msg);
        }
    }

    public static void debugErr(String msg) {
        if (isDebugEnabled()) {
            System.err.println("[NeoEssentials:DEBUG] " + msg);
        }
    }

    public static void debugStackTrace(Throwable t) {
        if (isDebugEnabled()) {
            t.printStackTrace();
        }
    }

    public static void reload() {
        try {
            File configFile = new File("config/neoessentials/config.json");
            if (configFile.exists()) {
                String json = new String(Files.readAllBytes(configFile.toPath()));
                JsonObject obj = JsonParser.parseString(json).getAsJsonObject();
                if (obj.has("debug")) {
                    JsonObject debugObj = obj.getAsJsonObject("debug");
                    if (debugObj.has("debugEnabled")) {
                        debugEnabled = debugObj.get("debugEnabled").getAsBoolean();
                    }
                }
            }
        } catch (Exception e) {
            // If debug can't be loaded, default to false
            debugEnabled = false;
        }
        loaded = true;
    }
}
