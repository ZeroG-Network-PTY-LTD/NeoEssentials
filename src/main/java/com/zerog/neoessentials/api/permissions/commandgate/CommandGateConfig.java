package com.zerog.neoessentials.api.permissions.commandgate;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.zerog.neoessentials.logging.LogCategory;
import com.zerog.neoessentials.logging.NeoLog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Parsed snapshot of {@code command_permissions.json}'s {@code mappings} array — a list of
 * (command path -> permission node) entries used to gate commands registered by other mods.
 * Immutable once built; {@link ThirdPartyCommandGate} swaps in a fresh instance on reload.
 */
public class CommandGateConfig {
    private static final Logger LOGGER = LoggerFactory.getLogger(CommandGateConfig.class);

    public record Mapping(List<String> pathSegments, String permissionNode) {}

    private final List<Mapping> mappings;

    private CommandGateConfig(List<Mapping> mappings) {
        this.mappings = mappings;
    }

    public static CommandGateConfig empty() {
        return new CommandGateConfig(List.of());
    }

    public static CommandGateConfig parse(JsonObject config) {
        List<Mapping> parsed = new ArrayList<>();
        if (config != null && config.has("mappings") && config.get("mappings").isJsonArray()) {
            JsonArray array = config.getAsJsonArray("mappings");
            for (int i = 0; i < array.size(); i++) {
                try {
                    JsonObject entry = array.get(i).getAsJsonObject();
                    String path = entry.get("path").getAsString().trim();
                    String permission = entry.get("permission").getAsString().trim();
                    if (path.isEmpty() || permission.isEmpty()) {
                        NeoLog.warn(LOGGER, LogCategory.PERMISSIONS,
                            "Skipping command_permissions.json mapping #{}: empty path or permission", i);
                        continue;
                    }
                    List<String> segments = Arrays.asList(path.split("\\s+"));
                    parsed.add(new Mapping(segments, permission));
                } catch (Exception e) {
                    NeoLog.warn(LOGGER, LogCategory.PERMISSIONS,
                        "Skipping malformed command_permissions.json mapping #{}: {}", i, e.getMessage());
                }
            }
        }
        return new CommandGateConfig(parsed);
    }

    public List<Mapping> getMappings() {
        return mappings;
    }

    /**
     * Deepest-match-wins lookup: among every mapping whose path is a prefix of (or equal to)
     * {@code actualPath}, returns the permission node of whichever mapping has the longest
     * matching path. Returns {@code null} if nothing matches.
     */
    public String findPermissionNodeFor(List<String> actualPath) {
        Mapping best = null;
        for (Mapping mapping : mappings) {
            if (isPrefix(mapping.pathSegments(), actualPath) &&
                (best == null || mapping.pathSegments().size() > best.pathSegments().size())) {
                best = mapping;
            }
        }
        return best != null ? best.permissionNode() : null;
    }

    private static boolean isPrefix(List<String> prefix, List<String> full) {
        if (prefix.size() > full.size()) {
            return false;
        }
        for (int i = 0; i < prefix.size(); i++) {
            if (!prefix.get(i).equals(full.get(i))) {
                return false;
            }
        }
        return true;
    }
}
