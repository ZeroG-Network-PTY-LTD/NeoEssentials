package com.zerog.neoessentials.commands;

import com.mojang.brigadier.CommandDispatcher;
<<<<<<< HEAD
<<<<<<< HEAD
<<<<<<< HEAD
=======
>>>>>>> 4fee73b0b24b6301947b09da0d1e52696e353f1d
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.zerog.neoessentials.NeoEssentials;
<<<<<<< HEAD
import com.zerog.neoessentials.utils.MessageUtil;
import com.zerog.neoessentials.utils.PermissionUtil;
import com.zerog.neoessentials.utils.VanillaBooleanParser;
=======
import com.mojang.brigadier.arguments.BooleanArgumentType;
=======
>>>>>>> a0123aa (refactor: Enhance message command handling and introduce StringToBooleanArgumentType for improved command argument parsing)
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.zerog.neoessentials.NeoEssentials;
import com.zerog.neoessentials.utils.MessageUtil;
import com.zerog.neoessentials.utils.PermissionUtil;
<<<<<<< HEAD
<<<<<<< HEAD
import com.zerog.neoessentials.utils.PlayerUtil;
>>>>>>> bac244b (Implement messaging and player state commands)
=======
import com.zerog.neoessentials.utils.StringToBooleanArgumentType;
>>>>>>> a0123aa (refactor: Enhance message command handling and introduce StringToBooleanArgumentType for improved command argument parsing)
=======
import com.zerog.neoessentials.utils.VanillaBooleanParser;
>>>>>>> c8bd7e4 (feat: Replace custom string-to-boolean argument type with vanilla-compatible implementation and update command handling)
=======
import com.zerog.neoessentials.util.LanguageUtil;
import com.zerog.neoessentials.util.PermissionUtil;
import com.zerog.neoessentials.utils.VanillaBooleanParser;
>>>>>>> 4fee73b0b24b6301947b09da0d1e52696e353f1d
import com.zerog.neoessentials.utils.TextUtil;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
<<<<<<< HEAD
<<<<<<< HEAD
<<<<<<< HEAD
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
=======
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
>>>>>>> bac244b (Implement messaging and player state commands)
=======
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
>>>>>>> 4b34e9e (Enhance player commands with improved functionality and suggestions)
=======
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
>>>>>>> 4fee73b0b24b6301947b09da0d1e52696e353f1d
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.food.FoodData;
<<<<<<< HEAD
<<<<<<< HEAD
<<<<<<< HEAD
=======
>>>>>>> 4fee73b0b24b6301947b09da0d1e52696e353f1d

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
<<<<<<< HEAD
=======
import net.minecraft.world.phys.Vec3;
=======
>>>>>>> 4b34e9e (Enhance player commands with improved functionality and suggestions)

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
<<<<<<< HEAD
import java.util.Map;
>>>>>>> bac244b (Implement messaging and player state commands)
=======
import java.util.List;
>>>>>>> 4b34e9e (Enhance player commands with improved functionality and suggestions)
=======
>>>>>>> 4fee73b0b24b6301947b09da0d1e52696e353f1d
import java.util.Set;
import java.util.UUID;

/**
 * Implements player state commands like /heal, /feed, /fly, /god, /speed, etc.
<<<<<<< HEAD
<<<<<<< HEAD
<<<<<<< HEAD
 * These commands allow admins to manage player state and abilities.
=======
>>>>>>> bac244b (Implement messaging and player state commands)
=======
 * These commands allow admins to manage player state and abilities.
>>>>>>> 4b34e9e (Enhance player commands with improved functionality and suggestions)
=======
 * These commands allow admins to manage player state and abilities.
>>>>>>> 4fee73b0b24b6301947b09da0d1e52696e353f1d
 */
public class PlayerCommands {
    
    // Tracking for god mode
    private final Set<UUID> godModePlayers = new HashSet<>();
    
    // Tracking for fly mode (not in creative/spectator)
    private final Set<UUID> flyModePlayers = new HashSet<>();
    
<<<<<<< HEAD
<<<<<<< HEAD
<<<<<<< HEAD
=======
>>>>>>> 4b34e9e (Enhance player commands with improved functionality and suggestions)
=======
>>>>>>> 4fee73b0b24b6301947b09da0d1e52696e353f1d
    // Speed types for suggestion provider
    private static final List<String> SPEED_TYPES = Arrays.asList("walk", "fly", "both");
    
