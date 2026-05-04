package com.zerog.neoessentials.util.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
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
import com.zerog.neoessentials.util.ResourceUtil;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.ArrayList;
import java.util.List;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Implements the /rules command - Displays server rules to players
 * Supports pagination and configurable rules
 */
public class RulesCommand {
    private static final Logger LOGGER = LoggerFactory.getLogger(RulesCommand.class);
    private static List<String> serverRules = new ArrayList<>();
    private static final Path RULES_DATA_FILE = ResourceUtil.getConfigPath("rules_data.json");
    // Legacy file name used in older NeoEssentials builds
    private static final Path RULES_LEGACY_FILE = ResourceUtil.getConfigPath("rules.json");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
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
                // /rules add <rule> - Add a new rule (admin)
                .then(Commands.literal("add")
                    .then(Commands.argument("rule", StringArgumentType.greedyString())
                        .executes(ctx -> {
                            PermissionValidator.PermissionResult permResult = 
                                PermissionValidator.validatePermission(ctx.getSource(), "neoessentials.rules.admin");
                            if (!permResult.hasPermission()) {
                                ctx.getSource().sendFailure(MessageUtil.error(permResult.getErrorMessage()));
                                return 0;
                            }
                            
                            String rule = StringArgumentType.getString(ctx, "rule");
                            return addRule(ctx.getSource(), rule);
                        })
                    )
                )
                // /rules remove <number> - Remove a rule (admin)
                .then(Commands.literal("remove")
                    .then(Commands.argument("number", IntegerArgumentType.integer(1))
                        .executes(ctx -> {
                            PermissionValidator.PermissionResult permResult = 
                                PermissionValidator.validatePermission(ctx.getSource(), "neoessentials.rules.admin");
                            if (!permResult.hasPermission()) {
                                ctx.getSource().sendFailure(MessageUtil.error(permResult.getErrorMessage()));
                                return 0;
                            }
                            
                            int number = IntegerArgumentType.getInteger(ctx, "number");
                            return removeRule(ctx.getSource(), number);
                        })
                    )
                )
                // /rules edit <number> <new text> - Edit a rule (admin)
                .then(Commands.literal("edit")
                    .then(Commands.argument("number", IntegerArgumentType.integer(1))
                        .then(Commands.argument("newText", StringArgumentType.greedyString())
                            .executes(ctx -> {
                                PermissionValidator.PermissionResult permResult = 
                                    PermissionValidator.validatePermission(ctx.getSource(), "neoessentials.rules.admin");
                                if (!permResult.hasPermission()) {
                                    ctx.getSource().sendFailure(MessageUtil.error(permResult.getErrorMessage()));
                                    return 0;
                                }
                                
                                int number = IntegerArgumentType.getInteger(ctx, "number");
                                String newText = StringArgumentType.getString(ctx, "newText");
                                return editRule(ctx.getSource(), number, newText);
                            })
                        )
                    )
                )
                // /rules insert <number> <rule> - Insert a rule at position (admin)
                .then(Commands.literal("insert")
                    .then(Commands.argument("number", IntegerArgumentType.integer(1))
                        .then(Commands.argument("rule", StringArgumentType.greedyString())
                            .executes(ctx -> {
                                PermissionValidator.PermissionResult permResult = 
                                    PermissionValidator.validatePermission(ctx.getSource(), "neoessentials.rules.admin");
                                if (!permResult.hasPermission()) {
                                    ctx.getSource().sendFailure(MessageUtil.error(permResult.getErrorMessage()));
                                    return 0;
                                }
                                
                                int number = IntegerArgumentType.getInteger(ctx, "number");
                                String rule = StringArgumentType.getString(ctx, "rule");
                                return insertRule(ctx.getSource(), number, rule);
                            })
                        )
                    )
                )
                // /rules clear - Clear all rules (admin)
                .then(Commands.literal("clear")
                    .executes(ctx -> {
                        PermissionValidator.PermissionResult permResult = 
                            PermissionValidator.validatePermission(ctx.getSource(), "neoessentials.rules.admin");
                        if (!permResult.hasPermission()) {
                            ctx.getSource().sendFailure(MessageUtil.error(permResult.getErrorMessage()));
                            return 0;
                        }
                        
                        return clearRules(ctx.getSource());
                    })
                )
                // /rules reload - Reload rules from file (admin)
                .then(Commands.literal("reload")
                    .executes(ctx -> {
                        PermissionValidator.PermissionResult permResult = 
                            PermissionValidator.validatePermission(ctx.getSource(), "neoessentials.rules.admin");
                        if (!permResult.hasPermission()) {
                            ctx.getSource().sendFailure(MessageUtil.error(permResult.getErrorMessage()));
                            return 0;
                        }
                        
                        return reloadRules(ctx.getSource());
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
     * Add a new rule
     */
    private static int addRule(CommandSourceStack source, String rule) {
        if (rule.length() > 200) {
            source.sendFailure(MessageUtil.error("commands.neoessentials.rules.too_long"));
            return 0;
        }
        
        serverRules.add(rule);
        saveRulesData();
        
        source.sendSuccess(() -> MessageUtil.success("commands.neoessentials.rules.added", serverRules.size()), false);
        return 1;
    }
    
    /**
     * Remove a rule by number
     */
    private static int removeRule(CommandSourceStack source, int number) {
        if (number < 1 || number > serverRules.size()) {
            source.sendFailure(MessageUtil.error("commands.neoessentials.rules.invalid_number", number, serverRules.size()));
            return 0;
        }
        
        String removedRule = serverRules.remove(number - 1);
        saveRulesData();
        
        String formattedRule = removedRule.replace("&", "§");
        source.sendSuccess(() -> MessageUtil.success("commands.neoessentials.rules.removed", number, formattedRule), false);
        return 1;
    }
    
    /**
     * Edit a rule by number
     */
    private static int editRule(CommandSourceStack source, int number, String newText) {
        if (number < 1 || number > serverRules.size()) {
            source.sendFailure(MessageUtil.error("commands.neoessentials.rules.invalid_number", number, serverRules.size()));
            return 0;
        }
        
        if (newText.length() > 200) {
            source.sendFailure(MessageUtil.error("commands.neoessentials.rules.too_long"));
            return 0;
        }
        
        serverRules.set(number - 1, newText);
        saveRulesData();
        
        source.sendSuccess(() -> MessageUtil.success("commands.neoessentials.rules.edited", number), false);
        return 1;
    }
    
    /**
     * Insert a rule at a specific position
     */
    private static int insertRule(CommandSourceStack source, int number, String rule) {
        if (number < 1 || number > serverRules.size() + 1) {
            source.sendFailure(MessageUtil.error("commands.neoessentials.rules.invalid_insert_position", number, serverRules.size() + 1));
            return 0;
        }
        
        if (rule.length() > 200) {
            source.sendFailure(MessageUtil.error("commands.neoessentials.rules.too_long"));
            return 0;
        }
        
        serverRules.add(number - 1, rule);
        saveRulesData();
        
        source.sendSuccess(() -> MessageUtil.success("commands.neoessentials.rules.inserted", number), false);
        return 1;
    }
    
    /**
     * Clear all rules
     */
    private static int clearRules(CommandSourceStack source) {
        if (serverRules.isEmpty()) {
            source.sendFailure(MessageUtil.error("commands.neoessentials.rules.already_empty"));
            return 0;
        }
        
        int count = serverRules.size();
        serverRules.clear();
        saveRulesData();
        
        source.sendSuccess(() -> MessageUtil.success("commands.neoessentials.rules.cleared", count), false);
        return 1;
    }
    
    /**
     * Reload rules from file
     */
    private static int reloadRules(CommandSourceStack source) {
        reload();
        int newCount = serverRules.size();
        source.sendSuccess(() -> MessageUtil.success("commands.neoessentials.rules.reloaded", newCount), false);
        return 1;
    }
    
    /**
     * Load rules data from file
     */
    private static void loadRulesData() {
        try {
            // Primary file
            if (Files.exists(RULES_DATA_FILE)) {
                String json = Files.readString(RULES_DATA_FILE);
                JsonObject data;
                try {
                    data = JsonParser.parseString(json).getAsJsonObject();
                } catch (Exception parseEx) {
                    LOGGER.error("╔══════════════════════════════════════════════════════╗");
                    LOGGER.error("║  RULES LOAD ERROR: rules_data.json is corrupt JSON  ║");
                    LOGGER.error("║  File: {}", RULES_DATA_FILE.toAbsolutePath());
                    LOGGER.error("║  Error: {}", parseEx.getMessage());
                    LOGGER.error("║  Fix:  Delete the file and run /rules reload to     ║");
                    LOGGER.error("║        regenerate defaults, then re-add your rules. ║");
                    LOGGER.error("╚══════════════════════════════════════════════════════╝");
                    createDefaultRules();
                    return;
                }
                serverRules.clear();
                if (data.has("rules")) {
                    JsonArray rulesArray = data.getAsJsonArray("rules");
                    for (JsonElement element : rulesArray) {
                        serverRules.add(element.getAsString());
                    }
                }
                LOGGER.debug("Loaded {} rules from {}", serverRules.size(), RULES_DATA_FILE.getFileName());
                return;
            }

            // Legacy fallback: rules.json (used by NeoEssentials <1.0.2.6)
            if (Files.exists(RULES_LEGACY_FILE)) {
                LOGGER.info("Migrating legacy rules.json → rules_data.json");
                String json = Files.readString(RULES_LEGACY_FILE);
                JsonObject data = JsonParser.parseString(json).getAsJsonObject();
                serverRules.clear();
                // Legacy format may store rules directly as an array or under a "rules" key
                if (data.has("rules")) {
                    JsonElement el = data.get("rules");
                    if (el.isJsonArray()) {
                        for (JsonElement element : el.getAsJsonArray()) {
                            serverRules.add(element.getAsString());
                        }
                    }
                } else {
                    // Treat every top-level string value as a rule
                    for (var entry : data.entrySet()) {
                        if (entry.getValue().isJsonPrimitive()) {
                            serverRules.add(entry.getValue().getAsString());
                        }
                    }
                }
                // Save to new format
                saveRulesData();
                LOGGER.info("Migrated {} rules from rules.json to rules_data.json", serverRules.size());
                return;
            }

            // No file found – create defaults and log guidance
            LOGGER.info("No rules_data.json found — generating default rules file at: {}", RULES_DATA_FILE.toAbsolutePath());
            LOGGER.info("  Edit rules in-game with:  /rules add <text>  |  /rules edit <n> <text>  |  /rules remove <n>");
            LOGGER.info("  Or edit directly:  config/neoessentials/rules_data.json  then run /rules reload");
            createDefaultRules();
        } catch (Exception e) {
            LOGGER.error("╔══════════════════════════════════════════════════════╗");
            LOGGER.error("║  RULES LOAD ERROR — using built-in defaults         ║");
            LOGGER.error("║  File: {}", RULES_DATA_FILE.toAbsolutePath());
            LOGGER.error("║  Error: {}", e.getMessage());
            LOGGER.error("║  Fix:                                                ║");
            LOGGER.error("║   1. Check file permissions on config/neoessentials/ ║");
            LOGGER.error("║   2. Verify rules_data.json is valid JSON            ║");
            LOGGER.error("║   3. Run /rules reload after fixing                  ║");
            LOGGER.error("╚══════════════════════════════════════════════════════╝");
            createDefaultRules();
        }
    }

    /**
     * Public reload method — called by /neoe reload and the dashboard API.
     */
    public static void reload() {
        loadRulesData();
        LOGGER.info("Rules reloaded — {} rule(s) active", serverRules.size());
    }

    /**
     * Replace the full rules list (called by the dashboard API after an edit).
     * Persists to disk immediately.
     */
    public static void setRules(List<String> newRules) {
        serverRules = new ArrayList<>(newRules);
        saveRulesData();
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
            LOGGER.error("Failed to save rules data: {}", e.getMessage());
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
    @SuppressWarnings("unused") // Public API method
    public static void showRulesToPlayer(ServerPlayer player) {
        showRules(player.createCommandSourceStack(), 1);
    }
}