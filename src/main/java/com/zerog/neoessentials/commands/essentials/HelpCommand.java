package com.zerog.neoessentials.commands.essentials;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.zerog.neoessentials.util.MessageUtil;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.server.level.ServerPlayer;

import java.util.*;

public class HelpCommand {
    
    private static final Map<String, List<HelpEntry>> HELP_CATEGORIES = new HashMap<>();
    private static final int ENTRIES_PER_PAGE = 8;
    
    static {
        setupHelpEntries();
    }
    
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("help")
            .executes(context -> showHelp(context, "general", 1))
            .then(Commands.argument("category", StringArgumentType.string())
                .suggests((context, builder) -> {
                    HELP_CATEGORIES.keySet().forEach(builder::suggest);
                    return builder.buildFuture();
                })
                .executes(context -> showHelp(context, StringArgumentType.getString(context, "category"), 1))
                .then(Commands.argument("page", IntegerArgumentType.integer(1))
                    .executes(context -> showHelp(context, 
                        StringArgumentType.getString(context, "category"), 
                        IntegerArgumentType.getInteger(context, "page"))))));
        
        // Also register /? as an alias
        dispatcher.register(Commands.literal("?")
            .executes(context -> showHelp(context, "general", 1)));
    }
    
    private static int showHelp(CommandContext<CommandSourceStack> context, String category, int page) {
        CommandSourceStack source = context.getSource();
        
        List<HelpEntry> entries = HELP_CATEGORIES.get(category.toLowerCase());
        if (entries == null) {
            sendTranslatedMessage(source, "neoessentials.help.unknown_category", category);
            sendTranslatedMessage(source, "neoessentials.help.available_categories", String.join(", ", HELP_CATEGORIES.keySet()));
            return 0;
        }

        int totalPages = (int) Math.ceil((double) entries.size() / ENTRIES_PER_PAGE);
        if (page < 1 || page > totalPages) {
            sendTranslatedMessage(source, "neoessentials.help.invalid_page", page); // Fix: show requested page
            return 0;
        }

        // Header
        sendTranslatedMessage(source, "neoessentials.help.header", category, page, totalPages);
        
        // Calculate entries for this page
        int startIndex = (page - 1) * ENTRIES_PER_PAGE;
        int endIndex = Math.min(startIndex + ENTRIES_PER_PAGE, entries.size());
        
        for (int i = startIndex; i < endIndex; i++) {
            HelpEntry entry = entries.get(i);
            
            if (source.getEntity() instanceof ServerPlayer) {
                // Interactive help for players
                Component helpComponent = MessageUtil.translatable("neoessentials.help.entry", entry.command, entry.description)
                    .withStyle(style -> style
                        .withClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/" + entry.command))
                        .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, 
                            MessageUtil.translatable("neoessentials.help.entry_hover", entry.usage))));
                source.sendSuccess(() -> helpComponent, false);
            } else {
                // Simple text for console
                sendTranslatedMessage(source, "neoessentials.help.entry", entry.command, entry.description);
            }
        }
        
        // Navigation (only for players)
        if (totalPages > 1 && source.getEntity() instanceof ServerPlayer) {
            MutableComponent navigation = Component.literal("");
            boolean hasPrev = false;
            if (page > 1) {
                MutableComponent prevButton = MessageUtil.translatable("neoessentials.help.prev_button")
                    .withStyle(style -> style
                        .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/help " + category + " " + (page - 1)))
                        .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, MessageUtil.translatable("neoessentials.help.prev_hover"))));
                navigation = navigation.append(prevButton);
                hasPrev = true;
            }
            MutableComponent pageInfo = MessageUtil.translatable("neoessentials.help.page_info", page, totalPages);
            if (hasPrev) navigation = navigation.append(Component.literal(" | "));
            navigation = navigation.append(pageInfo);
            if (page < totalPages) {
                navigation = navigation.append(Component.literal(" | "));
                MutableComponent nextButton = MessageUtil.translatable("neoessentials.help.next_button")
                    .withStyle(style -> style
                        .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/help " + category + " " + (page + 1)))
                        .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, MessageUtil.translatable("neoessentials.help.next_hover"))));
                navigation = navigation.append(nextButton);
            }
            final MutableComponent finalNav = navigation;
            source.sendSuccess(() -> finalNav, false);
        } else if (totalPages > 1) {
            // Simple navigation for console
            sendTranslatedMessage(source, "neoessentials.help.page_info_console", page, totalPages, category);
        }
        
        return 1;
    }
    
    private static void sendTranslatedMessage(CommandSourceStack source, String key, Object... args) {
        if (source.getEntity() instanceof ServerPlayer player) {
            MessageUtil.sendTranslatedMessage(player, key, args);
        } else {
            source.sendSuccess(() -> MessageUtil.translatable(key, args), false);
        }
    }
    
    private static void setupHelpEntries() {
        // General/Essential commands
        List<HelpEntry> general = Arrays.asList(
            new HelpEntry("help [category] [page]", "help", "Show this help menu"),
            new HelpEntry("heal [player]", "heal", "Restore health to full"),
            new HelpEntry("feed [player]", "feed", "Restore hunger to full"),
            new HelpEntry("god [player]", "god", "Toggle god mode (invincibility)"),
            new HelpEntry("vanish [player]", "vanish", "Toggle invisibility from other players"),
            new HelpEntry("fly [player]", "fly", "Toggle flight mode"),
            new HelpEntry("speed <walk|fly> <speed> [player]", "speed", "Set movement speed"),
            new HelpEntry("repair [hand|all]", "repair", "Repair items"),
            new HelpEntry("workbench", "workbench", "Open crafting table remotely"),
            new HelpEntry("anvil", "anvil", "Open anvil remotely")
        );
        HELP_CATEGORIES.put("general", general);
        
        // Teleportation commands
        List<HelpEntry> teleport = Arrays.asList(
            new HelpEntry("tp <player> [target]", "tp", "Teleport to player or teleport player to target"),
            new HelpEntry("tphere <player>", "tphere", "Teleport player to your location"),
            new HelpEntry("tpa <player>", "tpa", "Request to teleport to player"),
            new HelpEntry("tpahere <player>", "tpahere", "Request player to teleport to you"),
            new HelpEntry("tpaccept [player]", "tpaccept", "Accept teleport request"),
            new HelpEntry("tpdeny [player]", "tpdeny", "Deny teleport request"),
            new HelpEntry("back", "back", "Teleport to your last location"),
            new HelpEntry("spawn", "spawn", "Teleport to spawn point"),
            new HelpEntry("setspawn", "setspawn", "Set the world spawn point")
        );
        HELP_CATEGORIES.put("teleport", teleport);
        
        // Home and Warp commands
        List<HelpEntry> homes = Arrays.asList(
            new HelpEntry("home [name]", "home", "Teleport to your home"),
            new HelpEntry("sethome [name]", "sethome", "Set a home location"),
            new HelpEntry("delhome <name>", "delhome", "Delete a home"),
            new HelpEntry("homes", "homes", "List your homes"),
            new HelpEntry("warp <name>", "warp", "Teleport to a warp"),
            new HelpEntry("setwarp <name>", "setwarp", "Create a warp (admin)"),
            new HelpEntry("delwarp <name>", "delwarp", "Delete a warp (admin)"),
            new HelpEntry("warps", "warps", "List available warps")
        );
        HELP_CATEGORIES.put("homes", homes);
        
        // Moderation commands
        List<HelpEntry> moderation = Arrays.asList(
            new HelpEntry("ban <player> [reason]", "ban", "Ban a player permanently"),
            new HelpEntry("tempban <player> <duration> [reason]", "tempban", "Ban a player temporarily"),
            new HelpEntry("unban <player>", "unban", "Remove a player's ban"),
            new HelpEntry("kick <player> [reason]", "kick", "Kick a player from the server"),
            new HelpEntry("mute <player> [duration] [reason]", "mute", "Mute a player"),
            new HelpEntry("unmute <player>", "unmute", "Remove a player's mute"),
            new HelpEntry("warn <player> <reason>", "warn", "Issue a warning to a player"),
            new HelpEntry("jail <player> [duration]", "jail", "Jail a player"),
            new HelpEntry("unjail <player>", "unjail", "Release a player from jail")
        );
        HELP_CATEGORIES.put("moderation", moderation);
        
        // Server management
        List<HelpEntry> server = Arrays.asList(
            new HelpEntry("time <set|add> <value>", "time", "Manage world time"),
            new HelpEntry("weather <clear|rain|thunder>", "weather", "Change weather"),
            new HelpEntry("give <player> <item> [amount]", "give", "Give items to players"),
            new HelpEntry("list", "list", "Show online players"),
            new HelpEntry("whois <player>", "whois", "Show player information"),
            new HelpEntry("seen <player>", "seen", "Check when player was last online"),
            new HelpEntry("rules", "rules", "Display server rules"),
            new HelpEntry("broadcast <message>", "broadcast", "Send message to all players")
        );
        HELP_CATEGORIES.put("server", server);
        
        // Economy commands
        List<HelpEntry> economy = Arrays.asList(
            new HelpEntry("balance [player]", "balance", "Check balance"),
            new HelpEntry("pay <player> <amount>", "pay", "Send money to player"),
            new HelpEntry("eco give <player> <amount>", "eco", "Give money (admin)"),
            new HelpEntry("eco take <player> <amount>", "eco", "Take money (admin)"),
            new HelpEntry("eco set <player> <amount>", "eco", "Set balance (admin)"),
            new HelpEntry("baltop", "baltop", "Show richest players")
        );
        HELP_CATEGORIES.put("economy", economy);
    }
    
    private static class HelpEntry {
        final String usage;
        final String command;
        final String description;
        
        HelpEntry(String usage, String command, String description) {
            this.usage = usage;
            this.command = command;
            this.description = description;
        }
    }
}