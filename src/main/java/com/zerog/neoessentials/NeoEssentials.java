
package com.zerog.neoessentials;

import com.zerog.neoessentials.items.commands.dispose;

import com.zerog.neoessentials.api.NeoEssentialsAPI;
import com.zerog.neoessentials.economy.commands.EconomyCommands;
import com.zerog.neoessentials.economy.EconomyManager;
import com.zerog.neoessentials.permissions.PermissionManager;
import com.zerog.neoessentials.permissions.PermissionStorage;
import com.zerog.neoessentials.api.permissions.PermissionAPI;
import com.zerog.neoessentials.permissions.command.PermissionsCommand;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.io.File;
import java.io.InputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Mod("neoessentials")
public class NeoEssentials {
    public NeoEssentials() {
        ensureGlobalConfig();
        ensureEconomyConfig();
        ensurePermissionsConfig();
        ensureServerLangFile();
        // Initialize the core manager
        NeoEssentialsManager.getInstance();
        // Initialize permissions system
        try {
            PermissionManager permManager = new PermissionManager();
            PermissionStorage.load(permManager);
            PermissionAPI.setManager(permManager);
        } catch (Exception e) {
            e.printStackTrace();
        }
        // Register this mod class with the NeoForge event bus for non-static event handlers
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.register(this);
        // Example usage of the API (for demonstration)
        if (NeoEssentialsAPI.isAvailable()) {
            // API is available, ready for mod interoperability
        }
        // No manual event bus registration needed for NeoForge
    }

    private void ensureGlobalConfig() {
        File configFile = new File("config/neoessentials/config.json");
        if (!configFile.exists()) {
            try {
                File parent = configFile.getParentFile();
                if (parent != null && !parent.exists()) parent.mkdirs();
                try (InputStream in = NeoEssentials.class.getClassLoader().getResourceAsStream("data/config.json")) {
                    if (in != null) {
                        try (FileOutputStream out = new FileOutputStream(configFile)) {
                            byte[] buf = new byte[4096];
                            int len;
                            while ((len = in.read(buf)) > 0) out.write(buf, 0, len);
                        }
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void ensureEconomyConfig() {
        File configFile = new File("config/neoessentials/economy.json");
        if (!configFile.exists()) {
            try {
                File parent = configFile.getParentFile();
                if (parent != null && !parent.exists()) parent.mkdirs();
                try (InputStream in = NeoEssentials.class.getClassLoader().getResourceAsStream("data/economy.json")) {
                    if (in != null) {
                        try (FileOutputStream out = new FileOutputStream(configFile)) {
                            byte[] buf = new byte[4096];
                            int len;
                            while ((len = in.read(buf)) > 0) out.write(buf, 0, len);
                        }
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void ensurePermissionsConfig() {
        File configFile = new File("config/neoessentials/permissions.json");
        if (!configFile.exists()) {
            try {
                File parent = configFile.getParentFile();
                if (parent != null && !parent.exists()) parent.mkdirs();
                try (InputStream in = NeoEssentials.class.getClassLoader().getResourceAsStream("data/permissions.json")) {
                    if (in != null) {
                        try (FileOutputStream out = new FileOutputStream(configFile)) {
                            byte[] buf = new byte[4096];
                            int len;
                            while ((len = in.read(buf)) > 0) out.write(buf, 0, len);
                        }
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void ensureServerLangFile() {
        try {
            File serverLangDir = new File("neoessentials/lang");
            if (!serverLangDir.exists()) serverLangDir.mkdirs();
            File serverLangFile = new File(serverLangDir, "en_us.json");
            if (!serverLangFile.exists()) {
                // Try to copy from mod resources
                try (InputStream in = NeoEssentials.class.getClassLoader().getResourceAsStream("data/lang/en_us.json")) {
                    if (in != null) {
                        Files.copy(in, serverLangFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();
        EconomyCommands.register(dispatcher);
        PermissionsCommand.register(dispatcher);
    }

    @SubscribeEvent
    public void onServerStarted(ServerStartedEvent event) {
        MinecraftServer server = event.getServer();
        // Removed demonstration of LangUtil usage. Add your own messages here if needed.
        // Removed broken resource reload listener. Minecraft/NeoForge loads language files automatically if present in the JAR.
    }

    @SubscribeEvent
    public void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            UUID uuid = player.getUUID();
            EconomyManager.getInstance().loadPlayerEconomy(uuid);
        }
    }

    @SubscribeEvent
    public void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            UUID uuid = player.getUUID();
            EconomyManager.getInstance().savePlayerEconomy(uuid);
            // Auto-restore items if player disconnects with pending /dispose
            dispose.restorePendingItems(player);
        }
    }

    @SubscribeEvent
    public void onServerStopping(ServerStoppingEvent event) {
        EconomyManager.getInstance().saveAllPlayerEconomy();
    }
}