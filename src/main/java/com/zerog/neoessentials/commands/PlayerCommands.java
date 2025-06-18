package com.zerog.neoessentials.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.zerog.neoessentials.NeoEssentials;
import com.zerog.neoessentials.utils.PermissionUtil;
import com.zerog.neoessentials.utils.StringToBooleanArgumentType;
import com.zerog.neoessentials.utils.TextUtil;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.food.FoodData;
import net.minecraft.world.phys.Vec3;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Implements player state commands like /heal, /feed, /fly, /god, /speed, etc.
 */
public class PlayerCommands {
    
    // Tracking for god mode
    private final Set<UUID> godModePlayers = new HashSet<>();
    
    // Tracking for fly mode (not in creative/spectator)
    private final Set<UUID> flyModePlayers = new HashSet<>();
    
    /**
     * Registers all player-related commands
     * 
     * @param dispatcher The command dispatcher
     */
    public void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        // /heal [player]
        dispatcher.register(Commands.literal("heal")
            .requires(source -> CommandManager.hasPermission(source, "essentials.heal"))
            .executes(context -> healCommand(context, context.getSource().getPlayerOrException()))
            .then(Commands.argument("player", EntityArgument.players())
                .requires(source -> CommandManager.hasPermission(source, "essentials.heal.others"))
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
        
        // /feed [player]
        dispatcher.register(Commands.literal("feed")
            .requires(source -> CommandManager.hasPermission(source, "essentials.feed"))
            .executes(context -> feedCommand(context, context.getSource().getPlayerOrException()))
            .then(Commands.argument("player", EntityArgument.players())
                .requires(source -> CommandManager.hasPermission(source, "essentials.feed.others"))
                .executes(context -> {
                    Collection<ServerPlayer> players = EntityArgument.getPlayers(context, "player");
                    int count = 0;
                    for (ServerPlayer player : players) {
                        count += feedCommand(context, player);
                    }
                    return count;
                })
            )
        );
        
        // /god [player] [on|off]
        dispatcher.register(Commands.literal("god")
            .requires(source -> CommandManager.hasPermission(source, "essentials.god"))
            .executes(context -> godCommand(context, context.getSource().getPlayerOrException(), null))            .then(Commands.argument("enabled", StringToBooleanArgumentType.stringToBoolean())
                .executes(context -> godCommand(context, context.getSource().getPlayerOrException(), 
                                              StringToBooleanArgumentType.getBoolean(context, "enabled")))
                .then(Commands.argument("player", EntityArgument.player())
                    .requires(source -> CommandManager.hasPermission(source, "essentials.god.others"))
                    .executes(context -> godCommand(
                        context, 
                        EntityArgument.getPlayer(context, "player"), 
                        StringToBooleanArgumentType.getBoolean(context, "enabled")
                    ))
                )
            )
            .then(Commands.argument("player", EntityArgument.player())
                .requires(source -> CommandManager.hasPermission(source, "essentials.god.others"))
                .executes(context -> godCommand(
                    context, 
                    EntityArgument.getPlayer(context, "player"),
                    null
                ))
            )
        );
        
        // /fly [player] [on|off]
        dispatcher.register(Commands.literal("fly")
            .requires(source -> CommandManager.hasPermission(source, "essentials.fly"))
            .executes(context -> flyCommand(context, context.getSource().getPlayerOrException(), null))            .then(Commands.argument("enabled", StringToBooleanArgumentType.stringToBoolean())
                .executes(context -> flyCommand(context, context.getSource().getPlayerOrException(), 
                                              StringToBooleanArgumentType.getBoolean(context, "enabled")))
                .then(Commands.argument("player", EntityArgument.player())
                    .requires(source -> CommandManager.hasPermission(source, "essentials.fly.others"))
                    .executes(context -> flyCommand(
                        context, 
                        EntityArgument.getPlayer(context, "player"), 
                        StringToBooleanArgumentType.getBoolean(context, "enabled")
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
        );
        
        // /speed [type] <speed> [player]
        dispatcher.register(Commands.literal("speed")
            .requires(source -> CommandManager.hasPermission(source, "essentials.speed"))
            .then(Commands.literal("walk")
                .requires(source -> CommandManager.hasPermission(source, "essentials.speed.walk"))
                .then(Commands.argument("speed", FloatArgumentType.floatArg(0, 10))
                    .executes(context -> speedCommand(
                        context, 
                        context.getSource().getPlayerOrException(),
                        "walk", 
                        FloatArgumentType.getFloat(context, "speed")
                    ))
                    .then(Commands.argument("player", EntityArgument.player())
                        .requires(source -> CommandManager.hasPermission(source, "essentials.speed.others"))
                        .executes(context -> speedCommand(
                            context,
                            EntityArgument.getPlayer(context, "player"),
                            "walk",
                            FloatArgumentType.getFloat(context, "speed")
                        ))
                    )
                )
            )
            .then(Commands.literal("fly")
                .requires(source -> CommandManager.hasPermission(source, "essentials.speed.fly"))
                .then(Commands.argument("speed", FloatArgumentType.floatArg(0, 10))
                    .executes(context -> speedCommand(
                        context, 
                        context.getSource().getPlayerOrException(),
                        "fly", 
                        FloatArgumentType.getFloat(context, "speed")
                    ))
                    .then(Commands.argument("player", EntityArgument.player())
                        .requires(source -> CommandManager.hasPermission(source, "essentials.speed.others"))
                        .executes(context -> speedCommand(
                            context,
                            EntityArgument.getPlayer(context, "player"),
                            "fly",
                            FloatArgumentType.getFloat(context, "speed")
                        ))
                    )
                )
            )
            .then(Commands.argument("speed", FloatArgumentType.floatArg(0, 10))
                .executes(context -> speedCommand(
                    context, 
                    context.getSource().getPlayerOrException(),
                    null, 
                    FloatArgumentType.getFloat(context, "speed")
                ))
                .then(Commands.argument("player", EntityArgument.player())
                    .requires(source -> CommandManager.hasPermission(source, "essentials.speed.others"))
                    .executes(context -> speedCommand(
                        context,
                        EntityArgument.getPlayer(context, "player"),
                        null,
                        FloatArgumentType.getFloat(context, "speed")
                    ))
                )
            )
        );
        
        // /ext [player]
        dispatcher.register(Commands.literal("ext")
            .requires(source -> CommandManager.hasPermission(source, "essentials.ext"))
            .executes(context -> extinguishCommand(context, context.getSource().getPlayerOrException()))
            .then(Commands.argument("player", EntityArgument.players())
                .requires(source -> CommandManager.hasPermission(source, "essentials.ext.others"))
                .executes(context -> {
                    Collection<ServerPlayer> players = EntityArgument.getPlayers(context, "player");
                    int count = 0;
                    for (ServerPlayer player : players) {
                        count += extinguishCommand(context, player);
                    }
                    return count;
                })
            )
        );
    }
    
    /**
     * Heals a player to full health and removes negative effects
     */
    private int healCommand(CommandContext<CommandSourceStack> context, ServerPlayer player) {
        // Set health to max
        player.setHealth(player.getMaxHealth());
        
        // Remove negative effects
        player.removeAllEffects();
        
        // Fire resistance for 5 seconds to prevent immediate damage
        player.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 100, 0));
        
        // Send confirmation message
        if (player == context.getSource().getEntity()) {
            context.getSource().sendSuccess(() -> Component.translatable("commands.heal.success.self"), true);
        } else {
            context.getSource().sendSuccess(() -> Component.translatable("commands.heal.success.other", player.getDisplayName()), true);
            player.sendSystemMessage(Component.translatable("commands.heal.healed"));
        }
        
        return 1;
    }
    
    /**
     * Feeds a player to max food and saturation
     */
    private int feedCommand(CommandContext<CommandSourceStack> context, ServerPlayer player) {
        // Set food to max
        FoodData foodData = player.getFoodData();
        foodData.setFoodLevel(20);
        foodData.setSaturation(20.0F);
        
        // Send confirmation message
        if (player == context.getSource().getEntity()) {
            context.getSource().sendSuccess(() -> Component.translatable("commands.feed.success.self"), true);
        } else {
            context.getSource().sendSuccess(() -> Component.translatable("commands.feed.success.other", player.getDisplayName()), true);
            player.sendSystemMessage(Component.translatable("commands.feed.fed"));
        }
        
        return 1;
    }
    
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
    
    /**
     * Toggles flight for a player
     */
    private int flyCommand(CommandContext<CommandSourceStack> context, ServerPlayer player, Boolean enabled) {
        UUID playerUuid = player.getUUID();
        boolean isFlying = player.getAbilities().flying;
        
        // If enabled is specified, set to that value, otherwise toggle
        boolean shouldFly = (enabled != null) ? enabled : !player.getAbilities().mayfly;
        
        player.getAbilities().mayfly = shouldFly;
        
        // Keep flying if already flying and still allowed
        if (isFlying && shouldFly) {
            player.getAbilities().flying = true;
        } else if (!shouldFly) {
            player.getAbilities().flying = false;
        }
        
        // Track non-creative players with flight
        if (!player.isCreative() && !player.isSpectator()) {
            if (shouldFly) {
                flyModePlayers.add(playerUuid);
            } else {
                flyModePlayers.remove(playerUuid);
            }
        }
        
        player.onUpdateAbilities();
        
        // Send confirmation message
        if (player == context.getSource().getEntity()) {
            if (shouldFly) {
                context.getSource().sendSuccess(() -> Component.translatable("commands.fly.enabled.self"), true);
            } else {
                context.getSource().sendSuccess(() -> Component.translatable("commands.fly.disabled.self"), true);
            }
        } else {
            if (shouldFly) {
                context.getSource().sendSuccess(() -> Component.translatable("commands.fly.enabled.other", player.getDisplayName()), true);
                player.sendSystemMessage(Component.translatable("commands.fly.enabled.by", context.getSource().getDisplayName()));
            } else {
                context.getSource().sendSuccess(() -> Component.translatable("commands.fly.disabled.other", player.getDisplayName()), true);
                player.sendSystemMessage(Component.translatable("commands.fly.disabled.by", context.getSource().getDisplayName()));
            }
        }
        
        return 1;
    }
    
    /**
     * Sets the speed for a player
     */
    private int speedCommand(CommandContext<CommandSourceStack> context, ServerPlayer player, String type, float speed) {
        // Convert speed from 0-10 scale to Minecraft's 0.1-1.0 scale
        float mcSpeed = speed * 0.1f;
        
        // Determine if we're setting walk or fly speed
        boolean isFlying = player.getAbilities().flying;
        if (type == null) {
            // If type not specified, use the current state
            type = isFlying ? "fly" : "walk";
        }
        
        // Apply speed
        if ("fly".equals(type)) {
            player.getAbilities().setFlyingSpeed(mcSpeed);
        } else {
            player.getAbilities().setWalkingSpeed(mcSpeed);
        }
        
        player.onUpdateAbilities();
        
        // Send confirmation message
        String speedType = "fly".equals(type) ? "flying" : "walking";
        if (player == context.getSource().getEntity()) {
            context.getSource().sendSuccess(() -> 
                Component.translatable("commands.speed.success.self", speedType, speed), true);
        } else {
            context.getSource().sendSuccess(() -> 
                Component.translatable("commands.speed.success.other", 
                    speedType, speed, player.getDisplayName()), true);
            player.sendSystemMessage(Component.translatable("commands.speed.set", speedType, speed));
        }
        
        return 1;
    }
    
    /**
     * Extinguishes a player
     */
    private int extinguishCommand(CommandContext<CommandSourceStack> context, ServerPlayer player) {
        // Extinguish player
        player.clearFire();
        
        // Send confirmation message
        if (player == context.getSource().getEntity()) {
            context.getSource().sendSuccess(() -> Component.translatable("commands.ext.success.self"), true);
        } else {
            context.getSource().sendSuccess(() -> Component.translatable("commands.ext.success.other", player.getDisplayName()), true);
            player.sendSystemMessage(Component.translatable("commands.ext.extinguished"));
        }
        
        return 1;
    }
    
    /**
     * Checks if a player has god mode enabled
     * 
     * @param player The player to check
     * @return True if the player has god mode
     */
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
    }
}
