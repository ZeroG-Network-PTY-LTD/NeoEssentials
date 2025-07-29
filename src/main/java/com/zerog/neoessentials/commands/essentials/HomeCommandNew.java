package com.zerog.neoessentials.commands.essentials;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.neoessentials.api.home.HomeService;
import com.neoessentials.language.LanguageManager;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/**
 * Complete home system implementation for NeoEssentials - EssentialsX style
 * Commands: /home, /sethome, /delhome, /homes
 * 
 * @author ZeroG
 * @since 2.0.0
 */
public class HomeCommandNew {
    
    private static HomeService homeService;
    private static LanguageManager languageManager;
    
    /**
     * Initialize the command with services (called by CommandManager)
     */
    public static void initialize(HomeService service, LanguageManager lang) {
        homeService = service;
        languageManager = lang;
    }
    
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        // /home [name] - Teleport to home
        dispatcher.register(Commands.literal("home")
            .executes(HomeCommandNew::teleportToDefaultHome)
            .then(Commands.argument("name", StringArgumentType.word())
                .executes(HomeCommandNew::teleportToNamedHome)
            )
        );
        
        // /sethome [name] - Set home
        dispatcher.register(Commands.literal("sethome")
            .executes(HomeCommandNew::setDefaultHome)
            .then(Commands.argument("name", StringArgumentType.word())
                .executes(HomeCommandNew::setNamedHome)
            )
        );
        
        // /delhome <name> - Delete home
        dispatcher.register(Commands.literal("delhome")
            .then(Commands.argument("name", StringArgumentType.word())
                .executes(HomeCommandNew::deleteHome)
            )
        );
        
        // /homes - List all homes
        dispatcher.register(Commands.literal("homes")
            .executes(HomeCommandNew::listHomes)
        );
    }
    
    private static int teleportToDefaultHome(CommandContext<CommandSourceStack> context) {
        return teleportToHome(context, "home");
    }
    
    private static int teleportToNamedHome(CommandContext<CommandSourceStack> context) {
        String homeName = StringArgumentType.getString(context, "name");
        return teleportToHome(context, homeName);
    }
    
    private static int teleportToHome(CommandContext<CommandSourceStack> context, String homeName) {
        if (!(context.getSource().getEntity() instanceof ServerPlayer player)) {
            context.getSource().sendFailure(Component.literal("§cThis command can only be used by players."));
            return 0;
        }
        
        if (homeService == null) {
            player.sendSystemMessage(Component.literal("§cHome service not available."));
            return 0;
        }
        
        homeService.teleportToHome(player, homeName).thenAccept(result -> {
            homeService.sendTeleportMessage(player, result, homeName);
        });
        
        return 1;
    }
    
    private static int setDefaultHome(CommandContext<CommandSourceStack> context) {
        return setHome(context, "home");
    }
    
    private static int setNamedHome(CommandContext<CommandSourceStack> context) {
        String homeName = StringArgumentType.getString(context, "name");
        return setHome(context, homeName);
    }
    
    private static int setHome(CommandContext<CommandSourceStack> context, String homeName) {
        if (!(context.getSource().getEntity() instanceof ServerPlayer player)) {
            context.getSource().sendFailure(Component.literal("§cThis command can only be used by players."));
            return 0;
        }
        
        if (homeService == null) {
            player.sendSystemMessage(Component.literal("§cHome service not available."));
            return 0;
        }
        
        homeService.setHome(player, homeName).thenAccept(result -> {
            homeService.sendSetHomeMessage(player, result, homeName);
        });
        
        return 1;
    }
    
    private static int deleteHome(CommandContext<CommandSourceStack> context) {
        if (!(context.getSource().getEntity() instanceof ServerPlayer player)) {
            context.getSource().sendFailure(Component.literal("§cThis command can only be used by players."));
            return 0;
        }
        
        String homeName = StringArgumentType.getString(context, "name");
        
        if (homeService == null) {
            player.sendSystemMessage(Component.literal("§cHome service not available."));
            return 0;
        }
        
        homeService.deleteHome(player, homeName).thenAccept(result -> {
            homeService.sendDeleteMessage(player, result, homeName);
        });
        
        return 1;
    }
    
    private static int listHomes(CommandContext<CommandSourceStack> context) {
        if (!(context.getSource().getEntity() instanceof ServerPlayer player)) {
            context.getSource().sendFailure(Component.literal("§cThis command can only be used by players."));
            return 0;
        }
        
        if (homeService == null) {
            player.sendSystemMessage(Component.literal("§cHome service not available."));
            return 0;
        }
        
        homeService.getPlayerHomes(player).thenAccept(homes -> {
            if (languageManager != null) {
                Component header = languageManager.getMessage(player, "home.list_header");
                player.sendSystemMessage(header);
                
                if (homes.isEmpty()) {
                    Component noHomes = languageManager.getMessage(player, "home.list_none");
                    player.sendSystemMessage(noHomes);
                } else {
                    for (String homeName : homes) {
                        Component homeEntry = Component.literal("§7- §e" + homeName);
                        player.sendSystemMessage(homeEntry);
                    }
                }
            } else {
                // Fallback if language manager not available
                player.sendSystemMessage(Component.literal("§6Your homes:"));
                if (homes.isEmpty()) {
                    player.sendSystemMessage(Component.literal("§cYou have no homes set."));
                } else {
                    for (String homeName : homes) {
                        player.sendSystemMessage(Component.literal("§7- §e" + homeName));
                    }
                }
            }
        });
        
        return 1;
    }
}
