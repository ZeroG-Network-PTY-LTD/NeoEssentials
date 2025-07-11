package com.zerog.neoessentials.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.zerog.neoessentials.NeoEssentials;
import com.zerog.neoessentials.data.EnhancedHome;
import com.zerog.neoessentials.gui.EnhancedHomeInterface;
import com.zerog.neoessentials.managers.EnhancedHomeManager;
import com.zerog.neoessentials.util.TextUtils;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;
import java.util.Optional;

/**
 * Enhanced home management commands with GUI integration
 */
public class EnhancedHomeCommands {
    
    private final EnhancedHomeManager homeManager;
    
    public EnhancedHomeCommands(EnhancedHomeManager homeManager) {
        this.homeManager = homeManager;
    }
    
    public void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("enhomemanager")
            .requires(source -> source.isPlayer())
            .executes(this::openHomeManager)
            .then(Commands.literal("gui")
                .executes(this::openHomeManager))
            .then(Commands.literal("create")
                .then(Commands.argument("name", StringArgumentType.string())
                    .executes(this::createHome)
                    .then(Commands.argument("category", StringArgumentType.string())
                        .executes(this::createHomeWithCategory)
                        .then(Commands.argument("description", StringArgumentType.greedyString())
                            .executes(this::createHomeWithDescription)))))
            .then(Commands.literal("delete")
                .then(Commands.argument("name", StringArgumentType.string())
                    .executes(this::deleteHome)))
            .then(Commands.literal("teleport")
                .then(Commands.argument("name", StringArgumentType.string())
                    .executes(this::teleportToHome)))
            .then(Commands.literal("list")
                .executes(this::listHomes)
                .then(Commands.argument("category", StringArgumentType.string())
                    .executes(this::listHomesByCategory)))
            .then(Commands.literal("info")
                .then(Commands.argument("name", StringArgumentType.string())
                    .executes(this::showHomeInfo)))
            .then(Commands.literal("edit")
                .then(Commands.argument("name", StringArgumentType.string())
                    .then(Commands.literal("description")
                        .then(Commands.argument("description", StringArgumentType.greedyString())
                            .executes(this::editHomeDescription)))
                    .then(Commands.literal("category")
                        .then(Commands.argument("category", StringArgumentType.string())
                            .executes(this::editHomeCategory)))
                    .then(Commands.literal("public")
                        .then(Commands.argument("public", StringArgumentType.string())
                            .executes(this::editHomePublic)))))
            .then(Commands.literal("public")
                .executes(this::listPublicHomes)));
        
        // Alias commands for convenience
        dispatcher.register(Commands.literal("enhome")
            .requires(source -> source.isPlayer())
            .executes(this::openHomeManager)
            .then(Commands.argument("name", StringArgumentType.string())
                .executes(this::teleportToHome)));
        
        dispatcher.register(Commands.literal("ensethome")
            .requires(source -> source.isPlayer())
            .then(Commands.argument("name", StringArgumentType.string())
                .executes(this::createHome)));
        
        dispatcher.register(Commands.literal("endelhome")
            .requires(source -> source.isPlayer())
            .then(Commands.argument("name", StringArgumentType.string())
                .executes(this::deleteHome)));
        
        dispatcher.register(Commands.literal("enhomes")
            .requires(source -> source.isPlayer())
            .executes(this::listHomes));
    }
    
    /**
     * Opens the enhanced home manager GUI
     */
    private int openHomeManager(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        
        EnhancedHomeInterface homeInterface = new EnhancedHomeInterface(homeManager, player);
        homeInterface.openGui();
        
        return 1;
    }
    
    /**
     * Creates a new home
     */
    private int createHome(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        String name = StringArgumentType.getString(context, "name");
        
        if (homeManager.createHome(player, name, EnhancedHome.HomeCategory.GENERAL, "")) {
            player.sendSystemMessage(Component.literal("§aHome '§b" + name + "§a' created successfully!"));
        } else {
            player.sendSystemMessage(Component.literal("§cFailed to create home. Name may already exist or category limit reached."));
        }
        
        return 1;
    }
    
    /**
     * Creates a new home with specified category
     */
    private int createHomeWithCategory(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        String name = StringArgumentType.getString(context, "name");
        String categoryName = StringArgumentType.getString(context, "category");
        
        EnhancedHome.HomeCategory category = EnhancedHome.HomeCategory.fromString(categoryName);
        
        if (homeManager.createHome(player, name, category, "")) {
            player.sendSystemMessage(Component.literal("§aHome '§b" + name + "§a' created successfully in category §e" + category.getDisplayName() + "§a!"));
        } else {
            player.sendSystemMessage(Component.literal("§cFailed to create home. Name may already exist or category limit reached."));
        }
        
        return 1;
    }
    
    /**
     * Creates a new home with description
     */
    private int createHomeWithDescription(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        String name = StringArgumentType.getString(context, "name");
        String categoryName = StringArgumentType.getString(context, "category");
        String description = StringArgumentType.getString(context, "description");
        
        EnhancedHome.HomeCategory category = EnhancedHome.HomeCategory.fromString(categoryName);
        
        if (homeManager.createHome(player, name, category, description)) {
            player.sendSystemMessage(Component.literal("§aHome '§b" + name + "§a' created successfully!"));
        } else {
            player.sendSystemMessage(Component.literal("§cFailed to create home. Name may already exist or category limit reached."));
        }
        
        return 1;
    }
    
    /**
     * Deletes a home
     */
    private int deleteHome(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        String name = StringArgumentType.getString(context, "name");
        
        if (homeManager.deleteHome(player.getUUID(), name)) {
            player.sendSystemMessage(Component.literal("§aHome '§b" + name + "§a' deleted successfully!"));
        } else {
            player.sendSystemMessage(Component.literal("§cHome not found or you don't have permission to delete it."));
        }
        
        return 1;
    }
    
    /**
     * Teleports to a home
     */
    private int teleportToHome(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        String name = StringArgumentType.getString(context, "name");
        
        if (homeManager.teleportToHome(player, name)) {
            player.sendSystemMessage(Component.literal("§aTeleported to home '§b" + name + "§a'!"));
        } else {
            player.sendSystemMessage(Component.literal("§cHome not found or you don't have permission to use it."));
        }
        
        return 1;
    }
    
    /**
     * Lists all homes
     */
    private int listHomes(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        
        List<EnhancedHome> homes = homeManager.getPlayerHomes(player.getUUID());
        
        if (homes.isEmpty()) {
            player.sendSystemMessage(Component.literal("§7You don't have any homes yet. Use §b/enhome create <name> §7to create one!"));
            return 1;
        }
        
        player.sendSystemMessage(Component.literal("§6§lYour Enhanced Homes:"));
        
        for (EnhancedHome.HomeCategory category : EnhancedHome.HomeCategory.values()) {
            List<EnhancedHome> categoryHomes = homeManager.getPlayerHomesByCategory(player.getUUID(), category);
            
            if (!categoryHomes.isEmpty()) {
                player.sendSystemMessage(Component.literal("§r" + category.getDisplayName() + "§7:"));
                
                for (EnhancedHome home : categoryHomes) {
                    String visibility = home.isPublic() ? "§a[Public]" : "§c[Private]";
                    player.sendSystemMessage(Component.literal("  §7- §b" + home.getName() + " " + visibility + " §7- " + home.getFormattedDescription()));
                }
            }
        }
        
        return 1;
    }
    
    /**
     * Lists homes by category
     */
    private int listHomesByCategory(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        String categoryName = StringArgumentType.getString(context, "category");
        
        EnhancedHome.HomeCategory category = EnhancedHome.HomeCategory.fromString(categoryName);
        List<EnhancedHome> homes = homeManager.getPlayerHomesByCategory(player.getUUID(), category);
        
        if (homes.isEmpty()) {
            player.sendSystemMessage(Component.literal("§7You don't have any homes in category §e" + category.getDisplayName() + "§7."));
            return 1;
        }
        
        player.sendSystemMessage(Component.literal("§6§lYour " + category.getDisplayName() + " §6§lHomes:"));
        
        for (EnhancedHome home : homes) {
            String visibility = home.isPublic() ? "§a[Public]" : "§c[Private]";
            player.sendSystemMessage(Component.literal("  §7- §b" + home.getName() + " " + visibility + " §7- " + home.getFormattedDescription()));
        }
        
        return 1;
    }
    
    /**
     * Shows detailed information about a home
     */
    private int showHomeInfo(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        String name = StringArgumentType.getString(context, "name");
        
        EnhancedHome home = homeManager.getHome(player.getUUID(), name);
        if (home == null) {
            player.sendSystemMessage(Component.literal("§cHome not found."));
            return 1;
        }
        
        player.sendSystemMessage(Component.literal("§6§lHome Information: §b" + home.getName()));
        player.sendSystemMessage(Component.literal("§7Category: " + home.getCategory().getDisplayName()));
        player.sendSystemMessage(Component.literal("§7Description: " + home.getFormattedDescription()));
        player.sendSystemMessage(Component.literal("§7Location: §b" + home.getDimension().location() + " " + home.getPosition().toShortString()));
        player.sendSystemMessage(Component.literal("§7Visibility: " + (home.isPublic() ? "§aPublic" : "§cPrivate")));
        player.sendSystemMessage(Component.literal("§7" + home.getUsageStats()));
        player.sendSystemMessage(Component.literal("§7Created: §b" + home.getCreated().format(java.time.format.DateTimeFormatter.ofPattern("MM/dd/yyyy HH:mm"))));
        
        return 1;
    }
    
    /**
     * Edits home description
     */
    private int editHomeDescription(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        String name = StringArgumentType.getString(context, "name");
        String description = StringArgumentType.getString(context, "description");
        
        EnhancedHome home = homeManager.getHome(player.getUUID(), name);
        if (home == null) {
            player.sendSystemMessage(Component.literal("§cHome not found."));
            return 1;
        }
        
        if (homeManager.updateHome(player.getUUID(), name, description, home.getCategory(), home.isPublic())) {
            player.sendSystemMessage(Component.literal("§aHome description updated successfully!"));
        } else {
            player.sendSystemMessage(Component.literal("§cFailed to update home description."));
        }
        
        return 1;
    }
    
    /**
     * Edits home category
     */
    private int editHomeCategory(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        String name = StringArgumentType.getString(context, "name");
        String categoryName = StringArgumentType.getString(context, "category");
        
        EnhancedHome home = homeManager.getHome(player.getUUID(), name);
        if (home == null) {
            player.sendSystemMessage(Component.literal("§cHome not found."));
            return 1;
        }
        
        EnhancedHome.HomeCategory newCategory = EnhancedHome.HomeCategory.fromString(categoryName);
        
        if (homeManager.updateHome(player.getUUID(), name, home.getDescription(), newCategory, home.isPublic())) {
            player.sendSystemMessage(Component.literal("§aHome category updated to " + newCategory.getDisplayName() + "§a!"));
        } else {
            player.sendSystemMessage(Component.literal("§cFailed to update home category."));
        }
        
        return 1;
    }
    
    /**
     * Edits home public status
     */
    private int editHomePublic(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        String name = StringArgumentType.getString(context, "name");
        String publicStr = StringArgumentType.getString(context, "public");
        
        EnhancedHome home = homeManager.getHome(player.getUUID(), name);
        if (home == null) {
            player.sendSystemMessage(Component.literal("§cHome not found."));
            return 1;
        }
        
        boolean isPublic = publicStr.equalsIgnoreCase("true") || publicStr.equalsIgnoreCase("yes") || publicStr.equalsIgnoreCase("on");
        
        if (homeManager.updateHome(player.getUUID(), name, home.getDescription(), home.getCategory(), isPublic)) {
            player.sendSystemMessage(Component.literal("§aHome is now " + (isPublic ? "§apublic" : "§cprivate") + "§a!"));
        } else {
            player.sendSystemMessage(Component.literal("§cFailed to update home visibility."));
        }
        
        return 1;
    }
    
    /**
     * Lists public homes
     */
    private int listPublicHomes(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        
        List<EnhancedHome> publicHomes = homeManager.getPublicHomes();
        
        if (publicHomes.isEmpty()) {
            player.sendSystemMessage(Component.literal("§7There are no public homes available."));
            return 1;
        }
        
        player.sendSystemMessage(Component.literal("§6§lPublic Homes:"));
        
        for (EnhancedHome home : publicHomes) {
            player.sendSystemMessage(Component.literal("  §7- §b" + home.getName() + " §7by §e" + home.getOwner() + " §7- " + home.getFormattedDescription()));
        }
        
        return 1;
    }
}
