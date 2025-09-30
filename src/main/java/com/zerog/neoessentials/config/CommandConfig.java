package com.zerog.neoessentials.config;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class CommandConfig {
    private static final String CONFIG_PATH = "config/neoessentials/config.json";
    private static Map<String, Boolean> commandStates = new HashMap<>();
    private static boolean loaded = false;

    public static boolean isCommandEnabled(String command) {
        if (!loaded) load();
        return commandStates.getOrDefault(command, true);
    }

    public static void load() {
        File file = new File(CONFIG_PATH);
        if (!file.exists()) {
            // Default: all enabled
            commandStates.clear();
            loaded = true;
            return;
        }
        try (FileReader reader = new FileReader(file)) {
            Gson gson = new Gson();
            JsonObject obj = gson.fromJson(reader, JsonObject.class);
            if (obj.has("commands")) {
                JsonObject cmds = obj.getAsJsonObject("commands");
                for (String key : cmds.keySet()) {
                    commandStates.put(key, cmds.get(key).getAsBoolean());
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        loaded = true;
    }
}