    // Suggestion provider for speed types
    private static final SuggestionProvider<CommandSourceStack> SPEED_TYPE_SUGGESTIONS =
            (context, builder) -> SharedSuggestionProvider.suggest(SPEED_TYPES, builder);
    
<<<<<<< HEAD
<<<<<<< HEAD
=======
>>>>>>> bac244b (Implement messaging and player state commands)
=======
>>>>>>> 4b34e9e (Enhance player commands with improved functionality and suggestions)
=======
>>>>>>> 4fee73b0b24b6301947b09da0d1e52696e353f1d
    /**
     * Registers all player-related commands
     * 
     * @param dispatcher The command dispatcher
     */
    public void register(CommandDispatcher<CommandSourceStack> dispatcher) {
<<<<<<< HEAD
        // /heal [player]
        dispatcher.register(Commands.literal("heal")
<<<<<<< HEAD
<<<<<<< HEAD
            .requires(source -> PermissionUtil.hasPermission(source, "essentials.heal"))
            .executes(context -> healCommand(context, context.getSource().getPlayerOrException()))
            .then(Commands.argument("player", EntityArgument.players())
                .requires(source -> PermissionUtil.hasPermission(source, "essentials.heal.others"))
=======
            .requires(source -> CommandManager.hasPermission(source, "essentials.heal"))
            .executes(context -> healCommand(context, context.getSource().getPlayerOrException()))
            .then(Commands.argument("player", EntityArgument.players())
                .requires(source -> CommandManager.hasPermission(source, "essentials.heal.others"))
>>>>>>> bac244b (Implement messaging and player state commands)
=======
            .requires(source -> PermissionUtil.hasPermission(source, "essentials.heal"))
            .executes(context -> healCommand(context, context.getSource().getPlayerOrException()))
            .then(Commands.argument("player", EntityArgument.players())
                .requires(source -> PermissionUtil.hasPermission(source, "essentials.heal.others"))
>>>>>>> 4b34e9e (Enhance player commands with improved functionality and suggestions)
=======
        // /heal [player] - Admin command
        dispatcher.register(Commands.literal("heal")
            .requires(source -> PermissionUtil.hasAdminPermission(source, "neoessentials.command.heal"))
            .executes(context -> healCommand(context, context.getSource().getPlayerOrException()))
            .then(Commands.argument("player", EntityArgument.players())
                .requires(source -> PermissionUtil.hasAdminPermission(source, "neoessentials.command.heal.others"))
>>>>>>> 4fee73b0b24b6301947b09da0d1e52696e353f1d
                .executes(context -> {
                    Collection<ServerPlayer> players = EntityArgument.getPlayers(context, "player");
                    int count = 0;
                    for (ServerPlayer player : players) {
                        count += healCommand(context, player);
                    }
                    return count;
                })
            )
        );
        
<<<<<<< HEAD
        // /feed [player]
        dispatcher.register(Commands.literal("feed")
<<<<<<< HEAD
<<<<<<< HEAD
            .requires(source -> PermissionUtil.hasPermission(source, "essentials.feed"))
            .executes(context -> feedCommand(context, context.getSource().getPlayerOrException()))
            .then(Commands.argument("player", EntityArgument.players())
                .requires(source -> PermissionUtil.hasPermission(source, "essentials.feed.others"))
=======
            .requires(source -> CommandManager.hasPermission(source, "essentials.feed"))
            .executes(context -> feedCommand(context, context.getSource().getPlayerOrException()))
            .then(Commands.argument("player", EntityArgument.players())
                .requires(source -> CommandManager.hasPermission(source, "essentials.feed.others"))
>>>>>>> bac244b (Implement messaging and player state commands)
=======
            .requires(source -> PermissionUtil.hasPermission(source, "essentials.feed"))
            .executes(context -> feedCommand(context, context.getSource().getPlayerOrException()))
            .then(Commands.argument("player", EntityArgument.players())
                .requires(source -> PermissionUtil.hasPermission(source, "essentials.feed.others"))
>>>>>>> 4b34e9e (Enhance player commands with improved functionality and suggestions)
=======
        // /feed [player] - Admin command
        dispatcher.register(Commands.literal("feed")
            .requires(source -> PermissionUtil.hasAdminPermission(source, "neoessentials.command.feed"))
            .executes(context -> feedCommand(context, context.getSource().getPlayerOrException()))
            .then(Commands.argument("player", EntityArgument.players())
                .requires(source -> PermissionUtil.hasAdminPermission(source, "neoessentials.command.feed.others"))
>>>>>>> 4fee73b0b24b6301947b09da0d1e52696e353f1d
                .executes(context -> {
                    Collection<ServerPlayer> players = EntityArgument.getPlayers(context, "player");
                    int count = 0;
                    for (ServerPlayer player : players) {
                        count += feedCommand(context, player);
                    }
                    return count;
                })
            )
<<<<<<< HEAD
<<<<<<< HEAD
<<<<<<< HEAD
        );
<<<<<<< HEAD
        
<<<<<<< HEAD
=======
        );
        
>>>>>>> 4b34e9e (Enhance player commands with improved functionality and suggestions)
        // /fly [player] [on|off]
        dispatcher.register(Commands.literal("fly")
            .requires(source -> PermissionUtil.hasPermission(source, "essentials.fly"))
=======
        );
        
        // /fly [player] [on|off] - Admin command
        dispatcher.register(Commands.literal("fly")
            .requires(source -> PermissionUtil.hasAdminPermission(source, "neoessentials.command.fly"))
>>>>>>> 4fee73b0b24b6301947b09da0d1e52696e353f1d
            .executes(context -> flyCommand(context, context.getSource().getPlayerOrException(), null))
            .then(Commands.argument("enabled", VanillaBooleanParser.argument())
                .suggests(VanillaBooleanParser.booleanSuggestions())
                .executes(context -> flyCommand(
                    context, 
                    context.getSource().getPlayerOrException(), 
                    VanillaBooleanParser.getBoolean(context, "enabled")
                ))
            )
            .then(Commands.argument("player", EntityArgument.player())
                .requires(source -> PermissionUtil.hasPermission(source, "essentials.fly.others"))
                .executes(context -> flyCommand(
                    context, 
                    EntityArgument.getPlayer(context, "player"), 
                    null
                ))
                .then(Commands.argument("enabled", VanillaBooleanParser.argument())
                    .suggests(VanillaBooleanParser.booleanSuggestions())
                    .executes(context -> flyCommand(
                        context, 
                        EntityArgument.getPlayer(context, "player"), 
                        VanillaBooleanParser.getBoolean(context, "enabled")
                    ))
                )
            )
        );
        
        // /speed <speed> [type] [player]
        dispatcher.register(Commands.literal("speed")
            .requires(source -> PermissionUtil.hasPermission(source, "essentials.speed"))
            .then(Commands.argument("speed", FloatArgumentType.floatArg(0, 10))
                .executes(context -> speedCommand(
                    context, 
                    context.getSource().getPlayerOrException(), 
                    FloatArgumentType.getFloat(context, "speed"), 
                    "both"
                ))
                .then(Commands.argument("type", StringArgumentType.word())
                    .suggests(SPEED_TYPE_SUGGESTIONS)
                    .executes(context -> speedCommand(
                        context, 
                        context.getSource().getPlayerOrException(), 
                        FloatArgumentType.getFloat(context, "speed"), 
                        StringArgumentType.getString(context, "type")
                    ))
                    .then(Commands.argument("player", EntityArgument.player())
                        .requires(source -> PermissionUtil.hasPermission(source, "essentials.speed.others"))
                        .executes(context -> speedCommand(
                            context, 
                            EntityArgument.getPlayer(context, "player"), 
                            FloatArgumentType.getFloat(context, "speed"), 
                            StringArgumentType.getString(context, "type")
                        ))
                    )
                )
            )
        );
        
        // /god [player] [on|off]
<<<<<<< HEAD
<<<<<<< HEAD
=======
>>>>>>> 4fee73b0b24b6301947b09da0d1e52696e353f1d
        dispatcher.register(Commands.literal("god")
            .requires(source -> PermissionUtil.hasPermission(source, "essentials.god"))
            .executes(context -> godCommand(context, context.getSource().getPlayerOrException(), null))
            .then(Commands.argument("enabled", VanillaBooleanParser.argument())
                .suggests(VanillaBooleanParser.booleanSuggestions())
                .executes(context -> godCommand(context, context.getSource().getPlayerOrException(), 
                                              VanillaBooleanParser.getBoolean(context, "enabled")))
                .then(Commands.argument("player", EntityArgument.player())
                    .requires(source -> PermissionUtil.hasPermission(source, "essentials.god.others"))
                    .executes(context -> godCommand(
                        context, 
                        EntityArgument.getPlayer(context, "player"), 
                        VanillaBooleanParser.getBoolean(context, "enabled")
<<<<<<< HEAD
=======
        // /god [player] [on|off]
=======
          // /god [player] [on|off]
>>>>>>> 72db75e (refactor: Clean up god command registration and remove unnecessary argument type registration)
=======
        );        // /god [player] [on|off]
>>>>>>> c8bd7e4 (feat: Replace custom string-to-boolean argument type with vanilla-compatible implementation and update command handling)
=======
>>>>>>> 4b34e9e (Enhance player commands with improved functionality and suggestions)
        dispatcher.register(Commands.literal("god")
            .requires(source -> PermissionUtil.hasPermission(source, "essentials.god"))
            .executes(context -> godCommand(context, context.getSource().getPlayerOrException(), null))
            .then(Commands.argument("enabled", VanillaBooleanParser.argument())
                .suggests(VanillaBooleanParser.booleanSuggestions())
                .executes(context -> godCommand(context, context.getSource().getPlayerOrException(), 
                                              VanillaBooleanParser.getBoolean(context, "enabled")))
                .then(Commands.argument("player", EntityArgument.player())
                    .requires(source -> PermissionUtil.hasPermission(source, "essentials.god.others"))
                    .executes(context -> godCommand(
                        context, 
                        EntityArgument.getPlayer(context, "player"), 
<<<<<<< HEAD
<<<<<<< HEAD
                        BooleanArgumentType.getBool(context, "enabled")
>>>>>>> bac244b (Implement messaging and player state commands)
=======
                        StringToBooleanArgumentType.getBoolean(context, "enabled")
>>>>>>> a0123aa (refactor: Enhance message command handling and introduce StringToBooleanArgumentType for improved command argument parsing)
=======
                        VanillaBooleanParser.getBoolean(context, "enabled")
>>>>>>> c8bd7e4 (feat: Replace custom string-to-boolean argument type with vanilla-compatible implementation and update command handling)
=======
>>>>>>> 4fee73b0b24b6301947b09da0d1e52696e353f1d
                    ))
                )
            )
            .then(Commands.argument("player", EntityArgument.player())
<<<<<<< HEAD
<<<<<<< HEAD
<<<<<<< HEAD
                .requires(source -> PermissionUtil.hasPermission(source, "essentials.god.others"))
=======
                .requires(source -> CommandManager.hasPermission(source, "essentials.god.others"))
>>>>>>> bac244b (Implement messaging and player state commands)
=======
                .requires(source -> PermissionUtil.hasPermission(source, "essentials.god.others"))
>>>>>>> 4b34e9e (Enhance player commands with improved functionality and suggestions)
=======
                .requires(source -> PermissionUtil.hasPermission(source, "essentials.god.others"))
>>>>>>> 4fee73b0b24b6301947b09da0d1e52696e353f1d
                .executes(context -> godCommand(
                    context, 
                    EntityArgument.getPlayer(context, "player"),
                    null
                ))
            )
        );
        
<<<<<<< HEAD
<<<<<<< HEAD
<<<<<<< HEAD
=======
>>>>>>> 4b34e9e (Enhance player commands with improved functionality and suggestions)
=======
>>>>>>> 4fee73b0b24b6301947b09da0d1e52696e353f1d
        // Add aliases
        dispatcher.register(Commands.literal("godmode")
            .requires(source -> PermissionUtil.hasPermission(source, "essentials.god"))
            .executes(context -> godCommand(context, context.getSource().getPlayerOrException(), null))
<<<<<<< HEAD
<<<<<<< HEAD
=======
>>>>>>> 4fee73b0b24b6301947b09da0d1e52696e353f1d
        );
        
        NeoEssentials.LOGGER.info("Registered enhanced player commands");
    }
    
    // Health-related command implementations
    
    /**
     * Heal a player to full health, extinguish fire, and clear negative effects
     * 
     * @param context The command context
     * @param player The player to heal
     * @return 1 if successful, 0 otherwise
     */
    private int healCommand(CommandContext<CommandSourceStack> context, ServerPlayer player) {
        if (player == null) return 0;
        
        // Set player to full health
        player.setHealth(player.getMaxHealth());
        
        // Extinguish fire if burning
        player.clearFire();
        
        // Remove negative effects
        player.removeEffect(MobEffects.POISON);
        player.removeEffect(MobEffects.WITHER);
        player.removeEffect(MobEffects.WEAKNESS);
        player.removeEffect(MobEffects.MOVEMENT_SLOWDOWN);
        player.removeEffect(MobEffects.BLINDNESS);
        player.removeEffect(MobEffects.CONFUSION);
        player.removeEffect(MobEffects.HARM);
        
        // Send messages
        if (player == context.getSource().getEntity()) {
<<<<<<< HEAD
            player.sendSystemMessage(Component.literal(TextUtil.colorize("&aYou have been healed to full health.")));
            context.getSource().sendSuccess(() -> Component.literal(TextUtil.colorize("&aYou have been healed to full health.")), false);
        } else {
            player.sendSystemMessage(Component.literal(TextUtil.colorize("&aYou have been healed by " + context.getSource().getTextName() + ".")));
            context.getSource().sendSuccess(() -> Component.literal(TextUtil.colorize("&aYou healed &e" + player.getScoreboardName() + "&a to full health.")), true);
=======
        // /fly [player] [on|off]
        dispatcher.register(Commands.literal("fly")
            .requires(source -> CommandManager.hasPermission(source, "essentials.fly"))
            .executes(context -> flyCommand(context, context.getSource().getPlayerOrException(), null))
            .then(Commands.argument("enabled", VanillaBooleanParser.argument())
                .suggests(VanillaBooleanParser.booleanSuggestions())                .executes(context -> flyCommand(context, context.getSource().getPlayerOrException(), 
                                              VanillaBooleanParser.getBoolean(context, "enabled")))
                .then(Commands.argument("player", EntityArgument.player())
                    .requires(source -> CommandManager.hasPermission(source, "essentials.fly.others"))
                    .executes(context -> flyCommand(
                        context, 
                        EntityArgument.getPlayer(context, "player"), 
                        VanillaBooleanParser.getBoolean(context, "enabled")
                    ))
                )
            )
            .then(Commands.argument("player", EntityArgument.player())
                .requires(source -> CommandManager.hasPermission(source, "essentials.fly.others"))
                .executes(context -> flyCommand(
                    context, 
                    EntityArgument.getPlayer(context, "player"),
                    null
                ))
            )
=======
>>>>>>> 4b34e9e (Enhance player commands with improved functionality and suggestions)
        );
        
        NeoEssentials.LOGGER.info("Registered enhanced player commands");
    }
    
    // Health-related command implementations
    
    /**
     * Heal a player to full health, extinguish fire, and clear negative effects
     * 
     * @param context The command context
     * @param player The player to heal
     * @return 1 if successful, 0 otherwise
     */
    private int healCommand(CommandContext<CommandSourceStack> context, ServerPlayer player) {
        if (player == null) return 0;
        
        // Set player to full health
        player.setHealth(player.getMaxHealth());
        
        // Extinguish fire if burning
        player.clearFire();
        
        // Remove negative effects
        player.removeEffect(MobEffects.POISON);
        player.removeEffect(MobEffects.WITHER);
        player.removeEffect(MobEffects.WEAKNESS);
        player.removeEffect(MobEffects.MOVEMENT_SLOWDOWN);
        player.removeEffect(MobEffects.BLINDNESS);
        player.removeEffect(MobEffects.CONFUSION);
        player.removeEffect(MobEffects.HARM);
        
        // Send messages
        if (player == context.getSource().getEntity()) {
            player.sendSystemMessage(Component.literal(TextUtil.colorize("&aYou have been healed to full health.")));
            context.getSource().sendSuccess(() -> Component.literal(TextUtil.colorize("&aYou have been healed to full health.")), false);
        } else {
<<<<<<< HEAD
            context.getSource().sendSuccess(() -> Component.translatable("commands.heal.success.other", player.getDisplayName()), true);
            player.sendSystemMessage(Component.translatable("commands.heal.healed"));
>>>>>>> bac244b (Implement messaging and player state commands)
=======
            player.sendSystemMessage(Component.literal(TextUtil.colorize("&aYou have been healed by " + context.getSource().getTextName() + ".")));
            context.getSource().sendSuccess(() -> Component.literal(TextUtil.colorize("&aYou healed &e" + player.getScoreboardName() + "&a to full health.")), true);
>>>>>>> 4b34e9e (Enhance player commands with improved functionality and suggestions)
=======
            LanguageUtil.sendMessage(player, "neoessentials.player.healed");
        } else {
            LanguageUtil.sendMessage(player, "neoessentials.player.healed");
            LanguageUtil.sendMessage(context.getSource(), "neoessentials.player.healed_other", player.getScoreboardName());
>>>>>>> 4fee73b0b24b6301947b09da0d1e52696e353f1d
        }
        
        return 1;
    }
    
    /**
<<<<<<< HEAD
<<<<<<< HEAD
<<<<<<< HEAD
=======
>>>>>>> 4b34e9e (Enhance player commands with improved functionality and suggestions)
=======
>>>>>>> 4fee73b0b24b6301947b09da0d1e52696e353f1d
     * Feed a player to full hunger and saturation
     * 
     * @param context The command context
     * @param player The player to feed
     * @return 1 if successful, 0 otherwise
<<<<<<< HEAD
<<<<<<< HEAD
=======
>>>>>>> 4fee73b0b24b6301947b09da0d1e52696e353f1d
     */
    private int feedCommand(CommandContext<CommandSourceStack> context, ServerPlayer player) {
        if (player == null) return 0;
        
        // Set player to full food
        FoodData foodData = player.getFoodData();
        foodData.setFoodLevel(20); // Maximum food level
        foodData.setSaturation(20f); // Maximum saturation
        foodData.setExhaustion(0f); // Clear exhaustion
        
        // Send messages
        if (player == context.getSource().getEntity()) {
<<<<<<< HEAD
            player.sendSystemMessage(Component.literal(TextUtil.colorize("&aYour hunger has been satisfied.")));
            context.getSource().sendSuccess(() -> Component.literal(TextUtil.colorize("&aYour hunger has been satisfied.")), false);
        } else {
            player.sendSystemMessage(Component.literal(TextUtil.colorize("&aYour hunger has been satisfied by " + context.getSource().getTextName() + ".")));
            context.getSource().sendSuccess(() -> Component.literal(TextUtil.colorize("&aYou satisfied the hunger of &e" + player.getScoreboardName() + "&a.")), true);
=======
     * Feeds a player to max food and saturation
=======
>>>>>>> 4b34e9e (Enhance player commands with improved functionality and suggestions)
     */
    private int feedCommand(CommandContext<CommandSourceStack> context, ServerPlayer player) {
        if (player == null) return 0;
        
        // Set player to full food
        FoodData foodData = player.getFoodData();
        foodData.setFoodLevel(20); // Maximum food level
        foodData.setSaturation(20f); // Maximum saturation
        foodData.setExhaustion(0f); // Clear exhaustion
        
        // Send messages
        if (player == context.getSource().getEntity()) {
            player.sendSystemMessage(Component.literal(TextUtil.colorize("&aYour hunger has been satisfied.")));
            context.getSource().sendSuccess(() -> Component.literal(TextUtil.colorize("&aYour hunger has been satisfied.")), false);
        } else {
<<<<<<< HEAD
            context.getSource().sendSuccess(() -> Component.translatable("commands.feed.success.other", player.getDisplayName()), true);
            player.sendSystemMessage(Component.translatable("commands.feed.fed"));
>>>>>>> bac244b (Implement messaging and player state commands)
=======
            player.sendSystemMessage(Component.literal(TextUtil.colorize("&aYour hunger has been satisfied by " + context.getSource().getTextName() + ".")));
            context.getSource().sendSuccess(() -> Component.literal(TextUtil.colorize("&aYou satisfied the hunger of &e" + player.getScoreboardName() + "&a.")), true);
>>>>>>> 4b34e9e (Enhance player commands with improved functionality and suggestions)
=======
            LanguageUtil.sendMessage(player, "neoessentials.player.fed");
        } else {
            LanguageUtil.sendMessage(player, "neoessentials.player.fed");
            LanguageUtil.sendMessage(context.getSource(), "neoessentials.player.fed_other", player.getScoreboardName());
>>>>>>> 4fee73b0b24b6301947b09da0d1e52696e353f1d
        }
        
        return 1;
    }
    
<<<<<<< HEAD
<<<<<<< HEAD
<<<<<<< HEAD
=======
>>>>>>> 4fee73b0b24b6301947b09da0d1e52696e353f1d
    // Movement-related command implementations
    
    /**
     * Toggle or set flight mode for a player
     * 
     * @param context The command context
     * @param player The player to modify
     * @param enabled True to enable, false to disable, null to toggle
     * @return 1 if successful, 0 otherwise
     */
    private int flyCommand(CommandContext<CommandSourceStack> context, ServerPlayer player, Boolean enabled) {
        if (player == null) return 0;
        
        // Toggle flight if no value is provided
        if (enabled == null) {
            enabled = !player.getAbilities().mayfly;
        }
        
        // Set flight abilities
        player.getAbilities().mayfly = enabled;
        
        // If flight is disabled, make sure the player isn't flying
        if (!enabled && player.getAbilities().flying) {
            player.getAbilities().flying = false;
        }
        
        // Track flight status for non-creative players
        if (enabled && !player.isCreative() && !player.isSpectator()) {
            flyModePlayers.add(player.getUUID());
        } else {
            flyModePlayers.remove(player.getUUID());
        }
        
        // Sync abilities to client
        player.onUpdateAbilities();
        
        // Send messages
<<<<<<< HEAD
        String statusText = enabled ? "&aenabled" : "&cdisabled";
        
        if (player == context.getSource().getEntity()) {
            context.getSource().sendSuccess(() -> Component.literal(TextUtil.colorize("&7Flight mode " + statusText + "&7 for yourself.")), true);
        } else {
            player.sendSystemMessage(Component.literal(TextUtil.colorize("&7Your flight mode has been " + statusText + " &7by " + context.getSource().getTextName() + ".")));
            context.getSource().sendSuccess(() -> Component.literal(TextUtil.colorize("&7Flight mode " + statusText + " &7for &e" + player.getScoreboardName() + "&7.")), true);
=======
    /**
     * Toggles god mode for a player
     */
    private int godCommand(CommandContext<CommandSourceStack> context, ServerPlayer player, Boolean enabled) {
        UUID playerUuid = player.getUUID();
        boolean isGodMode = godModePlayers.contains(playerUuid);
        
        // If enabled is specified, set to that value, otherwise toggle
        boolean newGodMode = (enabled != null) ? enabled : !isGodMode;
        
        if (newGodMode) {
            godModePlayers.add(playerUuid);
        } else {
            godModePlayers.remove(playerUuid);
        }
        
        // Send confirmation message
        if (player == context.getSource().getEntity()) {
            if (newGodMode) {
                context.getSource().sendSuccess(() -> Component.translatable("commands.god.enabled.self"), true);
            } else {
                context.getSource().sendSuccess(() -> Component.translatable("commands.god.disabled.self"), true);
            }
        } else {
            if (newGodMode) {
                context.getSource().sendSuccess(() -> Component.translatable("commands.god.enabled.other", player.getDisplayName()), true);
                player.sendSystemMessage(Component.translatable("commands.god.enabled.by", context.getSource().getDisplayName()));
            } else {
                context.getSource().sendSuccess(() -> Component.translatable("commands.god.disabled.other", player.getDisplayName()), true);
                player.sendSystemMessage(Component.translatable("commands.god.disabled.by", context.getSource().getDisplayName()));
            }
        }
        
        return 1;
    }
=======
    // Movement-related command implementations
>>>>>>> 4b34e9e (Enhance player commands with improved functionality and suggestions)
    
    /**
     * Toggle or set flight mode for a player
     * 
     * @param context The command context
     * @param player The player to modify
     * @param enabled True to enable, false to disable, null to toggle
     * @return 1 if successful, 0 otherwise
     */
    private int flyCommand(CommandContext<CommandSourceStack> context, ServerPlayer player, Boolean enabled) {
        if (player == null) return 0;
        
        // Toggle flight if no value is provided
        if (enabled == null) {
            enabled = !player.getAbilities().mayfly;
        }
        
        // Set flight abilities
        player.getAbilities().mayfly = enabled;
        
        // If flight is disabled, make sure the player isn't flying
        if (!enabled && player.getAbilities().flying) {
            player.getAbilities().flying = false;
        }
        
        // Track flight status for non-creative players
        if (enabled && !player.isCreative() && !player.isSpectator()) {
            flyModePlayers.add(player.getUUID());
        } else {
            flyModePlayers.remove(player.getUUID());
        }
        
        // Sync abilities to client
        player.onUpdateAbilities();
        
        // Send messages
        String statusText = enabled ? "&aenabled" : "&cdisabled";
        
        if (player == context.getSource().getEntity()) {
            context.getSource().sendSuccess(() -> Component.literal(TextUtil.colorize("&7Flight mode " + statusText + "&7 for yourself.")), true);
        } else {
<<<<<<< HEAD
            if (shouldFly) {
                context.getSource().sendSuccess(() -> Component.translatable("commands.fly.enabled.other", player.getDisplayName()), true);
                player.sendSystemMessage(Component.translatable("commands.fly.enabled.by", context.getSource().getDisplayName()));
            } else {
                context.getSource().sendSuccess(() -> Component.translatable("commands.fly.disabled.other", player.getDisplayName()), true);
                player.sendSystemMessage(Component.translatable("commands.fly.disabled.by", context.getSource().getDisplayName()));
            }
>>>>>>> bac244b (Implement messaging and player state commands)
=======
            player.sendSystemMessage(Component.literal(TextUtil.colorize("&7Your flight mode has been " + statusText + " &7by " + context.getSource().getTextName() + ".")));
            context.getSource().sendSuccess(() -> Component.literal(TextUtil.colorize("&7Flight mode " + statusText + " &7for &e" + player.getScoreboardName() + "&7.")), true);
>>>>>>> 4b34e9e (Enhance player commands with improved functionality and suggestions)
=======
        String statusKey = enabled ? "neoessentials.player.fly_enabled" : "neoessentials.player.fly_disabled";
        
        if (player == context.getSource().getEntity()) {
            LanguageUtil.sendMessage(context.getSource(), statusKey);
        } else {
            LanguageUtil.sendMessage(player, statusKey + "_other", context.getSource().getTextName());
            LanguageUtil.sendMessage(context.getSource(), statusKey + "_for", player.getScoreboardName());
>>>>>>> 4fee73b0b24b6301947b09da0d1e52696e353f1d
        }
        
        return 1;
    }
    
    /**
<<<<<<< HEAD
<<<<<<< HEAD
<<<<<<< HEAD
=======
>>>>>>> 4b34e9e (Enhance player commands with improved functionality and suggestions)
=======
>>>>>>> 4fee73b0b24b6301947b09da0d1e52696e353f1d
     * Set speed (walk, fly, or both) for a player
     * 
     * @param context The command context
     * @param player The player to modify
     * @param speedValue Speed value (0-10, will be converted to appropriate Minecraft speed)
     * @param type Type of speed: "walk", "fly", or "both"
     * @return 1 if successful, 0 otherwise
<<<<<<< HEAD
<<<<<<< HEAD
=======
>>>>>>> 4fee73b0b24b6301947b09da0d1e52696e353f1d
     */
    private int speedCommand(CommandContext<CommandSourceStack> context, ServerPlayer player, float speedValue, String type) {
        if (player == null) return 0;
        
        // Clamp speed value between 0 and 10
        speedValue = Math.max(0.0f, Math.min(10.0f, speedValue));
        
        // Convert to Minecraft speed (0-10 scale to Minecraft scale)
        float walkSpeed = speedValue * 0.1f; // Convert to 0-1 range
        float flySpeed = speedValue * 0.05f; // Convert to 0-0.5 range
        
        // Set speed based on type
        boolean walkUpdated = false;
        boolean flyUpdated = false;
        
        if ("walk".equalsIgnoreCase(type) || "both".equalsIgnoreCase(type)) {
            // Can't directly access walkingSpeed, use reflection or approximation
            player.getAbilities().setWalkingSpeed(walkSpeed);
            walkUpdated = true;
        }
        
        if ("fly".equalsIgnoreCase(type) || "both".equalsIgnoreCase(type)) {
            // Can't directly access flyingSpeed, use reflection or approximation
            player.getAbilities().setFlyingSpeed(flySpeed);
            flyUpdated = true;
        }
        
        if (!walkUpdated && !flyUpdated) {
<<<<<<< HEAD
            context.getSource().sendFailure(Component.literal(TextUtil.colorize("&cInvalid speed type. Use 'walk', 'fly', or 'both'.")));
=======
            LanguageUtil.sendMessage(context.getSource(), "neoessentials.player.speed_invalid_type");
>>>>>>> 4fee73b0b24b6301947b09da0d1e52696e353f1d
            return 0;
        }
        
        // Sync abilities to client
        player.onUpdateAbilities();
        
        // Send messages
<<<<<<< HEAD
        StringBuilder speedMessage = new StringBuilder();
        
        if (walkUpdated && flyUpdated) {
            speedMessage.append("&7Both walking and flying speeds");
        } else if (walkUpdated) {
            speedMessage.append("&7Walking speed");
        } else {
            speedMessage.append("&7Flying speed");
        }
        
        speedMessage.append(" set to &e").append(speedValue).append("&7.");
        
        if (player == context.getSource().getEntity()) {
            context.getSource().sendSuccess(() -> Component.literal(TextUtil.colorize(speedMessage.toString())), false);
        } else {
            player.sendSystemMessage(Component.literal(TextUtil.colorize(speedMessage.toString() + " &7(set by " + context.getSource().getTextName() + ")")));
            context.getSource().sendSuccess(() -> Component.literal(TextUtil.colorize(speedMessage + " &7(for &e" + player.getScoreboardName() + "&7)")), true);
=======
        String speedType = "";
        if (walkUpdated && flyUpdated) {
            speedType = "both";
        } else if (walkUpdated) {
            speedType = "walk";
        } else {
            speedType = "fly";
        }
        
        if (player == context.getSource().getEntity()) {
            LanguageUtil.sendMessage(context.getSource(), "neoessentials.player.speed_set", speedType, String.valueOf(speedValue));
        } else {
            LanguageUtil.sendMessage(player, "neoessentials.player.speed_set_other", speedType, String.valueOf(speedValue), context.getSource().getTextName());
            LanguageUtil.sendMessage(context.getSource(), "neoessentials.player.speed_set_for", speedType, String.valueOf(speedValue), player.getScoreboardName());
>>>>>>> 4fee73b0b24b6301947b09da0d1e52696e353f1d
        }
        
        return 1;
    }
    
    // Player state command implementations
    
    /**
     * Toggle or set god mode for a player
     * 
     * @param context The command context
     * @param player The player to modify
     * @param enabled True to enable, false to disable, null to toggle
     * @return 1 if successful, 0 otherwise
     */
    private int godCommand(CommandContext<CommandSourceStack> context, ServerPlayer player, Boolean enabled) {
        if (player == null) return 0;
        
        UUID playerId = player.getUUID();
        
        // Toggle god mode if no value is provided
        if (enabled == null) {
            enabled = !godModePlayers.contains(playerId);
        }
        
        // Set god mode status
        if (enabled) {
            godModePlayers.add(playerId);
        } else {
            godModePlayers.remove(playerId);
        }
        
        // Send messages
<<<<<<< HEAD
        String statusText = enabled ? "&aenabled" : "&cdisabled";
        
        if (player == context.getSource().getEntity()) {
            context.getSource().sendSuccess(() -> Component.literal(TextUtil.colorize("&7God mode " + statusText + "&7 for yourself.")), false);
        } else {
            player.sendSystemMessage(Component.literal(TextUtil.colorize("&7Your god mode has been " + statusText + " &7by " + context.getSource().getTextName() + ".")));
            context.getSource().sendSuccess(() -> Component.literal(TextUtil.colorize("&7God mode " + statusText + " &7for &e" + player.getScoreboardName() + "&7.")), true);
=======
     * Sets the speed for a player
=======
>>>>>>> 4b34e9e (Enhance player commands with improved functionality and suggestions)
     */
    private int speedCommand(CommandContext<CommandSourceStack> context, ServerPlayer player, float speedValue, String type) {
        if (player == null) return 0;
        
        // Clamp speed value between 0 and 10
        speedValue = Math.max(0.0f, Math.min(10.0f, speedValue));
        
        // Convert to Minecraft speed (0-10 scale to Minecraft scale)
        float walkSpeed = speedValue * 0.1f; // Convert to 0-1 range
        float flySpeed = speedValue * 0.05f; // Convert to 0-0.5 range
        
        // Set speed based on type
        boolean walkUpdated = false;
        boolean flyUpdated = false;
        
        if ("walk".equalsIgnoreCase(type) || "both".equalsIgnoreCase(type)) {
            // Can't directly access walkingSpeed, use reflection or approximation
            player.getAbilities().setWalkingSpeed(walkSpeed);
            walkUpdated = true;
        }
        
        if ("fly".equalsIgnoreCase(type) || "both".equalsIgnoreCase(type)) {
            // Can't directly access flyingSpeed, use reflection or approximation
            player.getAbilities().setFlyingSpeed(flySpeed);
            flyUpdated = true;
        }
        
        if (!walkUpdated && !flyUpdated) {
            context.getSource().sendFailure(Component.literal(TextUtil.colorize("&cInvalid speed type. Use 'walk', 'fly', or 'both'.")));
            return 0;
        }
        
        // Sync abilities to client
        player.onUpdateAbilities();
        
        // Send messages
        StringBuilder speedMessage = new StringBuilder();
        
        if (walkUpdated && flyUpdated) {
            speedMessage.append("&7Both walking and flying speeds");
        } else if (walkUpdated) {
            speedMessage.append("&7Walking speed");
        } else {
<<<<<<< HEAD
            context.getSource().sendSuccess(() -> 
                Component.translatable("commands.speed.success.other", 
                    speedType, speed, player.getDisplayName()), true);
            player.sendSystemMessage(Component.translatable("commands.speed.set", speedType, speed));
>>>>>>> bac244b (Implement messaging and player state commands)
=======
            speedMessage.append("&7Flying speed");
        }
        
        speedMessage.append(" set to &e").append(speedValue).append("&7.");
        
        if (player == context.getSource().getEntity()) {
            context.getSource().sendSuccess(() -> Component.literal(TextUtil.colorize(speedMessage.toString())), false);
        } else {
            player.sendSystemMessage(Component.literal(TextUtil.colorize(speedMessage.toString() + " &7(set by " + context.getSource().getTextName() + ")")));
            context.getSource().sendSuccess(() -> Component.literal(TextUtil.colorize(speedMessage + " &7(for &e" + player.getScoreboardName() + "&7)")), true);
        }
        
        return 1;
    }
    
    // Player state command implementations
    
    /**
     * Toggle or set god mode for a player
     * 
     * @param context The command context
     * @param player The player to modify
     * @param enabled True to enable, false to disable, null to toggle
     * @return 1 if successful, 0 otherwise
     */
    private int godCommand(CommandContext<CommandSourceStack> context, ServerPlayer player, Boolean enabled) {
        if (player == null) return 0;
        
        UUID playerId = player.getUUID();
        
        // Toggle god mode if no value is provided
        if (enabled == null) {
            enabled = !godModePlayers.contains(playerId);
        }
        
        // Set god mode status
        if (enabled) {
            godModePlayers.add(playerId);
        } else {
            godModePlayers.remove(playerId);
        }
        
        // Send messages
        String statusText = enabled ? "&aenabled" : "&cdisabled";
        
        if (player == context.getSource().getEntity()) {
            context.getSource().sendSuccess(() -> Component.literal(TextUtil.colorize("&7God mode " + statusText + "&7 for yourself.")), false);
        } else {
            player.sendSystemMessage(Component.literal(TextUtil.colorize("&7Your god mode has been " + statusText + " &7by " + context.getSource().getTextName() + ".")));
            context.getSource().sendSuccess(() -> Component.literal(TextUtil.colorize("&7God mode " + statusText + " &7for &e" + player.getScoreboardName() + "&7.")), true);
>>>>>>> 4b34e9e (Enhance player commands with improved functionality and suggestions)
=======
        String statusKey = enabled ? "neoessentials.player.god_enabled" : "neoessentials.player.god_disabled";
        
        if (player == context.getSource().getEntity()) {
            LanguageUtil.sendMessage(context.getSource(), statusKey);
        } else {
            LanguageUtil.sendMessage(player, statusKey + "_other", context.getSource().getTextName());
            LanguageUtil.sendMessage(context.getSource(), statusKey + "_for", player.getScoreboardName());
>>>>>>> 4fee73b0b24b6301947b09da0d1e52696e353f1d
        }
        
        return 1;
    }
    
    /**
<<<<<<< HEAD
<<<<<<< HEAD
<<<<<<< HEAD
=======
>>>>>>> 4b34e9e (Enhance player commands with improved functionality and suggestions)
=======
>>>>>>> 4fee73b0b24b6301947b09da0d1e52696e353f1d
     * Check if a player has god mode enabled
     * 
     * @param player The player to check
     * @return True if the player has god mode enabled
<<<<<<< HEAD
<<<<<<< HEAD
=======
>>>>>>> 4fee73b0b24b6301947b09da0d1e52696e353f1d
     */
    public boolean isGodModeEnabled(ServerPlayer player) {
        return player != null && godModePlayers.contains(player.getUUID());
    }
    
    /**
     * Check if a player has fly mode enabled outside of creative/spectator
     * 
     * @param player The player to check
     * @return True if the player has fly mode enabled
     */
    public boolean hasFlyModeEnabled(ServerPlayer player) {
        return player != null && flyModePlayers.contains(player.getUUID());
<<<<<<< HEAD
=======
     * Extinguishes a player
=======
>>>>>>> 4b34e9e (Enhance player commands with improved functionality and suggestions)
     */
    public boolean isGodModeEnabled(ServerPlayer player) {
        return player != null && godModePlayers.contains(player.getUUID());
    }
    
    /**
     * Check if a player has fly mode enabled outside of creative/spectator
     * 
     * @param player The player to check
     * @return True if the player has fly mode enabled
     */
<<<<<<< HEAD
    public boolean hasGodMode(ServerPlayer player) {
        return godModePlayers.contains(player.getUUID());
    }
    
    /**
     * Checks if a player has custom fly mode enabled
     * This is separate from creative/spectator flight
     * 
     * @param player The player to check
     * @return True if the player has custom fly mode
     */
    public boolean hasCustomFlyMode(ServerPlayer player) {
        return flyModePlayers.contains(player.getUUID());
>>>>>>> bac244b (Implement messaging and player state commands)
=======
    public boolean hasFlyModeEnabled(ServerPlayer player) {
        return player != null && flyModePlayers.contains(player.getUUID());
>>>>>>> 4b34e9e (Enhance player commands with improved functionality and suggestions)
=======
>>>>>>> 4fee73b0b24b6301947b09da0d1e52696e353f1d
    }
}
