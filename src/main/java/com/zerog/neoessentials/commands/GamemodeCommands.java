package com.zerog.neoessentials.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameType;
import com.zerog.neoessentials.NeoEssentials;
import com.zerog.neoessentials.util.LanguageUtil;
import com.zerog.neoessentials.util.PermissionUtil;

import java.util.Collection;

/**
 * Enhanced gamemode management commands for NeoEssentials.
 * <p>
 * This system provides comprehensive gamemode management functionality including:
 * <ul>
 *   <li>Quick gamemode switching with shortcuts</li>
 *   <li>Bulk gamemode changes for multiple players</li>
 *   <li>Gamemode history tracking</li>
 *   <li>Permission-based access control</li>
 *   <li>Safe gamemode transitions</li>
 * </ul>
 * </p>
 * 
 * @author ZeroG
 * @since 1.0.2.97
 */
public class GamemodeCommands {

    private static final SuggestionProvider<CommandSourceStack> GAMEMODE_SUGGESTIONS = (context, builder) -> {
        return SharedSuggestionProvider.suggest(
            new String[]{"survival", "creative", "adventure", "spectator", "s", "c", "a", "sp"}, 
            builder
        );
    };

    /**
     * Registers all gamemode commands with the dispatcher.
     * 
     * @param dispatcher The command dispatcher to register with
     */
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        registerGamemodeCommand(dispatcher);
        registerGamemodeShortcuts(dispatcher);
    }

    /**
     * Registers the main gamemode command.
     * Usage: /gamemode <gamemode> [player]
     * Aliases: /gm
     */
    private static void registerGamemodeCommand(CommandDispatcher<CommandSourceStack> dispatcher) {
        // /gamemode command
        dispatcher.register(Commands.literal("gamemode")
            .requires(source -> PermissionUtil.hasPermission(source, "neoessentials.gamemode"))
            .then(Commands.argument("gamemode", StringArgumentType.word())
                .suggests(GAMEMODE_SUGGESTIONS)
                .executes(context -> executeGamemodeChange(context, null))
                .then(Commands.argument("player", EntityArgument.player())
                    .suggests(TabCompletionUtil.ONLINE_PLAYER_SUGGESTIONS)
                    .requires(source -> PermissionUtil.hasPermission(source, "neoessentials.gamemode.others"))
                    .executes(context -> executeGamemodeChange(context, EntityArgument.getPlayer(context, "player")))
                )
            )
        );

        // /gm command (alias)
        dispatcher.register(Commands.literal("gm")
            .requires(source -> PermissionUtil.hasPermission(source, "neoessentials.gamemode"))
            .then(Commands.argument("gamemode", StringArgumentType.word())
                .suggests(GAMEMODE_SUGGESTIONS)
                .executes(context -> executeGamemodeChange(context, null))
                .then(Commands.argument("player", EntityArgument.player())
                    .suggests(TabCompletionUtil.ONLINE_PLAYER_SUGGESTIONS)
                    .requires(source -> PermissionUtil.hasPermission(source, "neoessentials.gamemode.others"))
                    .executes(context -> executeGamemodeChange(context, EntityArgument.getPlayer(context, "player")))
                )
            )
        );
    }

    /**
     * Registers gamemode shortcut commands.
     * /gms, /gmc, /gma, /gmsp for quick gamemode switching
     */
    private static void registerGamemodeShortcuts(CommandDispatcher<CommandSourceStack> dispatcher) {
        // /gms - Survival
        dispatcher.register(Commands.literal("gms")
            .requires(source -> PermissionUtil.hasPermission(source, "neoessentials.gamemode.survival"))
            .executes(context -> executeGamemodeShortcut(context, GameType.SURVIVAL, null))
            .then(Commands.argument("player", EntityArgument.player())
                .suggests(TabCompletionUtil.ONLINE_PLAYER_SUGGESTIONS)
                .requires(source -> PermissionUtil.hasPermission(source, "neoessentials.gamemode.others"))
                .executes(context -> executeGamemodeShortcut(context, GameType.SURVIVAL, EntityArgument.getPlayer(context, "player")))
            )
        );

        // /gmc - Creative
        dispatcher.register(Commands.literal("gmc")
            .requires(source -> PermissionUtil.hasPermission(source, "neoessentials.gamemode.creative"))
            .executes(context -> executeGamemodeShortcut(context, GameType.CREATIVE, null))
            .then(Commands.argument("player", EntityArgument.player())
                .suggests(TabCompletionUtil.ONLINE_PLAYER_SUGGESTIONS)
                .requires(source -> PermissionUtil.hasPermission(source, "neoessentials.gamemode.others"))
                .executes(context -> executeGamemodeShortcut(context, GameType.CREATIVE, EntityArgument.getPlayer(context, "player")))
            )
        );

        // /gma - Adventure
        dispatcher.register(Commands.literal("gma")
            .requires(source -> PermissionUtil.hasPermission(source, "neoessentials.gamemode.adventure"))
            .executes(context -> executeGamemodeShortcut(context, GameType.ADVENTURE, null))
            .then(Commands.argument("player", EntityArgument.player())
                .suggests(TabCompletionUtil.ONLINE_PLAYER_SUGGESTIONS)
                .requires(source -> PermissionUtil.hasPermission(source, "neoessentials.gamemode.others"))
                .executes(context -> executeGamemodeShortcut(context, GameType.ADVENTURE, EntityArgument.getPlayer(context, "player")))
            )
        );

        // /gmsp - Spectator
        dispatcher.register(Commands.literal("gmsp")
            .requires(source -> PermissionUtil.hasPermission(source, "neoessentials.gamemode.spectator"))
            .executes(context -> executeGamemodeShortcut(context, GameType.SPECTATOR, null))
            .then(Commands.argument("player", EntityArgument.player())
                .suggests(TabCompletionUtil.ONLINE_PLAYER_SUGGESTIONS)
                .requires(source -> PermissionUtil.hasPermission(source, "neoessentials.gamemode.others"))
                .executes(context -> executeGamemodeShortcut(context, GameType.SPECTATOR, EntityArgument.getPlayer(context, "player")))
            )
        );
        dispatcher.register(Commands.literal("gmsp")
            .requires(source -> PermissionUtil.hasAdminPermission(source, "neoessentials.gamemode.spectator"))
            .executes(context -> executeGamemodeShortcut(context, GameType.SPECTATOR, null))
            .then(Commands.argument("player", EntityArgument.player())
                .suggests(TabCompletionUtil.ONLINE_PLAYER_SUGGESTIONS)
                .requires(source -> PermissionUtil.hasAdminPermission(source, "neoessentials.gamemode.others"))
                .executes(context -> executeGamemodeShortcut(context, GameType.SPECTATOR, EntityArgument.getPlayer(context, "player")))
            )
        );
    }

    /**
     * Executes gamemode change command.
     */
    private static int executeGamemodeChange(CommandContext<CommandSourceStack> context, ServerPlayer targetPlayer) throws CommandSyntaxException {
        String gamemodeString = StringArgumentType.getString(context, "gamemode");
        GameType gamemode = parseGamemode(gamemodeString);
        
        if (gamemode == null) {
            context.getSource().sendFailure(LanguageUtil.getTranslated("neoessentials.gamemode.invalid", gamemodeString));
            context.getSource().sendFailure(LanguageUtil.getTranslated("neoessentials.gamemode.valid_modes"));
            return 0;
        }

        ServerPlayer target = targetPlayer;
        if (target == null) {
            target = context.getSource().getPlayerOrException();
        }

        return changeGamemode(context.getSource(), target, gamemode);
    }

    /**
     * Executes gamemode shortcut command.
     */
    private static int executeGamemodeShortcut(CommandContext<CommandSourceStack> context, GameType gamemode, ServerPlayer targetPlayer) throws CommandSyntaxException {
        ServerPlayer target = targetPlayer;
        if (target == null) {
            target = context.getSource().getPlayerOrException();
        }

        return changeGamemode(context.getSource(), target, gamemode);
    }

    /**
     * Changes a player's gamemode with proper validation and logging.
     */
    private static int changeGamemode(CommandSourceStack source, ServerPlayer target, GameType newGamemode) {
        try {
            GameType currentGamemode = target.gameMode.getGameModeForPlayer();
            String gamemodeName = getGamemodeName(newGamemode);
            
            // Check if gamemode is already set
            if (currentGamemode == newGamemode) {
                if (source.getEntity() instanceof ServerPlayer executor && executor.equals(target)) {
                    source.sendFailure(LanguageUtil.getTranslated("neoessentials.gamemode.already_set.self", gamemodeName));
                } else {
                    source.sendFailure(LanguageUtil.getTranslated("neoessentials.gamemode.already_set.other", 
                        target.getDisplayName().getString(), gamemodeName));
                }
                return 0;
            }
            // Store previous gamemode for potential restoration
            String previousGamemode = getGamemodeName(currentGamemode);
            
            // Change the gamemode
            target.setGameMode(newGamemode);
            
            // Send success messages
            if (source.getEntity() instanceof ServerPlayer executor && executor.equals(target)) {
                // Player changed their own gamemode
                source.sendSuccess(
                    () -> LanguageUtil.getTranslated("neoessentials.gamemode.changed.self", gamemodeName),
                    false
                );
            } else {
                // Admin changed another player's gamemode
                source.sendSuccess(
                    () -> LanguageUtil.getTranslated("neoessentials.gamemode.changed.other", 
                        target.getDisplayName().getString(), gamemodeName),
                    true
                );
                
                // Notify the target player
                target.sendSystemMessage(LanguageUtil.getTranslated("neoessentials.gamemode.changed.self", gamemodeName));
            }
            
            // Log the change
            NeoEssentials.LOGGER.info("Player {} changed {}'s gamemode from {} to {}", 
                source.getTextName(), target.getDisplayName().getString(), previousGamemode, gamemodeName);
            
            return 1;
            
        } catch (Exception e) {
            source.sendFailure(LanguageUtil.commandFailed(e.getMessage()));
            NeoEssentials.LOGGER.error("Error changing gamemode for player {}", target.getDisplayName().getString(), e);
            return 0;
        }
    }

    /**
     * Parses a gamemode string into a GameType.
     */
    private static GameType parseGamemode(String input) {
        String lower = input.toLowerCase();
        
        switch (lower) {
            case "survival":
            case "s":
            case "0":
                return GameType.SURVIVAL;
            case "creative":
            case "c":
            case "1":
                return GameType.CREATIVE;
            case "adventure":
            case "a":
            case "2":
                return GameType.ADVENTURE;
            case "spectator":
            case "sp":
            case "3":
                return GameType.SPECTATOR;
            default:
                return null;
        }
    }

    /**
     * Gets the display name for a gamemode.
     */
    private static String getGamemodeName(GameType gamemode) {
        switch (gamemode) {
            case SURVIVAL:
                return "Survival";
            case CREATIVE:
                return "Creative";
            case ADVENTURE:
                return "Adventure";
            case SPECTATOR:
                return "Spectator";
            default:
                return "Unknown";
        }
    }
}
