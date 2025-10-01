
package com.zerog.neoessentials.config;
import com.zerog.neoessentials.util.DebugUtil;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.File;
import java.io.FileReader;
import java.io.InputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class CommandModuleConfig {
    public Map<String, Boolean> commandStates = new HashMap<>();
    public boolean enabled = true;

    public static CommandModuleConfig load(File configFile) {
        // If the file does not exist, copy the default from resources
        if (!configFile.exists()) {
            try {
                File parent = configFile.getParentFile();
                if (parent != null && !parent.exists()) parent.mkdirs();
                try (InputStream in = CommandModuleConfig.class.getClassLoader().getResourceAsStream("data/config.json")) {
                    if (in != null) {
                        try (FileOutputStream out = new FileOutputStream(configFile)) {
                            byte[] buf = new byte[4096];
                            int len;
                            while ((len = in.read(buf)) > 0) out.write(buf, 0, len);
                        }
                    }
                }
            } catch (IOException e) {
                DebugUtil.debugStackTrace(e);
            }
        }
        CommandModuleConfig config = new CommandModuleConfig();
        try (FileReader reader = new FileReader(configFile)) {
            JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
            if (root.has("modules") && root.getAsJsonObject("modules").has("commandsEnabled")) {
                config.enabled = root.getAsJsonObject("modules").get("commandsEnabled").getAsBoolean();
            }
            if (root.has("commands")) {
                JsonObject cmds = root.getAsJsonObject("commands");
                for (String key : cmds.keySet()) {
                    config.commandStates.put(key, cmds.get(key).getAsBoolean());
                }
            }
        } catch (Exception e) {
            // Log error and use defaults
            DebugUtil.debugStackTrace(e);
        }
        return config;
    }

    public boolean isCommandEnabled(String command) {
        return enabled && commandStates.getOrDefault(command, true);
    }
}
