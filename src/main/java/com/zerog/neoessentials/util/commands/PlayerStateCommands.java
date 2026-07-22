package com.zerog.neoessentials.util.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.zerog.neoessentials.api.permissions.PermissionAPI;
import com.zerog.neoessentials.util.MessageUtil;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.ServerChatEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Player-state and admin-tool commands ported from EssentialsX:
 *  /fly [player] [on|off]    — toggle flight
 *  /god [player] [on|off]    — toggle god mode
 *  /heal [player]             — restore health & hunger
 *  /feed [player]             — restore hunger
 *  /speed [walk|fly] <0-10> [player] — set walk/fly speed
 *  /ext [player]              — extinguish fire
 *  /burn <player> [seconds]   — set player on fire
 *  /give <player> <item> [amount] — give items
 *  /more [amount]             — fill held stack to max
 *  /hat                       — wear held item as helmet
 *  /exp [show|set|give] <amount> [player] — XP management
 *  /sudo <player> <command>   — run command as player; command may be "c:<message>" to
 *                                force chat instead, and <player> may be "*" (everyone but
 *                                the sender) or "**" (everyone including the sender)
 *  /playtime [player]         — show play time
 */
public class PlayerStateCommands {
    private static final Logger LOGGER = LoggerFactory.getLogger(PlayerStateCommands.class);

