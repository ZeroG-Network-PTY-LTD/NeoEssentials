package com.neoessentials.commands.teleportation;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.neoessentials.api.home.HomeService;
import com.neoessentials.language.LanguageManager;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;

/**
 * Complete home system implementation - EssentialsX style
 * Commands: /home, /sethome, /delhome, /homes
 */
public class HomeCommand {
    
    private final HomeService homeService;
    private final LanguageManager languageManager;
    
    public HomeCommand(HomeService homeService, LanguageManager languageManager) {
        this.homeService = homeService;
        this.languageManager = languageManager;
    }
    
    public void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        // /home [name] - Teleport to home
        dispatcher.register(Commands.literal("home")
            .executes(this::teleportToDefaultHome)
            .then(Commands.argument("name", StringArgumentType.word())
                .executes(this::teleportToNamedHome)
            )
        );
        
        // /sethome [name] - Set home
        dispatcher.register(Commands.literal("sethome")
            .executes(this::setDefaultHome)
            .then(Commands.argument("name", StringArgumentType.word())
                .executes(this::setNamedHome)
            )
        );
        
        // /delhome <name> - Delete home
        dispatcher.register(Commands.literal("delhome")
            .then(Commands.argument("name", StringArgumentType.word())
                .executes(this::deleteHome)
            )
        );
        
        // /homes - List all homes
        dispatcher.register(Commands.literal("homes")
            .executes(this::listHomes)
        );
    }
    
    private int teleportToDefaultHome(CommandContext<CommandSourceStack> context) {
        return teleportToHome(context, "home");
    }
    
    private int teleportToNamedHome(CommandContext<CommandSourceStack> context) {
        String homeName = StringArgumentType.getString(context, "name");
        return teleportToHome(context, homeName);
    }
    
    private int teleportToHome(CommandContext<CommandSourceStack> context, String homeName) {
        if (!(context.getSource().getEntity() instanceof ServerPlayer player)) {
            context.getSource().sendFailure(languageManager.getMessage(null, "general.must_be_player"));
            return 0;
        }
        
        homeService.teleportToHome(player, homeName).thenAccept(result -> {
            homeService.sendTeleportMessage(player, result, homeName);
        });
        
        return 1;
    }
    
    private int setDefaultHome(CommandContext<CommandSourceStack> context) {
        return setHome(context, "home");
    }
    
    private int setNamedHome(CommandContext<CommandSourceStack> context) {
        String homeName = StringArgumentType.getString(context, "name");
        return setHome(context, homeName);
    }
    
    private int setHome(CommandContext<CommandSourceStack> context, String homeName) {
        if (!(context.getSource().getEntity() instanceof ServerPlayer player)) {
            context.getSource().sendFailure(languageManager.getMessage(null, "general.must_be_player"));
            return 0;
        }
        
        homeService.setHome(player, homeName).thenAccept(result -> {
            homeService.sendSetHomeMessage(player, result, homeName);
        });
        
        return 1;
    }
    
    private int deleteHome(CommandContext<CommandSourceStack> context) {
        if (!(context.getSource().getEntity() instanceof ServerPlayer player)) {
            context.getSource().sendFailure(languageManager.getMessage(null, "general.must_be_player"));
            return 0;
        }
        
        String homeName = StringArgumentType.getString(context, "name");
        
        homeService.deleteHome(player, homeName).thenAccept(result -> {
            homeService.sendDeleteMessage(player, result, homeName);
        });
        
        return 1;
    }
    
    private int listHomes(CommandContext<CommandSourceStack> context) {
        if (!(context.getSource().getEntity() instanceof ServerPlayer player)) {
            context.getSource().sendFailure(languageManager.getMessage(null, "general.must_be_player"));
            return 0;
        }
        
        homeService.getPlayerHomes(player).thenAccept(homes -> {
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
        });
        
        return 1;
    }
}
