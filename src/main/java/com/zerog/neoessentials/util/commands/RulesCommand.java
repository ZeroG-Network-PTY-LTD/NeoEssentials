package com.zerog.neoessentials.util.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import com.zerog.neoessentials.config.ConfigManager;
import com.zerog.neoessentials.util.MessageUtil;
import com.zerog.neoessentials.util.PermissionValidator;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.util.ArrayList;
import java.util.List;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Implements the /rules command - Displays server rules to players
 * Supports pagination and configurable rules
 */
public class RulesCommand {
    private static List<String> serverRules = new ArrayList<>();
    private static final Path RULES_DATA_FILE = Paths.get("config", "neoessentials", "rules_data.json");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final int RULES_PER_PAGE = 10;
    
    /**
     * Register the /rules command
     */
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        if (!ConfigManager.getInstance().isCommandEnabled("rules")) return;
        
        // Load rules data on registration
        loadRulesData();
        
        dispatcher.register(
            Commands.literal("rules")
                // /rules - Show rules (page 1)
                .executes(ctx -> {
                    PermissionValidator.PermissionResult permResult = 
                        PermissionValidator.validatePermission(ctx.getSource(), "neoessentials.rules");
                    if (!permResult.hasPermission()) {
                        ctx.getSource().sendFailure(MessageUtil.error(permResult.getErrorMessage()));
                        return 0;
                    }
                    
                    return showRules(ctx.getSource(), 1);
                })
                // /rules <page> - Show specific page
                .then(Commands.argument("page", IntegerArgumentType.integer(1))
                    .executes(ctx -> {
                        PermissionValidator.PermissionResult permResult = 
                            PermissionValidator.validatePermission(ctx.getSource(), "neoessentials.rules");
                        if (!permResult.hasPermission()) {
                            ctx.getSource().sendFailure(MessageUtil.error(permResult.getErrorMessage()));
                            return 0;
                        }
                        
                        int page = IntegerArgumentType.getInteger(ctx, "page");
                        return showRules(ctx.getSource(), page);
                    })
                )
        );
    }
    
    /**
     * Show server rules with pagination
     */
    private static int showRules(CommandSourceStack source, int page) {
        if (serverRules.isEmpty()) {
            source.sendSuccess(() -> MessageUtil.info("commands.neoessentials.rules.no_rules"), false);
            return 1;
        }
        
        int totalPages = (int) Math.ceil((double) serverRules.size() / RULES_PER_PAGE);
        
        if (page > totalPages) {
            source.sendFailure(MessageUtil.error("commands.neoessentials.rules.invalid_page", page, totalPages));
            return 0;
        }
        
        int startIndex = (page - 1) * RULES_PER_PAGE;
        int endIndex = Math.min(startIndex + RULES_PER_PAGE, serverRules.size());
        
        // Header
        source.sendSuccess(() -> MessageUtil.success("commands.neoessentials.rules.header", page, totalPages), false);
        
        // Rules
        for (int i = startIndex; i < endIndex; i++) {
            String rule = serverRules.get(i);
            String formattedRule = rule.replace("&", "§");
            
            MutableComponent ruleComponent = Component.literal(String.format("§6%d. §f%s", i + 1, formattedRule));
            source.sendSuccess(() -> ruleComponent, false);
        }
        
        // Footer with navigation
        if (totalPages > 1) {
            MutableComponent footer = Component.literal("§7Page " + page + "/" + totalPages + " ");
            
            if (page > 1) {
                footer.append(Component.literal("§7[§a◀ Prev§7]")
                    .withStyle(style -> style
                        .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/rules " + (page - 1)))
                        .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal("§7Click to view previous page")))
                    ));
                footer.append(Component.literal(" "));
            }
            
            if (page < totalPages) {
                footer.append(Component.literal("§7[§aNext ▶§7]")
                    .withStyle(style -> style
                        .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/rules " + (page + 1)))
                        .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal("§7Click to view next page")))
                    ));
            }
            
            source.sendSuccess(() -> footer, false);
        }
        
        // Server info footer
        source.sendSuccess(() -> MessageUtil.info("commands.neoessentials.rules.footer"), false);
        
        return 1;
    }
    
    /**
     * Load rules data from file
     */
    private static void loadRulesData() {
        try {
            if (!Files.exists(RULES_DATA_FILE)) {
                // Create default rules file
                createDefaultRules();
                return;
            }
            
            String json = Files.readString(RULES_DATA_FILE);
            JsonObject data = JsonParser.parseString(json).getAsJsonObject();
            
            serverRules.clear();
            
            if (data.has("rules")) {
                JsonArray rulesArray = data.getAsJsonArray("rules");
                for (JsonElement element : rulesArray) {
                    serverRules.add(element.getAsString());
                }
            }
            
        } catch (Exception e) {
            System.err.println("Failed to load rules data: " + e.getMessage());
            createDefaultRules();
        }
    }
    
    /**
     * Create default rules file
     */
    private static void createDefaultRules() {
        serverRules.clear();
        serverRules.add("&6Be respectful to all players and staff members");
        serverRules.add("&cNo griefing, stealing, or destroying other players' builds");
        serverRules.add("&eNo spamming in chat or using excessive caps");
        serverRules.add("&bNo cheating, hacking, or using unauthorized mods");
        serverRules.add("&5Keep builds appropriate and family-friendly");
        serverRules.add("&aFollow staff instructions and respect their decisions");
        serverRules.add("&9No advertising other servers or external content");
        serverRules.add("&dUse common sense and help maintain a fun environment");
        serverRules.add("&7Report any issues or rule violations to staff");
        serverRules.add("&2Have fun and enjoy your time on the server!");
        
        saveRulesData();
    }
    
    /**
     * Save rules data to file
     */
    private static void saveRulesData() {
        try {
            JsonObject data = new JsonObject();
            JsonArray rulesArray = new JsonArray();
            
            for (String rule : serverRules) {
                rulesArray.add(rule);
            }
            
            data.add("rules", rulesArray);
            
            Files.createDirectories(RULES_DATA_FILE.getParent());
            Files.writeString(RULES_DATA_FILE, GSON.toJson(data));
            
        } catch (Exception e) {
            System.err.println("Failed to save rules data: " + e.getMessage());
        }
    }
    
    /**
     * Get all server rules
     */
    public static List<String> getRules() {
        return new ArrayList<>(serverRules);
    }
    
    /**
     * Show rules to a player (for join messages or other events)
     */
    public static void showRulesToPlayer(ServerPlayer player) {
        showRules(player.createCommandSourceStack(), 1);
    }
}