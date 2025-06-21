package com.zerog.neoessentials.data;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonIOException;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;
import com.zerog.neoessentials.NeoEssentials;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.io.*;
import java.lang.reflect.Type;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages powertool bindings for players.
 */
public class PowerToolManager {
    // Map of player UUID to their powertools (item registry name -> command)
    private final Map<UUID, Map<String, String>> playerPowerTools = new ConcurrentHashMap<>();
    // Map of player UUID to toggle state
    private final Map<UUID, Boolean> powerToolToggleState = new ConcurrentHashMap<>();
    private final File powerToolsFile;
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    /**
     * Creates a new PowerToolManager.
     *
     * @param dataFolder The folder to store powertool data in
     */
    public PowerToolManager(File dataFolder) {
        this.powerToolsFile = new File(dataFolder, "powertools.json");
        loadPowerTools();
    }

    /**
     * Loads the powertools from the powertools.json file.
     */
    private void loadPowerTools() {
        try {
            if (!powerToolsFile.exists()) {
                savePowerTools();
                return;
            }

            try (Reader reader = new FileReader(powerToolsFile)) {
                Type type = new TypeToken<Map<UUID, PowerToolData>>() {}.getType();
                Map<UUID, PowerToolData> loadedData = GSON.fromJson(reader, type);

                if (loadedData != null) {
                    playerPowerTools.clear();
                    powerToolToggleState.clear();

                    loadedData.forEach((uuid, data) -> {
                        playerPowerTools.put(uuid, data.getCommands());
                        powerToolToggleState.put(uuid, data.isEnabled());
                    });

                    NeoEssentials.LOGGER.info("Loaded powertool data for {} players", playerPowerTools.size());
                }
            }
        } catch (JsonIOException | JsonSyntaxException | IOException e) {
            NeoEssentials.LOGGER.error("Failed to load powertools", e);
        }
    }

    /**
     * Saves the powertools to the powertools.json file.
     */
    public void savePowerTools() {
        try {
            if (!powerToolsFile.getParentFile().exists() && !powerToolsFile.getParentFile().mkdirs()) {
                NeoEssentials.LOGGER.error("Failed to create powertools directory");
                return;
            }

            Map<UUID, PowerToolData> dataToSave = new HashMap<>();
            playerPowerTools.forEach((uuid, commands) -> {
                boolean enabled = powerToolToggleState.getOrDefault(uuid, true);
                dataToSave.put(uuid, new PowerToolData(commands, enabled));
            });

            try (Writer writer = new FileWriter(powerToolsFile)) {
                GSON.toJson(dataToSave, writer);
            }
        } catch (JsonIOException | IOException e) {
            NeoEssentials.LOGGER.error("Failed to save powertools", e);
        }
    }

    /**
     * Sets a powertool binding for a player.
     *
     * @param player The player to set the binding for
     * @param item The item to bind the command to
     * @param command The command to bind
     */
    public void setPowerTool(ServerPlayer player, Item item, String command) {
        UUID uuid = player.getUUID();
        String itemKey = item.toString();

        Map<String, String> playerTools = playerPowerTools.computeIfAbsent(uuid, k -> new HashMap<>());
        playerTools.put(itemKey, command);

        savePowerTools();
    }

    /**
     * Clears a powertool binding for a player.
     *
     * @param player The player to clear the binding for
     * @param item The item to clear the binding from
     * @return True if a binding was cleared, false otherwise
     */
    public boolean clearPowerTool(ServerPlayer player, Item item) {
        UUID uuid = player.getUUID();
        String itemKey = item.toString();

        Map<String, String> playerTools = playerPowerTools.get(uuid);
        if (playerTools != null) {
            boolean removed = playerTools.remove(itemKey) != null;
            
            // Remove the entry for the player if they have no more powertools
            if (playerTools.isEmpty()) {
                playerPowerTools.remove(uuid);
                powerToolToggleState.remove(uuid);
            }
            
            if (removed) {
                savePowerTools();
            }
            return removed;
        }

        return false;
    }

