package com.zerog.neoessentials.kits.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.zerog.neoessentials.kits.Kit;
import com.zerog.neoessentials.kits.KitManager;
import com.zerog.neoessentials.api.permissions.PermissionAPI;
import com.zerog.neoessentials.util.MessageUtil;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.level.ServerPlayer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Handles the /listkits command for administrative kit management overview.
 */
public class ListKitsCommand {
    private static final Logger LOGGER = LoggerFactory.getLogger(ListKitsCommand.class);
    private static final int KITS_PER_PAGE = 10;
    
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        // Check if kit module is enabled
        if (!com.zerog.neoessentials.config.ConfigManager.isKitSystemEnabled()) {
            return; // Don't register kit commands if module is disabled
        }
        
        if (!com.zerog.neoessentials.config.ConfigManager.getInstance().isCommandEnabled("listkits")) {
            return;
        }

        registerListKitsCommand(dispatcher, "listkits");
        registerListKitsCommand(dispatcher, "kits");
    }
    
    private static void registerListKitsCommand(CommandDispatcher<CommandSourceStack> dispatcher, String commandName) {
        dispatcher.register(Commands.literal(commandName)
            .requires(source -> {
                if (source.getEntity() instanceof ServerPlayer player) {
                    return PermissionAPI.hasPermission(player.getUUID(), "neoessentials.kits.list");
                }
                return source.hasPermission(4); // Console/OP fallback
            })
            .executes(ListKitsCommand::listFirstPage)
            .then(Commands.argument("page", IntegerArgumentType.integer(1))
                .executes(ListKitsCommand::listSpecificPage)
            )
        );
    }
    
    private static int listFirstPage(CommandContext<CommandSourceStack> context) {
        return listKits(context, 1);
    }
    
    private static int listSpecificPage(CommandContext<CommandSourceStack> context) {
        int page = IntegerArgumentType.getInteger(context, "page");
        return listKits(context, page);
    }
    
    private static int listKits(CommandContext<CommandSourceStack> context, int page) {
        CommandSourceStack source = context.getSource();

        try {
            // Check and deduct listkits command cost if economy is enabled
            var entity = source.getEntity();
            if (entity instanceof ServerPlayer player) {
                int cost = (int) com.zerog.neoessentials.config.ConfigManager.getKitCommandCost("listkits");
                if (cost > 0 && com.zerog.neoessentials.economy.managers.EconomyManager.getInstance().isEnabled()) {
                    var eco = com.zerog.neoessentials.economy.managers.EconomyManager.getInstance();
                    var bal = eco.getBalance(player.getUUID());
                    if (bal.doubleValue() < cost) {
                        source.sendFailure(com.zerog.neoessentials.util.MessageUtil.error("commands.neoessentials.listkits.not_enough_money", cost));
                        return 0;
                    }
                    if (!eco.subtractBalance(player.getUUID(), java.math.BigDecimal.valueOf(cost))) {
                        source.sendFailure(com.zerog.neoessentials.util.MessageUtil.error("commands.neoessentials.listkits.charge_failed"));
                        return 0;
                    }
                }
            }
            KitManager kitManager = KitManager.getInstance();
            Set<String> kitNames = kitManager.getAllKitNames();

            // Filter out one-time kits already used by the player if config is enabled
            boolean skipUsedOneTime = com.zerog.neoessentials.config.ConfigManager.isSkipUsedOneTimeKitsFromKitList();
            if (skipUsedOneTime && source.getEntity() instanceof ServerPlayer player) {
                kitNames = kitNames.stream().filter(kitName -> {
                    Kit kit = kitManager.getKit(kitName);
                    if (kit == null) return false;
                    // One-time kit: maxUses == 1 or < 0 (legacy)
                    int maxUses = kit.getMaxUses();
                    if (maxUses == 1 || maxUses < 0) {
                        int usageCount = kitManager.getUsageCount(player.getUUID(), kitName);
                        // If used at least once, skip
                        return usageCount < 1;
                    }
                    return true;
                }).collect(Collectors.toSet());
            }

            if (kitNames.isEmpty()) {
                source.sendSuccess(() -> MessageUtil.info("commands.neoessentials.listkits.empty"), false);
                return 1;
            }

            // Sort kit names for consistent display
            List<String> sortedKitNames = kitNames.stream()
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .toList();

            int totalKits = sortedKitNames.size();
            int maxPages = (int) Math.ceil((double) totalKits / KITS_PER_PAGE);

            // Validate page number
            if (page < 1 || page > maxPages) {
                source.sendFailure(MessageUtil.error("commands.neoessentials.listkits.invalid_page", 
                    page, maxPages));
                return 0;
            }
            
            // Calculate page bounds
            int startIndex = (page - 1) * KITS_PER_PAGE;
            int endIndex = Math.min(startIndex + KITS_PER_PAGE, totalKits);
            
            // Send header
            source.sendSuccess(() -> MessageUtil.info("commands.neoessentials.listkits.header", 
                page, maxPages, totalKits), false);
            source.sendSuccess(() -> MessageUtil.coloredText("§7▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬"), false);
            
            // Display kits for this page
            for (int i = startIndex; i < endIndex; i++) {
                String kitName = sortedKitNames.get(i);
                Kit kit = kitManager.getKit(kitName);
                
                if (kit != null) {
                    displayKitInfo(source, kit, i + 1);
                }
            }
            
            // Send footer with navigation hints
            source.sendSuccess(() -> MessageUtil.coloredText("§7▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬"), false);
            
            if (maxPages > 1) {
                if (page < maxPages) {
                    source.sendSuccess(() -> MessageUtil.info("commands.neoessentials.listkits.next_page", 
                        page + 1), false);
                }
                if (page > 1) {
                    source.sendSuccess(() -> MessageUtil.info("commands.neoessentials.listkits.prev_page", 
                        page - 1), false);
                }
            }
            
            return 1;
            
        } catch (Exception e) {
            LOGGER.error("Error listing kits (page {}): {}", page, e.getMessage(), e);
            source.sendFailure(MessageUtil.error("commands.neoessentials.listkits.error"));
            return 0;
        }
    }
    
    private static void displayKitInfo(CommandSourceStack source, Kit kit, int index) {
        String name = kit.getName();
        String displayName = kit.getDisplayName();
        int itemCount = kit.getItems().size();
        String cooldown = formatCooldown(kit.getCooldownMillis());
        String permission = kit.getPermission();
        
        // Basic kit info
        source.sendSuccess(() -> MessageUtil.info("commands.neoessentials.listkits.kit_entry", 
            index, name, displayName, itemCount), false);
        
        // Additional details
        if (!cooldown.equals("none")) {
            source.sendSuccess(() -> MessageUtil.coloredText("§8  └ Cooldown: " + cooldown), false);
        }
        
        if (permission != null && !permission.isEmpty()) {
            source.sendSuccess(() -> MessageUtil.coloredText("§8  └ Permission: " + permission), false);
        }
        
        if (kit.getDescription() != null && !kit.getDescription().isEmpty()) {
            final String desc = kit.getDescription().length() > 60 ? 
                kit.getDescription().substring(0, 57) + "..." : kit.getDescription();
            source.sendSuccess(() -> MessageUtil.coloredText("§8  └ Description: " + desc), false);
        }
    }
    
    private static String formatCooldown(long millis) {
        if (millis == 0) return "none";
        
        long seconds = millis / 1000;
        if (seconds < 60) return seconds + "s";
        
        long minutes = seconds / 60;
        if (minutes < 60) return minutes + "m";
        
        long hours = minutes / 60;
        if (hours < 24) {
            long remainingMinutes = minutes % 60;
            return remainingMinutes > 0 ? hours + "h " + remainingMinutes + "m" : hours + "h";
        }
        
        long days = hours / 24;
        long remainingHours = hours % 24;
        return remainingHours > 0 ? days + "d " + remainingHours + "h" : days + "d";
    }
}