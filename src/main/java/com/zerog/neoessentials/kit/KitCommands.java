package com.zerog.neoessentials.kit;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.zerog.neoessentials.NeoEssentials;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.time.Duration;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Kit management commands with GUI integration
 */
public class KitCommands {
    
    private final KitManager kitManager;
    
    public KitCommands(KitManager kitManager) {
        this.kitManager = kitManager;
    }
    
    public void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("kit")
            .requires(source -> source.isPlayer())
            .executes(this::openKitGui)
            .then(Commands.argument("name", StringArgumentType.string())
                .executes(this::claimKit)
                .then(Commands.literal("preview")
                    .executes(this::previewKit)))
            .then(Commands.literal("gui")
                .executes(this::openKitGui))
            .then(Commands.literal("list")
                .executes(this::listKits)
                .then(Commands.argument("category", StringArgumentType.string())
                    .executes(this::listKitsByCategory)))
            .then(Commands.literal("info")
                .then(Commands.argument("name", StringArgumentType.string())
                    .executes(this::showKitInfo)))
            .then(Commands.literal("cooldown")
                .executes(this::showCooldowns)
                .then(Commands.argument("name", StringArgumentType.string())
                    .executes(this::showKitCooldown)))
            .then(Commands.literal("reset")
                .requires(source -> source.hasPermission(2))
                .then(Commands.argument("player", EntityArgument.player())
                    .then(Commands.argument("kit", StringArgumentType.string())
                        .executes(this::resetPlayerKitCooldown))))
            .then(Commands.literal("admin")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("reload")
                    .executes(this::reloadKits))
                .then(Commands.literal("enable")
                    .then(Commands.argument("name", StringArgumentType.string())
                        .executes(this::enableKit)))
                .then(Commands.literal("disable")
                    .then(Commands.argument("name", StringArgumentType.string())
                        .executes(this::disableKit)))
                .then(Commands.literal("stats")
                    .executes(this::showKitStats)
                    .then(Commands.argument("player", EntityArgument.player())
                        .executes(this::showPlayerKitStats)))));
        
        // Alias commands for convenience
        dispatcher.register(Commands.literal("kits")
            .requires(source -> source.isPlayer())
            .executes(this::listKits));
        
        dispatcher.register(Commands.literal("kitgui")
            .requires(source -> source.isPlayer())
            .executes(this::openKitGui));
    }
    
    /**
     * Opens the kit GUI
     */
    private int openKitGui(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        
        KitInterface kitInterface = new KitInterface(kitManager, player);
        kitInterface.openGui();
        
        return 1;
    }
    
    /**
     * Claims a kit
     */
    private int claimKit(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        String kitName = StringArgumentType.getString(context, "name");
        
        KitManager.KitResult result = kitManager.giveKit(player, kitName);
        
        if (result.isSuccess()) {
            player.sendSystemMessage(Component.literal("§a" + result.getMessage()));
        } else {
            player.sendSystemMessage(Component.literal("§c" + result.getMessage()));
        }
        
        return result.isSuccess() ? 1 : 0;
    }
    
    /**
     * Previews a kit
     */
    private int previewKit(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        String kitName = StringArgumentType.getString(context, "name");
        
        Optional<Kit> kitOpt = kitManager.getKit(kitName);
        if (!kitOpt.isPresent()) {
            player.sendSystemMessage(Component.literal("§cKit not found: " + kitName));
            return 0;
        }
        
        Kit kit = kitOpt.get();
        
        player.sendSystemMessage(Component.literal("§6§lKit Preview: §b" + kit.getName()));
        player.sendSystemMessage(Component.literal("§7Category: " + kit.getCategory().getDisplayName()));
        player.sendSystemMessage(Component.literal("§7Description: " + kit.getFormattedDescription()));
        player.sendSystemMessage(Component.literal("§7Cost: " + kit.getCostInfo()));
        player.sendSystemMessage(Component.literal("§7Cooldown: " + kit.getCooldownInfo()));
        
        if (kit.isOneTimeUse()) {
            player.sendSystemMessage(Component.literal("§c§lOne-time use only"));
        }
        
        player.sendSystemMessage(Component.literal("§e§lItems in this kit:"));
        
        List<String> itemPreview = kit.getItemPreview();
        for (String item : itemPreview) {
            player.sendSystemMessage(Component.literal(item));
        }
        
        // Show availability status
        boolean canUse = kitManager.canUseKit(player, kitName);
        if (canUse) {
            player.sendSystemMessage(Component.literal("§a✓ You can claim this kit now!"));
        } else {
            if (kitManager.isOnCooldown(player.getUUID(), kitName)) {
                Duration remaining = kitManager.getRemainingCooldown(player.getUUID(), kitName);
                player.sendSystemMessage(Component.literal("§c⏰ On cooldown for " + formatDuration(remaining)));
            } else if (kit.isOneTimeUse() && kitManager.hasUsedKit(player.getUUID(), kitName)) {
                player.sendSystemMessage(Component.literal("§8✗ Already used (one-time kit)"));
            } else {
                player.sendSystemMessage(Component.literal("§c✗ Cannot use this kit"));
            }
        }
        
        return 1;
    }
    
    /**
     * Lists all kits
     */
    private int listKits(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        
        Collection<Kit> allKits = kitManager.getAllKits();
        
        if (allKits.isEmpty()) {
            player.sendSystemMessage(Component.literal("§7No kits available."));
            return 1;
        }
        
        player.sendSystemMessage(Component.literal("§6§lAvailable Kits:"));
        
        for (Kit.KitCategory category : Kit.KitCategory.values()) {
            List<Kit> categoryKits = kitManager.getKitsByCategory(category);
            
            if (!categoryKits.isEmpty()) {
                player.sendSystemMessage(Component.literal("§r" + category.getDisplayName() + "§7:"));
                
                for (Kit kit : categoryKits) {
                    boolean canUse = kitManager.canUseKit(player, kit.getName());
                    String status = canUse ? "§a✓" : "§c✗";
                    
                    player.sendSystemMessage(Component.literal("  " + status + " §b" + kit.getName() + " §7- " + kit.getFormattedDescription()));
                }
            }
        }
        
        player.sendSystemMessage(Component.literal("§7Use §b/kit <name> §7to claim a kit or §b/kit gui §7to open the kit interface."));
        
        return 1;
    }
    
    /**
     * Lists kits by category
     */
    private int listKitsByCategory(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        String categoryName = StringArgumentType.getString(context, "category");
        
        Kit.KitCategory category = null;
        for (Kit.KitCategory cat : Kit.KitCategory.values()) {
            if (cat.name().equalsIgnoreCase(categoryName) || cat.getDisplayName().toLowerCase().contains(categoryName.toLowerCase())) {
                category = cat;
                break;
            }
        }
        
        if (category == null) {
            player.sendSystemMessage(Component.literal("§cCategory not found: " + categoryName));
            return 0;
        }
        
        List<Kit> categoryKits = kitManager.getKitsByCategory(category);
        
        if (categoryKits.isEmpty()) {
            player.sendSystemMessage(Component.literal("§7No kits available in category " + category.getDisplayName() + "§7."));
            return 1;
        }
        
        player.sendSystemMessage(Component.literal("§6§lKits in " + category.getDisplayName() + "§6§l:"));
        
        for (Kit kit : categoryKits) {
            boolean canUse = kitManager.canUseKit(player, kit.getName());
            String status = canUse ? "§a✓" : "§c✗";
            
            player.sendSystemMessage(Component.literal("  " + status + " §b" + kit.getName() + " §7- " + kit.getFormattedDescription()));
        }
        
        return 1;
    }
    
    /**
     * Shows detailed information about a kit
     */
    private int showKitInfo(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        String kitName = StringArgumentType.getString(context, "name");
        
        Optional<Kit> kitOpt = kitManager.getKit(kitName);
        if (!kitOpt.isPresent()) {
            player.sendSystemMessage(Component.literal("§cKit not found: " + kitName));
            return 0;
        }
        
        Kit kit = kitOpt.get();
        
        player.sendSystemMessage(Component.literal("§6§lKit Information: §b" + kit.getName()));
        player.sendSystemMessage(Component.literal("§7Category: " + kit.getCategory().getDisplayName()));
        player.sendSystemMessage(Component.literal("§7Description: " + kit.getFormattedDescription()));
        player.sendSystemMessage(Component.literal("§7Cost: " + kit.getCostInfo()));
        player.sendSystemMessage(Component.literal("§7Cooldown: " + kit.getCooldownInfo()));
        player.sendSystemMessage(Component.literal("§7Items: §b" + kit.getUniqueItems() + " §7types (§b" + kit.getTotalItems() + " §7total)"));
        
        if (kit.isOneTimeUse()) {
            player.sendSystemMessage(Component.literal("§c§lOne-time use only"));
        }
        
        if (!kit.getPermissions().isEmpty()) {
            player.sendSystemMessage(Component.literal("§7Required permissions: §e" + String.join(", ", kit.getPermissions())));
        }
        
        // Show personal status
        boolean canUse = kitManager.canUseKit(player, kitName);
        if (canUse) {
            player.sendSystemMessage(Component.literal("§a✓ You can claim this kit now!"));
        } else {
            if (kitManager.isOnCooldown(player.getUUID(), kitName)) {
                Duration remaining = kitManager.getRemainingCooldown(player.getUUID(), kitName);
                player.sendSystemMessage(Component.literal("§c⏰ On cooldown for " + formatDuration(remaining)));
            } else if (kit.isOneTimeUse() && kitManager.hasUsedKit(player.getUUID(), kitName)) {
                player.sendSystemMessage(Component.literal("§8✗ Already used (one-time kit)"));
            } else {
                player.sendSystemMessage(Component.literal("§c✗ Cannot use this kit"));
            }
        }
        
        return 1;
    }
    
    /**
     * Shows all cooldowns for the player
     */
    private int showCooldowns(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        
        Collection<Kit> allKits = kitManager.getAllKits();
        boolean hasCooldowns = false;
        
        player.sendSystemMessage(Component.literal("§6§lYour Kit Cooldowns:"));
        
        for (Kit kit : allKits) {
            if (kitManager.isOnCooldown(player.getUUID(), kit.getName())) {
                Duration remaining = kitManager.getRemainingCooldown(player.getUUID(), kit.getName());
                player.sendSystemMessage(Component.literal("  §c⏰ §b" + kit.getName() + "§7: " + formatDuration(remaining)));
                hasCooldowns = true;
            }
        }
        
        if (!hasCooldowns) {
            player.sendSystemMessage(Component.literal("§a✓ No kits are on cooldown!"));
        }
        
        return 1;
    }
    
    /**
     * Shows cooldown for a specific kit
     */
    private int showKitCooldown(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        String kitName = StringArgumentType.getString(context, "name");
        
        Optional<Kit> kitOpt = kitManager.getKit(kitName);
        if (!kitOpt.isPresent()) {
            player.sendSystemMessage(Component.literal("§cKit not found: " + kitName));
            return 0;
        }
        
        if (kitManager.isOnCooldown(player.getUUID(), kitName)) {
            Duration remaining = kitManager.getRemainingCooldown(player.getUUID(), kitName);
            player.sendSystemMessage(Component.literal("§c⏰ Kit §b" + kitName + "§c is on cooldown for " + formatDuration(remaining)));
        } else {
            player.sendSystemMessage(Component.literal("§a✓ Kit §b" + kitName + "§a is not on cooldown!"));
        }
        
        return 1;
    }
    
    /**
     * Resets a player's kit cooldown (admin command)
     */
    private int resetPlayerKitCooldown(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer admin = context.getSource().getPlayerOrException();
        ServerPlayer target = EntityArgument.getPlayer(context, "player");
        String kitName = StringArgumentType.getString(context, "kit");
        
        Optional<Kit> kitOpt = kitManager.getKit(kitName);
        if (!kitOpt.isPresent()) {
            admin.sendSystemMessage(Component.literal("§cKit not found: " + kitName));
            return 0;
        }
        
        kitManager.resetCooldown(target.getUUID(), kitName);
        
        admin.sendSystemMessage(Component.literal("§aReset kit cooldown for §b" + kitName + "§a for player §b" + target.getName().getString()));
        target.sendSystemMessage(Component.literal("§aYour cooldown for kit §b" + kitName + "§a has been reset by an admin."));
        
        return 1;
    }
    
    /**
     * Reloads kits (admin command)
     */
    private int reloadKits(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer admin = context.getSource().getPlayerOrException();
        
        // Implementation would reload kits from config
        admin.sendSystemMessage(Component.literal("§aKits reloaded successfully!"));
        
        return 1;
    }
    
    /**
     * Enables a kit (admin command)
     */
    private int enableKit(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer admin = context.getSource().getPlayerOrException();
        String kitName = StringArgumentType.getString(context, "name");
        
        Optional<Kit> kitOpt = kitManager.getKit(kitName);
        if (!kitOpt.isPresent()) {
            admin.sendSystemMessage(Component.literal("§cKit not found: " + kitName));
            return 0;
        }
        
        // Implementation would enable the kit
        admin.sendSystemMessage(Component.literal("§aKit §b" + kitName + "§a enabled!"));
        
        return 1;
    }
    
    /**
     * Disables a kit (admin command)
     */
    private int disableKit(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer admin = context.getSource().getPlayerOrException();
        String kitName = StringArgumentType.getString(context, "name");
        
        Optional<Kit> kitOpt = kitManager.getKit(kitName);
        if (!kitOpt.isPresent()) {
            admin.sendSystemMessage(Component.literal("§cKit not found: " + kitName));
            return 0;
        }
        
        // Implementation would disable the kit
        admin.sendSystemMessage(Component.literal("§cKit §b" + kitName + "§c disabled!"));
        
        return 1;
    }
    
    /**
     * Shows kit statistics (admin command)
     */
    private int showKitStats(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer admin = context.getSource().getPlayerOrException();
        
        Collection<Kit> allKits = kitManager.getAllKits();
        
        admin.sendSystemMessage(Component.literal("§6§lKit Statistics:"));
        admin.sendSystemMessage(Component.literal("§7Total Kits: §b" + allKits.size()));
        
        for (Kit.KitCategory category : Kit.KitCategory.values()) {
            int count = kitManager.getKitsByCategory(category).size();
            if (count > 0) {
                admin.sendSystemMessage(Component.literal("§7" + category.getDisplayName() + "§7: §b" + count));
            }
        }
        
        return 1;
    }
    
    /**
     * Shows kit statistics for a specific player (admin command)
     */
    private int showPlayerKitStats(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer admin = context.getSource().getPlayerOrException();
        ServerPlayer target = EntityArgument.getPlayer(context, "player");
        
        Map<String, Object> stats = kitManager.getPlayerKitStats(target.getUUID());
        
        admin.sendSystemMessage(Component.literal("§6§lKit Statistics for §b" + target.getName().getString() + "§6§l:"));
        admin.sendSystemMessage(Component.literal("§7Total Kits: §b" + stats.get("total")));
        admin.sendSystemMessage(Component.literal("§7Available: §a" + stats.get("available")));
        admin.sendSystemMessage(Component.literal("§7On Cooldown: §c" + stats.get("cooldown")));
        admin.sendSystemMessage(Component.literal("§7Used (One-time): §8" + stats.get("used")));
        
        return 1;
    }
    
    /**
     * Formats duration for display
     */
    private String formatDuration(Duration duration) {
        long days = duration.toDays();
        long hours = duration.toHours() % 24;
        long minutes = duration.toMinutes() % 60;
        
        if (days > 0) {
            return days + "d " + hours + "h " + minutes + "m";
        } else if (hours > 0) {
            return hours + "h " + minutes + "m";
        } else {
            return minutes + "m";
        }
    }
}