    /**
     * Clears all powertool bindings for a player.
     *
     * @param player The player to clear all bindings for
     * @return The number of bindings cleared
     */
    public int clearAllPowerTools(ServerPlayer player) {
        UUID uuid = player.getUUID();

        Map<String, String> playerTools = playerPowerTools.get(uuid);
        if (playerTools != null) {
            int count = playerTools.size();
            playerPowerTools.remove(uuid);
            powerToolToggleState.remove(uuid);
            
            if (count > 0) {
                savePowerTools();
            }
            return count;
        }

        return 0;
    }

    /**
     * Gets the command bound to an item for a player.
     *
     * @param player The player to get the binding for
     * @param item The item to get the binding for
     * @return The bound command, or null if no binding exists
     */
    public String getPowerToolCommand(ServerPlayer player, Item item) {
        UUID uuid = player.getUUID();
        String itemKey = item.toString();

        Map<String, String> playerTools = playerPowerTools.get(uuid);
        if (playerTools != null) {
            return playerTools.get(itemKey);
        }

        return null;
    }

    /**
     * Gets all powertool bindings for a player.
     *
     * @param player The player to get the bindings for
     * @return A map of item registry name to command
     */
    public Map<String, String> getPlayerPowerTools(ServerPlayer player) {
        UUID uuid = player.getUUID();
        return playerPowerTools.getOrDefault(uuid, Collections.emptyMap());
    }

    /**
     * Gets whether powertools are enabled for a player.
     *
     * @param player The player to check
     * @return True if powertools are enabled, false otherwise
     */
    public boolean isPowerToolEnabled(ServerPlayer player) {
        UUID uuid = player.getUUID();
        return powerToolToggleState.getOrDefault(uuid, true);
    }

    /**
     * Sets whether powertools are enabled for a player.
     *
     * @param player The player to set the state for
     * @param enabled Whether powertools should be enabled
     */
    public void setPowerToolEnabled(ServerPlayer player, boolean enabled) {
        UUID uuid = player.getUUID();
        powerToolToggleState.put(uuid, enabled);
        savePowerTools();
    }

    /**
     * Toggles whether powertools are enabled for a player.
     *
     * @param player The player to toggle the state for
     * @return The new state (true if enabled, false if disabled)
     */
    public boolean togglePowerTool(ServerPlayer player) {
        UUID uuid = player.getUUID();
        boolean newState = !powerToolToggleState.getOrDefault(uuid, true);
        powerToolToggleState.put(uuid, newState);
        savePowerTools();
        return newState;
    }

    /**
     * Gets whether an item is a powertool for a player.
     *
     * @param player The player to check
     * @param item The item to check
     * @return True if the item is a powertool, false otherwise
     */
    public boolean isPowerTool(ServerPlayer player, Item item) {
        UUID uuid = player.getUUID();
        String itemKey = item.toString();

        Map<String, String> playerTools = playerPowerTools.get(uuid);
        return playerTools != null && playerTools.containsKey(itemKey);
    }

    /**
     * Checks if a player has any powertools.
     *
     * @param player The player to check
     * @return True if the player has any powertools, false otherwise
     */
    public boolean hasAnyPowerTools(ServerPlayer player) {
        UUID uuid = player.getUUID();
        Map<String, String> playerTools = playerPowerTools.get(uuid);
        return playerTools != null && !playerTools.isEmpty();
    }

    /**
     * Class to store powertool data for serialization/deserialization.
     */
    private static class PowerToolData {
        private final Map<String, String> commands;
        private final boolean enabled;

        public PowerToolData(Map<String, String> commands, boolean enabled) {
            this.commands = commands;
            this.enabled = enabled;
        }

        public Map<String, String> getCommands() {
            return commands;
        }

        public boolean isEnabled() {
            return enabled;
        }
    }
}
