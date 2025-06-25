package com.zerog.neoessentials.debug;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.zerog.neoessentials.NeoEssentials;
import com.zerog.neoessentials.ui.tab.DataManagerHooks;
import com.zerog.neoessentials.ui.tab.TabManager;
import com.zerog.neoessentials.ui.tab.TemplateManager;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.bus.api.SubscribeEvent;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

/**
 * Emergency fix for tablist templates not loading correctly
 */
public class TablistFix {

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        NeoEssentials.LOGGER.info("Registering tabfix command");
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();
        
        dispatcher.register(
            Commands.literal("tabfix")
                .requires(source -> source.hasPermission(2)) // Operator level permission
                .executes(context -> {
                    CommandSourceStack source = context.getSource();
                    try {
                        if (!(source.getEntity() instanceof ServerPlayer)) {
                            source.sendFailure(Component.literal("This command must be run by a player"));
                            return 0;
                        }
                        
                        return executeTabFix(source);
                    } catch (Exception e) {
                        source.sendFailure(Component.literal("Error: " + e.getMessage()));
                        NeoEssentials.LOGGER.error("Error executing tabfix command", e);
                        return 0;
                    }
                })
        );
    }
    
    private static int executeTabFix(CommandSourceStack source) {
        try {
            source.sendSystemMessage(Component.literal("§e===== §6NeoEssentials Tablist Fix §e====="));
            source.sendSystemMessage(Component.literal("§7Running tablist template fix..."));
            
            // Get TabManager from DataManagerHooks
            TabManager tabManager = DataManagerHooks.getTabManager();
            if (tabManager == null) {
                source.sendSystemMessage(Component.literal("§cError: TabManager is not initialized"));
                return 0;
            }
            
            // Step 1: Check for templates.json in both locations
            Path neoEssentialsDir = Paths.get("neoessentials");
            Path configDir = Paths.get("config", "neoessentials");
            
            Path neoTemplatesJsonFile = neoEssentialsDir.resolve("templates.json");
            Path configTemplatesFile = configDir.resolve("templates.json");
            
            boolean neoTemplatesExists = Files.exists(neoTemplatesJsonFile);
            boolean configTemplatesExists = Files.exists(configTemplatesFile);
            
            source.sendSystemMessage(Component.literal("§7Checking template files:"));
            source.sendSystemMessage(Component.literal("§7- templates.json in neoessentials dir: " + 
                (neoTemplatesExists ? "§aExists" : "§cMissing")));
            source.sendSystemMessage(Component.literal("§7- templates.json in config/neoessentials dir: " + 
                (configTemplatesExists ? "§aExists" : "§cMissing")));
            
            // Step 2: Create needed directories
            if (!Files.exists(neoEssentialsDir)) {
                Files.createDirectories(neoEssentialsDir);
                source.sendSystemMessage(Component.literal("§7Created neoessentials directory"));
            }
            
            // Step 3: Copy/create templates file
            if (!neoTemplatesExists) {
                if (configTemplatesExists) {
                    // Copy from config dir to neoessentials dir
                    Files.copy(configTemplatesFile, neoTemplatesJsonFile, StandardCopyOption.REPLACE_EXISTING);
                    source.sendSystemMessage(Component.literal("§aCopied templates.json from config dir to neoessentials dir"));
                } else {                    // Extract from resources
                    TemplateManager templateManager = tabManager.getTemplateManager();
                    boolean created = templateManager.createDefaultTemplatesFile();
                    if (created) {
                        source.sendSystemMessage(Component.literal("§aCreated default templates.json in neoessentials dir"));
                    } else {
                        source.sendSystemMessage(Component.literal("§cFailed to create default templates.json file"));
                    }
                }
            } else {
                source.sendSystemMessage(Component.literal("§7Using existing templates.json in neoessentials dir"));
            }
            
            // Step 4: Force reload templates
            TemplateManager templateManager = tabManager.getTemplateManager();
            templateManager.loadTemplates();
            
            // Step 5: Update TabManager config
            tabManager.loadConfig();
            
            // Step 6: Check if templates loaded
            int headerCount = templateManager.getGlobalHeaders().size();
            int footerCount = templateManager.getGlobalFooters().size();
            
            source.sendSystemMessage(Component.literal("§7After reload:"));
            source.sendSystemMessage(Component.literal("§7- Headers loaded: §f" + headerCount));
            source.sendSystemMessage(Component.literal("§7- Footers loaded: §f" + footerCount));
            
            if (headerCount > 0 || footerCount > 0) {
                source.sendSystemMessage(Component.literal("§a✓ Fix applied successfully! Templates loaded."));
                source.sendSystemMessage(Component.literal("§a✓ Tablist should now display your custom templates."));
                source.sendSystemMessage(Component.literal("§7If issues persist, check console for errors."));
            } else {
                source.sendSystemMessage(Component.literal("§cWarning: No templates loaded after fix."));
                source.sendSystemMessage(Component.literal("§7Check the format of your templates.json file."));
            }
            
            return 1;
        } catch (Exception e) {
            source.sendSystemMessage(Component.literal("§cError during tablist fix: " + e.getMessage()));
            NeoEssentials.LOGGER.error("Tablist fix error", e);
            return 0;
        }
    }
}
