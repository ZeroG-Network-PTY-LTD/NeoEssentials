package com.zerog.neoessentials.commands.essentials;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameType;

/**
 * GameMode command implementation - /gamemode, /gm
 * Allows changing game mode for players
 */
public class GameModeCommand {
    
    private static final SuggestionProvider<CommandSourceStack> GAMEMODE_SUGGESTIONS = 
        (context, builder) -> SharedSuggestionProvider.suggest(
            new String[]{"survival", "creative", "adventure", "spectator", "0", "1", "2", "3"}, 
            builder
        );
    
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        // /gamemode <mode> [player] - Change game mode
        dispatcher.register(Commands.literal("gamemode")
            .requires(source -> source.hasPermission(2))
            .then(Commands.argument("mode", StringArgumentType.word())
                .suggests(GAMEMODE_SUGGESTIONS)
                .executes(ctx -> setGameMode(ctx, StringArgumentType.getString(ctx, "mode"), null))
                .then(Commands.argument("player", EntityArgument.player())
                    .executes(ctx -> setGameMode(ctx, StringArgumentType.getString(ctx, "mode"), EntityArgument.getPlayer(ctx, "player")))
                )
            )
        );
        
        // /gm - Alias for /gamemode  
        dispatcher.register(Commands.literal("gm")
            .requires(source -> source.hasPermission(2))
            .then(Commands.argument("mode", StringArgumentType.word())
                .suggests(GAMEMODE_SUGGESTIONS)
                .executes(ctx -> setGameMode(ctx, StringArgumentType.getString(ctx, "mode"), null))
                .then(Commands.argument("player", EntityArgument.player())
                    .executes(ctx -> setGameMode(ctx, StringArgumentType.getString(ctx, "mode"), EntityArgument.getPlayer(ctx, "player")))
                )
            )
        );
        
        // Individual gamemode commands
        registerShortcuts(dispatcher);
    }
    
    private static void registerShortcuts(CommandDispatcher<CommandSourceStack> dispatcher) {
        // /gms - Survival mode
        dispatcher.register(Commands.literal("gms")
            .requires(source -> source.hasPermission(2))
            .executes(ctx -> setGameMode(ctx, "survival", null))
            .then(Commands.argument("player", EntityArgument.player())
                .executes(ctx -> setGameMode(ctx, "survival", EntityArgument.getPlayer(ctx, "player")))
            )
        );
        
        // /gmc - Creative mode
        dispatcher.register(Commands.literal("gmc")
            .requires(source -> source.hasPermission(2))
            .executes(ctx -> setGameMode(ctx, "creative", null))
            .then(Commands.argument("player", EntityArgument.player())
                .executes(ctx -> setGameMode(ctx, "creative", EntityArgument.getPlayer(ctx, "player")))
            )
        );
        
        // /gma - Adventure mode
        dispatcher.register(Commands.literal("gma")
            .requires(source -> source.hasPermission(2))
            .executes(ctx -> setGameMode(ctx, "adventure", null))
            .then(Commands.argument("player", EntityArgument.player())
                .executes(ctx -> setGameMode(ctx, "adventure", EntityArgument.getPlayer(ctx, "player")))
            )
        );
        
        // /gmsp - Spectator mode
        dispatcher.register(Commands.literal("gmsp")
            .requires(source -> source.hasPermission(2))
            .executes(ctx -> setGameMode(ctx, "spectator", null))
            .then(Commands.argument("player", EntityArgument.player())
                .executes(ctx -> setGameMode(ctx, "spectator", EntityArgument.getPlayer(ctx, "player")))
            )
        );
    }
    
    private static int setGameMode(CommandContext<CommandSourceStack> context, String modeStr, ServerPlayer targetPlayer) throws CommandSyntaxException {
        ServerPlayer player = targetPlayer != null ? targetPlayer : context.getSource().getPlayerOrException();
        GameType gameType = parseGameMode(modeStr);
        
        if (gameType == null) {
            context.getSource().sendFailure(Component.literal("§cInvalid game mode: " + modeStr));
            return 0;
        }
        
        if (player.gameMode.getGameModeForPlayer() == gameType) {
            if (targetPlayer != null && targetPlayer != context.getSource().getPlayerOrException()) {
                context.getSource().sendFailure(Component.literal("§c" + player.getName().getString() + " is already in " + gameType.getName() + " mode!"));
            } else {
                context.getSource().sendFailure(Component.literal("§cYou are already in " + gameType.getName() + " mode!"));
            }
            return 0;
        }
        
        // Change the game mode
        player.setGameMode(gameType);
        
        if (targetPlayer != null && targetPlayer != context.getSource().getPlayerOrException()) {
            context.getSource().sendSuccess(() -> Component.literal("§aSet " + player.getName().getString() + "'s game mode to " + gameType.getName()), true);
            targetPlayer.sendSystemMessage(Component.literal("§aYour game mode has been changed to " + gameType.getName()));
        } else {
            context.getSource().sendSuccess(() -> Component.literal("§aSet your game mode to " + gameType.getName()), true);
        }
        
        return 1;
    }
    
    private static GameType parseGameMode(String mode) {
        return switch (mode.toLowerCase()) {
            case "survival", "s", "0" -> GameType.SURVIVAL;
            case "creative", "c", "1" -> GameType.CREATIVE;
            case "adventure", "a", "2" -> GameType.ADVENTURE;
            case "spectator", "sp", "3" -> GameType.SPECTATOR;
            default -> null;
        };
    }
}