    // God mode state: UUID → enabled
    private static final Map<UUID, Boolean> godMode = new HashMap<>();
    // Join time for playtime (supplemented by persistent data in a real impl)
    private static final Map<UUID, Long> sessionStart = new HashMap<>();

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        registerFly(dispatcher);
        registerGod(dispatcher);
        registerHeal(dispatcher);
        registerFeed(dispatcher);
        registerSpeed(dispatcher);
        registerExt(dispatcher);
        registerBurn(dispatcher);
        registerGive(dispatcher);
        registerMore(dispatcher);
        registerHat(dispatcher);
        registerExp(dispatcher);
        registerSudo(dispatcher);
        registerPlaytime(dispatcher);
    }

    // ── /fly [player] [on|off] ────────────────────────────────────────────────
    private static void registerFly(CommandDispatcher<CommandSourceStack> d) {
        d.register(Commands.literal("fly")
            .requires(src -> {
                var p = src.getPlayer();
                return p == null || PermissionAPI.hasPermission(p.getUUID(), "neoessentials.fly");
            })
            .executes(ctx -> executeFly(ctx, null, null))
            .then(Commands.argument("target", StringArgumentType.word())
                .suggests((ctx, b) -> SharedSuggestionProvider.suggest(ctx.getSource().getServer().getPlayerNames(), b))
                .requires(src -> src.getPlayer() == null || PermissionAPI.hasPermission(src.getPlayer().getUUID(), "neoessentials.fly.others"))
                .executes(ctx -> executeFly(ctx, StringArgumentType.getString(ctx, "target"), null))
                .then(Commands.literal("on").executes(ctx -> executeFly(ctx, StringArgumentType.getString(ctx, "target"), true)))
                .then(Commands.literal("off").executes(ctx -> executeFly(ctx, StringArgumentType.getString(ctx, "target"), false)))
            )
            .then(Commands.literal("on").executes(ctx -> executeFly(ctx, null, true)))
            .then(Commands.literal("off").executes(ctx -> executeFly(ctx, null, false)))
        );
    }

    private static int executeFly(CommandContext<CommandSourceStack> ctx, String targetName, Boolean enable) {
        var src = ctx.getSource();
        ServerPlayer target = resolveTarget(src, targetName);
        if (target == null) return 0;
        boolean newState = enable != null ? enable : !target.getAbilities().mayfly;
        target.getAbilities().mayfly = newState;
        if (!newState) target.getAbilities().flying = false;
        target.onUpdateAbilities();
        target.fallDistance = 0f;
        String state = newState ? "§aenabled" : "§cdisabled";
        if (isOtherTarget(src, target)) {
            src.sendSuccess(() -> MessageUtil.success("commands.neoessentials.fly.other", target.getName().getString(), state), true);
            target.sendSystemMessage(MessageUtil.info("commands.neoessentials.fly.self", state));
        } else {
            src.sendSuccess(() -> MessageUtil.success("commands.neoessentials.fly.self", state), false);
        }
        LOGGER.info("{} set fly={} for {}", senderName(src), newState, target.getName().getString());
        return 1;
    }

    // ── /god [player] [on|off] ────────────────────────────────────────────────
    private static void registerGod(CommandDispatcher<CommandSourceStack> d) {
        d.register(Commands.literal("god")
            .requires(src -> {
                var p = src.getPlayer();
                return p == null || PermissionAPI.hasPermission(p.getUUID(), "neoessentials.god");
            })
            .executes(ctx -> executeGod(ctx, null, null))
            .then(Commands.argument("target", StringArgumentType.word())
                .suggests((ctx, b) -> SharedSuggestionProvider.suggest(ctx.getSource().getServer().getPlayerNames(), b))
                .requires(src -> src.getPlayer() == null || PermissionAPI.hasPermission(src.getPlayer().getUUID(), "neoessentials.god.others"))
                .executes(ctx -> executeGod(ctx, StringArgumentType.getString(ctx, "target"), null))
                .then(Commands.literal("on").executes(ctx -> executeGod(ctx, StringArgumentType.getString(ctx, "target"), true)))
                .then(Commands.literal("off").executes(ctx -> executeGod(ctx, StringArgumentType.getString(ctx, "target"), false)))
            )
            .then(Commands.literal("on").executes(ctx -> executeGod(ctx, null, true)))
            .then(Commands.literal("off").executes(ctx -> executeGod(ctx, null, false)))
        );
    }

    private static int executeGod(CommandContext<CommandSourceStack> ctx, String targetName, Boolean enable) {
        var src = ctx.getSource();
        ServerPlayer target = resolveTarget(src, targetName);
        if (target == null) return 0;
        boolean cur = godMode.getOrDefault(target.getUUID(), false);
        boolean newState = enable != null ? enable : !cur;
        godMode.put(target.getUUID(), newState);
        // Restore health/hunger when enabling (Essentials pattern)
        if (newState) {
            target.setHealth(target.getMaxHealth());
            target.getFoodData().setFoodLevel(20);
        }
        String state = newState ? "§aenabled" : "§cdisabled";
        if (isOtherTarget(src, target)) {
            src.sendSuccess(() -> MessageUtil.success("commands.neoessentials.god.other", target.getName().getString(), state), true);
            target.sendSystemMessage(MessageUtil.info("commands.neoessentials.god.self", state));
        } else {
            src.sendSuccess(() -> MessageUtil.success("commands.neoessentials.god.self", state), false);
        }
        return 1;
    }

    /** Returns true if this player is in god mode. Used by damage event handler. */
    public static boolean isGodMode(UUID uuid) {
        return godMode.getOrDefault(uuid, false);
    }

    // ── Dashboard-facing wrappers ─────────────────────────────────────────────
    // Same state changes as the /fly, /god, /feed, /speed, /ext commands, just driven
    // by a target ServerPlayer directly instead of a CommandSourceStack — the dashboard
    // has no in-game command source to reply through.

    /** Sets flight ability on/off for {@code target}. Returns the new state. */
    public static boolean setFly(ServerPlayer target, Boolean enable) {
        boolean newState = enable != null ? enable : !target.getAbilities().mayfly;
        target.getAbilities().mayfly = newState;
        if (!newState) target.getAbilities().flying = false;
        target.onUpdateAbilities();
        target.fallDistance = 0f;
        return newState;
    }

    /** Sets god mode on/off for {@code target}. Returns the new state. */
    public static boolean setGod(ServerPlayer target, Boolean enable) {
        boolean cur = godMode.getOrDefault(target.getUUID(), false);
        boolean newState = enable != null ? enable : !cur;
        godMode.put(target.getUUID(), newState);
        if (newState) {
            target.setHealth(target.getMaxHealth());
            target.getFoodData().setFoodLevel(20);
        }
        return newState;
    }

    /** Restores hunger/saturation for {@code target}. */
    public static void feedPlayer(ServerPlayer target) {
        target.getFoodData().setFoodLevel(20);
        target.getFoodData().setSaturation(20f);
    }

    /** Extinguishes any fire on {@code target}. */
    public static void extinguishPlayer(ServerPlayer target) {
        target.clearFire();
    }

    /** Sets walk or fly speed (0-10 scale, same mapping as {@code /speed}) for {@code target}. */
    public static void setSpeed(ServerPlayer target, boolean fly, float speed) {
        float mcSpeed = Math.min(speed / 10f, 1.0f);
        if (fly) {
            target.getAbilities().setFlyingSpeed(mcSpeed);
            target.onUpdateAbilities();
        } else {
            var attr = target.getAttribute(Attributes.MOVEMENT_SPEED);
            if (attr != null) attr.setBaseValue(mcSpeed);
        }
    }

    /**
     * Forces {@code target} to run a command (or chat, if {@code isChat}), same as {@code /sudo}
     * — just driven by the dashboard instead of a CommandSourceStack. Respects
     * {@code neoessentials.sudo.exempt} the same way the command does. Returns an error message,
     * or {@code null} on success.
     */
    public static String runSudoAdmin(ServerPlayer target, String command, boolean isChat) {
        if (PermissionAPI.hasPermission(target.getUUID(), "neoessentials.sudo.exempt")) {
            return target.getName().getString() + " is exempt from /sudo.";
        }
        if (isChat) {
            sudoChat(target, command);
        } else {
            String payload = command.startsWith("/") ? command.substring(1) : command;
            target.getServer().getCommands().performPrefixedCommand(target.createCommandSourceStack(), payload);
        }
        return null;
    }

    /** Called on player quit to clean up god/fly state. */
    public static void onPlayerQuit(UUID uuid) {
        godMode.remove(uuid);
        sessionStart.remove(uuid);
    }

    /** Called on player join to track session start. */
    public static void onPlayerJoin(UUID uuid) {
        sessionStart.put(uuid, System.currentTimeMillis());
    }

    // ── /heal [player] ────────────────────────────────────────────────────────
    private static void registerHeal(CommandDispatcher<CommandSourceStack> d) {
        d.register(Commands.literal("heal")
            .requires(src -> {
                var p = src.getPlayer();
                return p == null || PermissionAPI.hasPermission(p.getUUID(), "neoessentials.heal");
            })
            .executes(ctx -> executeHeal(ctx, null))
            .then(Commands.argument("target", StringArgumentType.word())
                .suggests((ctx, b) -> SharedSuggestionProvider.suggest(ctx.getSource().getServer().getPlayerNames(), b))
                .requires(src -> src.getPlayer() == null || PermissionAPI.hasPermission(src.getPlayer().getUUID(), "neoessentials.heal.others"))
                .executes(ctx -> executeHeal(ctx, StringArgumentType.getString(ctx, "target")))
            )
        );
    }

    private static int executeHeal(CommandContext<CommandSourceStack> ctx, String targetName) {
        var src = ctx.getSource();
        ServerPlayer target = resolveTarget(src, targetName);
        if (target == null) return 0;
        if (target.isDeadOrDying()) {
            src.sendFailure(MessageUtil.error("commands.neoessentials.heal.dead", target.getName().getString()));
            return 0;
        }
        target.setHealth(target.getMaxHealth());
        target.getFoodData().setFoodLevel(20);
        target.getFoodData().setSaturation(20f);
        target.removeAllEffects();
        if (isOtherTarget(src, target)) {
            src.sendSuccess(() -> MessageUtil.success("commands.neoessentials.heal.other", target.getName().getString()), true);
            target.sendSystemMessage(MessageUtil.success("commands.neoessentials.heal.self"));
        } else {
            src.sendSuccess(() -> MessageUtil.success("commands.neoessentials.heal.self"), false);
        }
        return 1;
    }

    // ── /feed [player] ────────────────────────────────────────────────────────
    private static void registerFeed(CommandDispatcher<CommandSourceStack> d) {
        d.register(Commands.literal("feed")
            .requires(src -> {
                var p = src.getPlayer();
                return p == null || PermissionAPI.hasPermission(p.getUUID(), "neoessentials.feed");
            })
            .executes(ctx -> executeFeed(ctx, null))
            .then(Commands.argument("target", StringArgumentType.word())
                .suggests((ctx, b) -> SharedSuggestionProvider.suggest(ctx.getSource().getServer().getPlayerNames(), b))
                .requires(src -> src.getPlayer() == null || PermissionAPI.hasPermission(src.getPlayer().getUUID(), "neoessentials.feed.others"))
                .executes(ctx -> executeFeed(ctx, StringArgumentType.getString(ctx, "target")))
            )
        );
    }

    private static int executeFeed(CommandContext<CommandSourceStack> ctx, String targetName) {
        var src = ctx.getSource();
        ServerPlayer target = resolveTarget(src, targetName);
        if (target == null) return 0;
        target.getFoodData().setFoodLevel(20);
        target.getFoodData().setSaturation(20f);
        if (isOtherTarget(src, target)) {
            src.sendSuccess(() -> MessageUtil.success("commands.neoessentials.feed.other", target.getName().getString()), true);
            target.sendSystemMessage(MessageUtil.success("commands.neoessentials.feed.self"));
        } else {
            src.sendSuccess(() -> MessageUtil.success("commands.neoessentials.feed.self"), false);
        }
        return 1;
    }

    // ── /speed [walk|fly] <0-10> [player] ────────────────────────────────────
    // Essentials: walk speed 0-10 maps to Minecraft 0.0-1.0 (x0.1), fly speed same
    private static void registerSpeed(CommandDispatcher<CommandSourceStack> d) {
        d.register(Commands.literal("speed")
            .requires(src -> {
                var p = src.getPlayer();
                return p == null || PermissionAPI.hasPermission(p.getUUID(), "neoessentials.speed");
            })
            // /speed <value>  (auto-detect walk/fly based on current state)
            .then(Commands.argument("speed", FloatArgumentType.floatArg(0f, 10f))
                .executes(ctx -> executeSpeed(ctx, null, FloatArgumentType.getFloat(ctx, "speed"), null))
                .then(Commands.argument("target", StringArgumentType.word())
                    .suggests((ctx, b) -> SharedSuggestionProvider.suggest(ctx.getSource().getServer().getPlayerNames(), b))
                    .requires(src -> src.getPlayer() == null || PermissionAPI.hasPermission(src.getPlayer().getUUID(), "neoessentials.speed.others"))
                    .executes(ctx -> executeSpeed(ctx, null, FloatArgumentType.getFloat(ctx, "speed"), StringArgumentType.getString(ctx, "target")))
                )
            )
            // /speed walk <value> [player]
            .then(Commands.literal("walk")
                .then(Commands.argument("speed", FloatArgumentType.floatArg(0f, 10f))
                    .executes(ctx -> executeSpeed(ctx, false, FloatArgumentType.getFloat(ctx, "speed"), null))
                    .then(Commands.argument("target", StringArgumentType.word())
                        .suggests((ctx, b) -> SharedSuggestionProvider.suggest(ctx.getSource().getServer().getPlayerNames(), b))
                        .requires(src -> src.getPlayer() == null || PermissionAPI.hasPermission(src.getPlayer().getUUID(), "neoessentials.speed.others"))
                        .executes(ctx -> executeSpeed(ctx, false, FloatArgumentType.getFloat(ctx, "speed"), StringArgumentType.getString(ctx, "target")))
                    )
                )
            )
            // /speed fly <value> [player]
            .then(Commands.literal("fly")
                .then(Commands.argument("speed", FloatArgumentType.floatArg(0f, 10f))
                    .executes(ctx -> executeSpeed(ctx, true, FloatArgumentType.getFloat(ctx, "speed"), null))
                    .then(Commands.argument("target", StringArgumentType.word())
                        .suggests((ctx, b) -> SharedSuggestionProvider.suggest(ctx.getSource().getServer().getPlayerNames(), b))
                        .requires(src -> src.getPlayer() == null || PermissionAPI.hasPermission(src.getPlayer().getUUID(), "neoessentials.speed.others"))
                        .executes(ctx -> executeSpeed(ctx, true, FloatArgumentType.getFloat(ctx, "speed"), StringArgumentType.getString(ctx, "target")))
                    )
                )
            )
        );
    }

    private static int executeSpeed(CommandContext<CommandSourceStack> ctx, Boolean fly, float speed, String targetName) {
        var src = ctx.getSource();
        ServerPlayer target = resolveTarget(src, targetName);
        if (target == null) return 0;
        // Auto-detect fly mode if not specified
        boolean isFly = fly != null ? fly : target.getAbilities().flying;
        // Essentials maps 0-10 to 0.0-1.0 (x0.1), capped at 1.0
        float mcSpeed = Math.min(speed / 10f, 1.0f);
        if (isFly) {
            // NOTE: Attributes.FLYING_SPEED is never actually consulted for player flight —
            // Player.createAttributes() only registers MOVEMENT_SPEED, so
            // getAttribute(Attributes.FLYING_SPEED) always returns null here and the old
            // "if (attr != null)" guard silently no-op'd every call. Actual client-side fly
            // speed is driven by Abilities.flyingSpeed, synced via onUpdateAbilities()'s
            // ClientboundPlayerAbilitiesPacket — that's the value that needs setting.
            target.getAbilities().setFlyingSpeed(mcSpeed);
            target.onUpdateAbilities();
        } else {
            // Walk speed via MOVEMENT_SPEED attribute
            var attr = target.getAttribute(Attributes.MOVEMENT_SPEED);
            if (attr != null) attr.setBaseValue(mcSpeed);
        }
        String type = isFly ? "fly" : "walk";
        if (isOtherTarget(src, target)) {
            src.sendSuccess(() -> MessageUtil.success("commands.neoessentials.speed.other", target.getName().getString(), type, speed), true);
            target.sendSystemMessage(MessageUtil.info("commands.neoessentials.speed.self", type, speed));
        } else {
            src.sendSuccess(() -> MessageUtil.success("commands.neoessentials.speed.self", type, speed), false);
        }
        return 1;
    }

    // ── /ext [player] ─────────────────────────────────────────────────────────
    private static void registerExt(CommandDispatcher<CommandSourceStack> d) {
        d.register(Commands.literal("ext")
            .requires(src -> { var p = src.getPlayer(); return p == null || PermissionAPI.hasPermission(p.getUUID(), "neoessentials.ext"); })
            .executes(ctx -> executeExt(ctx, null))
            .then(Commands.argument("target", StringArgumentType.word())
                .suggests((ctx, b) -> SharedSuggestionProvider.suggest(ctx.getSource().getServer().getPlayerNames(), b))
                .requires(src -> src.getPlayer() == null || PermissionAPI.hasPermission(src.getPlayer().getUUID(), "neoessentials.ext.others"))
                .executes(ctx -> executeExt(ctx, StringArgumentType.getString(ctx, "target")))
            )
        );
        // alias: /extinguish
        d.register(Commands.literal("extinguish")
            .requires(src -> { var p = src.getPlayer(); return p == null || PermissionAPI.hasPermission(p.getUUID(), "neoessentials.ext"); })
            .executes(ctx -> executeExt(ctx, null))
            .then(Commands.argument("target", StringArgumentType.word())
                .suggests((ctx, b) -> SharedSuggestionProvider.suggest(ctx.getSource().getServer().getPlayerNames(), b))
                .requires(src -> src.getPlayer() == null || PermissionAPI.hasPermission(src.getPlayer().getUUID(), "neoessentials.ext.others"))
                .executes(ctx -> executeExt(ctx, StringArgumentType.getString(ctx, "target")))
            )
        );
    }

    private static int executeExt(CommandContext<CommandSourceStack> ctx, String targetName) {
        var src = ctx.getSource();
        ServerPlayer target = resolveTarget(src, targetName);
        if (target == null) return 0;
        target.clearFire();
        if (isOtherTarget(src, target)) {
            src.sendSuccess(() -> MessageUtil.success("commands.neoessentials.ext.other", target.getName().getString()), true);
            target.sendSystemMessage(MessageUtil.success("commands.neoessentials.ext.self"));
        } else {
            src.sendSuccess(() -> MessageUtil.success("commands.neoessentials.ext.self"), false);
        }
        return 1;
    }

    // ── /burn <player> [seconds] ──────────────────────────────────────────────
    private static void registerBurn(CommandDispatcher<CommandSourceStack> d) {
        d.register(Commands.literal("burn")
            .requires(src -> { var p = src.getPlayer(); return p == null || PermissionAPI.hasPermission(p.getUUID(), "neoessentials.burn"); })
            .then(Commands.argument("target", StringArgumentType.word())
                .suggests((ctx, b) -> SharedSuggestionProvider.suggest(ctx.getSource().getServer().getPlayerNames(), b))
                .executes(ctx -> executeBurn(ctx, StringArgumentType.getString(ctx, "target"), 10))
                .then(Commands.argument("seconds", IntegerArgumentType.integer(1, 600))
                    .executes(ctx -> executeBurn(ctx, StringArgumentType.getString(ctx, "target"), IntegerArgumentType.getInteger(ctx, "seconds")))
                )
            )
        );
    }

    private static int executeBurn(CommandContext<CommandSourceStack> ctx, String targetName, int seconds) {
        var src = ctx.getSource();
        ServerPlayer target = resolveTarget(src, targetName);
        if (target == null) return 0;
        target.setRemainingFireTicks(seconds * 20);
        src.sendSuccess(() -> MessageUtil.success("commands.neoessentials.burn.success", target.getName().getString(), seconds), true);
        return 1;
    }

    // ── /give <player> <item> [amount] ────────────────────────────────────────
    private static void registerGive(CommandDispatcher<CommandSourceStack> d) {
        d.register(Commands.literal("give")
            .requires(src -> { var p = src.getPlayer(); return p == null || PermissionAPI.hasPermission(p.getUUID(), "neoessentials.give"); })
            .then(Commands.argument("target", StringArgumentType.word())
                .suggests((ctx, b) -> SharedSuggestionProvider.suggest(ctx.getSource().getServer().getPlayerNames(), b))
                .then(Commands.argument("item", StringArgumentType.word())
                    .executes(ctx -> executeGive(ctx, StringArgumentType.getString(ctx, "target"), StringArgumentType.getString(ctx, "item"), 1))
                    .then(Commands.argument("amount", IntegerArgumentType.integer(1, 3456))
                        .executes(ctx -> executeGive(ctx,
                            StringArgumentType.getString(ctx, "target"),
                            StringArgumentType.getString(ctx, "item"),
                            IntegerArgumentType.getInteger(ctx, "amount")))
                    )
                )
            )
        );
    }

    private static int executeGive(CommandContext<CommandSourceStack> ctx, String targetName, String itemId, int amount) {
        var src = ctx.getSource();
        ServerPlayer target = resolveTarget(src, targetName);
        if (target == null) return 0;
        ItemStack stack = com.zerog.neoessentials.economy.worth.WorthManager.resolveItem(itemId);
        if (stack == null) {
            src.sendFailure(MessageUtil.error("commands.neoessentials.give.unknown_item", itemId));
            return 0;
        }
        // Distribute across multiple stacks if amount > maxStackSize
        int remaining = amount;
        int maxStack = stack.getMaxStackSize();
        Inventory inv = target.getInventory();
        while (remaining > 0) {
            int give = Math.min(remaining, maxStack);
            ItemStack toGive = stack.copyWithCount(give);
            if (!inv.add(toGive)) {
                target.drop(toGive, false);
            }
            remaining -= give;
        }
        src.sendSuccess(() -> MessageUtil.success("commands.neoessentials.give.success",
            amount, itemId, target.getName().getString()), true);
        target.sendSystemMessage(MessageUtil.info("commands.neoessentials.give.received", amount, itemId));
        return 1;
    }

    // ── /more [amount] ────────────────────────────────────────────────────────
    private static void registerMore(CommandDispatcher<CommandSourceStack> d) {
        d.register(Commands.literal("more")
            .requires(src -> { var p = src.getPlayer(); return p == null || PermissionAPI.hasPermission(p.getUUID(), "neoessentials.more"); })
            .executes(ctx -> executeMore(ctx, 0))
            .then(Commands.argument("amount", IntegerArgumentType.integer(1, 3456))
                .executes(ctx -> executeMore(ctx, IntegerArgumentType.getInteger(ctx, "amount")))
            )
        );
    }

    private static int executeMore(CommandContext<CommandSourceStack> ctx, int amount) {
        var src = ctx.getSource();
        var player = src.getPlayer();
        if (player == null) { src.sendFailure(MessageUtil.error("commands.neoessentials.general.player_only")); return 0; }
        ItemStack held = player.getMainHandItem();
        if (held.isEmpty()) { src.sendFailure(MessageUtil.error("commands.neoessentials.more.no_item")); return 0; }
        int newCount = amount > 0 ? amount : held.getMaxStackSize();
        held.setCount(newCount);
        src.sendSuccess(() -> MessageUtil.success("commands.neoessentials.more.success", newCount, com.zerog.neoessentials.economy.worth.WorthManager.getItemId(held)), false);
        return 1;
    }

    // ── /hat ──────────────────────────────────────────────────────────────────
    private static void registerHat(CommandDispatcher<CommandSourceStack> d) {
        d.register(Commands.literal("hat")
            .requires(src -> { var p = src.getPlayer(); return p == null || PermissionAPI.hasPermission(p.getUUID(), "neoessentials.hat"); })
            .executes(ctx -> {
                var src = ctx.getSource();
                var player = src.getPlayer();
                if (player == null) { src.sendFailure(MessageUtil.error("commands.neoessentials.general.player_only")); return 0; }
                ItemStack held = player.getMainHandItem();
                if (held.isEmpty()) { src.sendFailure(MessageUtil.error("commands.neoessentials.hat.no_item")); return 0; }
                ItemStack current = player.getInventory().armor.get(3); // helmet slot (index 3)
                player.getInventory().armor.set(3, held.copy());
                player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, current);
                src.sendSuccess(() -> MessageUtil.success("commands.neoessentials.hat.success"), false);
                return 1;
            })
        );
    }

    // ── /exp [show|set|give] [amount] [player] ────────────────────────────────
    private static void registerExp(CommandDispatcher<CommandSourceStack> d) {
        d.register(Commands.literal("exp")
            .requires(src -> { var p = src.getPlayer(); return p == null || PermissionAPI.hasPermission(p.getUUID(), "neoessentials.exp"); })
            // /exp  — show own exp
            .executes(ctx -> executeExpShow(ctx, null))
            // /exp show [player]
            .then(Commands.literal("show")
                .executes(ctx -> executeExpShow(ctx, null))
                .then(Commands.argument("target", StringArgumentType.word())
                    .suggests((ctx, b) -> SharedSuggestionProvider.suggest(ctx.getSource().getServer().getPlayerNames(), b))
                    .executes(ctx -> executeExpShow(ctx, StringArgumentType.getString(ctx, "target")))
                )
            )
            // /exp set <amount> [player]
            .then(Commands.literal("set")
                .requires(src -> src.getPlayer() == null || PermissionAPI.hasPermission(src.getPlayer().getUUID(), "neoessentials.exp.set"))
                .then(Commands.argument("amount", IntegerArgumentType.integer(0))
                    .executes(ctx -> executeExpSet(ctx, IntegerArgumentType.getInteger(ctx, "amount"), null))
                    .then(Commands.argument("target", StringArgumentType.word())
                        .suggests((ctx, b) -> SharedSuggestionProvider.suggest(ctx.getSource().getServer().getPlayerNames(), b))
                        .requires(src -> src.getPlayer() == null || PermissionAPI.hasPermission(src.getPlayer().getUUID(), "neoessentials.exp.set.others"))
                        .executes(ctx -> executeExpSet(ctx, IntegerArgumentType.getInteger(ctx, "amount"), StringArgumentType.getString(ctx, "target")))
                    )
                )
            )
            // /exp give <amount> [player]
            .then(Commands.literal("give")
                .requires(src -> src.getPlayer() == null || PermissionAPI.hasPermission(src.getPlayer().getUUID(), "neoessentials.exp.give"))
                .then(Commands.argument("amount", IntegerArgumentType.integer(1))
                    .executes(ctx -> executeExpGive(ctx, IntegerArgumentType.getInteger(ctx, "amount"), null))
                    .then(Commands.argument("target", StringArgumentType.word())
                        .suggests((ctx, b) -> SharedSuggestionProvider.suggest(ctx.getSource().getServer().getPlayerNames(), b))
                        .requires(src -> src.getPlayer() == null || PermissionAPI.hasPermission(src.getPlayer().getUUID(), "neoessentials.exp.give.others"))
                        .executes(ctx -> executeExpGive(ctx, IntegerArgumentType.getInteger(ctx, "amount"), StringArgumentType.getString(ctx, "target")))
                    )
                )
            )
        );
        // /xp alias
        d.register(Commands.literal("xp")
            .requires(src -> { var p = src.getPlayer(); return p == null || PermissionAPI.hasPermission(p.getUUID(), "neoessentials.exp"); })
            .executes(ctx -> executeExpShow(ctx, null))
            .then(Commands.literal("set")
                .requires(src -> src.getPlayer() == null || PermissionAPI.hasPermission(src.getPlayer().getUUID(), "neoessentials.exp.set"))
                .then(Commands.argument("amount", IntegerArgumentType.integer(0))
                    .executes(ctx -> executeExpSet(ctx, IntegerArgumentType.getInteger(ctx, "amount"), null))
                )
            )
            .then(Commands.literal("give")
                .requires(src -> src.getPlayer() == null || PermissionAPI.hasPermission(src.getPlayer().getUUID(), "neoessentials.exp.give"))
                .then(Commands.argument("amount", IntegerArgumentType.integer(1))
                    .executes(ctx -> executeExpGive(ctx, IntegerArgumentType.getInteger(ctx, "amount"), null))
                )
            )
        );
    }

    private static int executeExpShow(CommandContext<CommandSourceStack> ctx, String targetName) {
        var src = ctx.getSource();
        ServerPlayer target = resolveTarget(src, targetName);
        if (target == null) return 0;
        int totalXp = getExperienceTotal(target);
        int level = target.experienceLevel;
        src.sendSuccess(() -> MessageUtil.info("commands.neoessentials.exp.show",
            target.getName().getString(), level, totalXp), false);
        return 1;
    }

    private static int executeExpSet(CommandContext<CommandSourceStack> ctx, int amount, String targetName) {
        var src = ctx.getSource();
        ServerPlayer target = resolveTarget(src, targetName);
        if (target == null) return 0;
        target.setExperienceLevels(0);
        target.setExperiencePoints(0);
        target.giveExperiencePoints(amount);
        src.sendSuccess(() -> MessageUtil.success("commands.neoessentials.exp.set", target.getName().getString(), amount), true);
        return 1;
    }

    private static int executeExpGive(CommandContext<CommandSourceStack> ctx, int amount, String targetName) {
        var src = ctx.getSource();
        ServerPlayer target = resolveTarget(src, targetName);
        if (target == null) return 0;
        target.giveExperiencePoints(amount);
        src.sendSuccess(() -> MessageUtil.success("commands.neoessentials.exp.give", target.getName().getString(), amount), true);
        return 1;
    }

    /** Calculate total XP from level + progress. */
    private static int getExperienceTotal(ServerPlayer p) {
        int level = p.experienceLevel;
        float progress = p.experienceProgress;
        int xpToNextLevel = p.getXpNeededForNextLevel();
        return (int)(getXpForLevel(level) + (progress * xpToNextLevel));
    }

    private static int getXpForLevel(int level) {
        if (level <= 16) return (int)(level * level + 6 * level);
        if (level <= 31) return (int)(2.5 * level * level - 40.5 * level + 360);
        return (int)(4.5 * level * level - 162.5 * level + 2220);
    }

    // ── /sudo <player|*|**> <command|c:message> ─────────────────────────────────
    // "*" and "**" are registered as their own literal branches, not folded into the
    // "target" word argument below — Brigadier's unquoted-string charset for
    // StringArgumentType.word() doesn't include '*', so a literal "*"/"**" token would
    // never match that argument and always fail to parse ("trailing data").
    private static void registerSudo(CommandDispatcher<CommandSourceStack> d) {
        d.register(Commands.literal("sudo")
            .requires(src -> { var p = src.getPlayer(); return p == null || PermissionAPI.hasPermission(p.getUUID(), "neoessentials.sudo"); })
            .then(Commands.literal("*")
                .then(Commands.argument("command", StringArgumentType.greedyString())
                    .executes(ctx -> executeSudo(ctx, "*", StringArgumentType.getString(ctx, "command"))))
            )
            .then(Commands.literal("**")
                .then(Commands.argument("command", StringArgumentType.greedyString())
                    .executes(ctx -> executeSudo(ctx, "**", StringArgumentType.getString(ctx, "command"))))
            )
            .then(Commands.argument("target", StringArgumentType.word())
                .suggests((ctx, b) -> SharedSuggestionProvider.suggest(
                    Stream.concat(Stream.of("*", "**"), Arrays.stream(ctx.getSource().getServer().getPlayerNames())),
                    b))
                .then(Commands.argument("command", StringArgumentType.greedyString())
                    .executes(ctx -> executeSudo(ctx,
                        StringArgumentType.getString(ctx, "target"),
                        StringArgumentType.getString(ctx, "command")))
                )
            )
        );
    }

    private static int executeSudo(CommandContext<CommandSourceStack> ctx, String targetSpec, String command) {
        var src = ctx.getSource();
        boolean isWildcard = targetSpec.equals("*") || targetSpec.equals("**");
        boolean includeSelf = targetSpec.equals("**");
        ServerPlayer self = src.getPlayer();

        List<ServerPlayer> targets;
        if (isWildcard) {
            targets = src.getServer().getPlayerList().getPlayers().stream()
                .filter(p -> includeSelf || self == null || !p.getUUID().equals(self.getUUID()))
                .collect(Collectors.toList());
            if (targets.isEmpty()) {
                src.sendFailure(MessageUtil.error("commands.neoessentials.sudo.no_targets"));
                return 0;
            }
        } else {
            ServerPlayer target = src.getServer().getPlayerList().getPlayerByName(targetSpec);
            if (target == null) {
                src.sendFailure(MessageUtil.error("commands.neoessentials.general.player_not_found", targetSpec));
                return 0;
            }
            // Prevent explicitly sudo-ing yourself by name (use ** to include yourself in bulk).
            if (self != null && self.getUUID().equals(target.getUUID())) {
                src.sendFailure(MessageUtil.error("commands.neoessentials.sudo.self"));
                return 0;
            }
            // Essentials: sudo.exempt check
            if (PermissionAPI.hasPermission(target.getUUID(), "neoessentials.sudo.exempt") && self != null) {
                src.sendFailure(MessageUtil.error("commands.neoessentials.sudo.exempt", targetSpec));
                return 0;
            }
            targets = List.of(target);
        }

        boolean isChat = command.startsWith("c:");
        String payload = isChat ? command.substring(2) : (command.startsWith("/") ? command.substring(1) : command);

        List<ServerPlayer> affected = new ArrayList<>();
        for (ServerPlayer target : targets) {
            // In bulk (wildcard) mode, silently skip exempt players instead of aborting the whole batch.
            if (isWildcard && self != null && PermissionAPI.hasPermission(target.getUUID(), "neoessentials.sudo.exempt")) {
                continue;
            }
            if (isChat) {
                sudoChat(target, payload);
            } else {
                src.getServer().getCommands().performPrefixedCommand(target.createCommandSourceStack(), payload);
            }
            affected.add(target);
        }

        String cmdLabel = "/" + payload;
        if (!isWildcard) {
            String name = affected.get(0).getName().getString();
            String key = isChat ? "commands.neoessentials.sudo.success.chat" : "commands.neoessentials.sudo.success";
            String label = isChat ? payload : cmdLabel;
            src.sendSuccess(() -> MessageUtil.success(key, name, label), true);
        } else {
            int count = affected.size();
            String key = isChat ? "commands.neoessentials.sudo.success.chat.bulk" : "commands.neoessentials.sudo.success.bulk";
            String label = isChat ? payload : cmdLabel;
            src.sendSuccess(() -> MessageUtil.success(key, count, label), true);
        }
        LOGGER.info("{} sudoed {} player(s) ({}) to {}: {}",
            senderName(src), affected.size(), targetSpec, isChat ? "say" : "run", payload);
        return affected.size();
    }

    /** Forces a player's client to publicly chat {@code message}, through the same
     *  NeoEssentials chat pipeline (formatting, mute/freeze/anti-spam checks, Discord
     *  relay) real player-typed chat goes through — see ChannelCommands' identical pattern. */
    private static void sudoChat(ServerPlayer player, String message) {
        @SuppressWarnings("UnstableApiUsage")
        ServerChatEvent chatEvent = new ServerChatEvent(player, message, Component.literal(message));
        NeoForge.EVENT_BUS.post(chatEvent);
    }

    // ── /playtime [player] ────────────────────────────────────────────────────
    private static void registerPlaytime(CommandDispatcher<CommandSourceStack> d) {
        d.register(Commands.literal("playtime")
            .requires(src -> { var p = src.getPlayer(); return p == null || PermissionAPI.hasPermission(p.getUUID(), "neoessentials.playtime"); })
            .executes(ctx -> executePlaytime(ctx, null))
            .then(Commands.argument("target", StringArgumentType.word())
                .suggests((ctx, b) -> SharedSuggestionProvider.suggest(ctx.getSource().getServer().getPlayerNames(), b))
                .requires(src -> src.getPlayer() == null || PermissionAPI.hasPermission(src.getPlayer().getUUID(), "neoessentials.playtime.others"))
                .executes(ctx -> executePlaytime(ctx, StringArgumentType.getString(ctx, "target")))
            )
        );
    }

    private static int executePlaytime(CommandContext<CommandSourceStack> ctx, String targetName) {
        var src = ctx.getSource();
        ServerPlayer target = resolveTarget(src, targetName);
        if (target == null) return 0;
        long sessionMs = 0;
        Long start = sessionStart.get(target.getUUID());
        if (start != null) sessionMs = System.currentTimeMillis() - start;
        // NeoForge 1.21.1: Stats.PLAY_TIME is a ResourceLocation key; use Stats.CUSTOM.get() for the Stat object
        int ticksPlayed = target.getStats().getValue(Stats.CUSTOM.get(Stats.PLAY_TIME));
        long totalMs = (ticksPlayed * 50L) + sessionMs;
        String formatted = formatDuration(totalMs);
        src.sendSuccess(() -> MessageUtil.info("commands.neoessentials.playtime.result",
            target.getName().getString(), formatted), false);
        return 1;
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private static ServerPlayer resolveTarget(CommandSourceStack src, String targetName) {
        if (targetName != null) {
            ServerPlayer p = src.getServer().getPlayerList().getPlayerByName(targetName);
            if (p == null) {
                src.sendFailure(MessageUtil.error("commands.neoessentials.general.player_not_found", targetName));
            }
            return p;
        }
        ServerPlayer self = src.getPlayer();
        if (self == null) {
            src.sendFailure(MessageUtil.error("commands.neoessentials.general.player_only"));
        }
        return self;
    }

    private static boolean isOtherTarget(CommandSourceStack src, ServerPlayer target) {
        return src.getPlayer() == null || !src.getPlayer().getUUID().equals(target.getUUID());
    }

    private static String senderName(CommandSourceStack src) {
        return src.getPlayer() != null ? src.getPlayer().getName().getString() : "Console";
    }

    private static String formatDuration(long ms) {
        long s = ms / 1000, m = s / 60, h = m / 60, days = h / 24;
        if (days > 0) return days + "d " + (h % 24) + "h " + (m % 60) + "m";
        if (h > 0) return h + "h " + (m % 60) + "m " + (s % 60) + "s";
        if (m > 0) return m + "m " + (s % 60) + "s";
        return s + "s";
    }
}






