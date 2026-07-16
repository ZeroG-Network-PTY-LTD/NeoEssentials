package com.zerog.neoessentials.util.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.zerog.neoessentials.api.permissions.PermissionAPI;
import com.zerog.neoessentials.util.MessageUtil;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.MessageArgument;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.CartographyTableMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.LoomMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemLore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Item customisation & miscellaneous commands ported from EssentialsX:
 *
 *  /me <action>                     — broadcast action message
 *  /tptoggle [on|off]               — toggle teleport request acceptance
 *  /gc                              — show server memory/TPS/uptime info
 *  /lightning [player]              — strike lightning at target or self (alias: /smite)
 *  /skull [player]                  — get a player head item
 *  /itemname [name|-]               — rename held item
 *  /itemlore add|set|clear [args]   — edit held item lore
 *  /remove <type> [radius] [world]  — remove entities in radius
 *  /loom                            — open portable loom
 *  /cartography                     — open portable cartography table
 */
public class ItemCustomisationCommands {
    private static final Logger LOGGER = LoggerFactory.getLogger(ItemCustomisationCommands.class);

    // tptoggle state: UUID → allowed (true=can receive tp requests, false=blocked)
    private static final Map<UUID, Boolean> tpToggleState = new ConcurrentHashMap<>();

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        registerMe(dispatcher);
        registerTpToggle(dispatcher);
        registerGc(dispatcher);
        registerLightning(dispatcher);
        registerSkull(dispatcher);
        registerItemName(dispatcher);
        registerItemLore(dispatcher);
        registerRemove(dispatcher);
        registerLoom(dispatcher);
        registerCartography(dispatcher);
    }

    // ── /me <action> ──────────────────────────────────────────────────────────
    private static void registerMe(CommandDispatcher<CommandSourceStack> d) {
        d.register(Commands.literal("me")
            .requires(src -> {
                var p = src.getPlayer();
                return p == null || PermissionAPI.hasPermission(p.getUUID(), "neoessentials.me");
            })
            // Vanilla already registers its own built-in "/me <action>" using
            // MessageArgumentType for the "action" argument. Brigadier's addChild() merges
            // same-named argument nodes by keeping the FIRST-registered node's type and just
            // swapping in whichever .executes() was registered last — so regardless of what
            // type this registration declares, the argument is retrieved at runtime as
            // whatever vanilla's node actually is. Using StringArgumentType here (as this used
            // to) threw "Argument 'action' is defined as Message, not String" for exactly that
            // reason. Matching vanilla's own MessageArgument.message() avoids the mismatch.
            .then(Commands.argument("action", MessageArgument.message())
                .executes(ctx -> {
                    var src = ctx.getSource();
                    String action = MessageArgument.getMessage(ctx, "action").getString();
                    String name = src.getPlayer() != null
                        ? src.getPlayer().getName().getString() : "Console";
                    Component msg = MessageUtil.coloredText("§5* §d" + name + " §f" + action);
                    src.getServer().getPlayerList().getPlayers().forEach(p -> p.sendSystemMessage(msg));
                    src.getServer().sendSystemMessage(msg);
                    return 1;
                })
            )
        );
    }

    // ── /tptoggle [on|off] [player] ───────────────────────────────────────────
    // Essentials: user.setTeleportEnabled(). Our /tpa checks this via isTpToggleAllowed().
    private static void registerTpToggle(CommandDispatcher<CommandSourceStack> d) {
        d.register(Commands.literal("tptoggle")
            .requires(src -> {
                var p = src.getPlayer();
                return p == null || PermissionAPI.hasPermission(p.getUUID(), "neoessentials.tptoggle");
            })
            .executes(ctx -> executeTpToggle(ctx, null, null))
            .then(Commands.literal("on").executes(ctx -> executeTpToggle(ctx, null, true)))
            .then(Commands.literal("off").executes(ctx -> executeTpToggle(ctx, null, false)))
            .then(Commands.argument("target", StringArgumentType.word())
                .suggests((ctx, b) -> SharedSuggestionProvider.suggest(ctx.getSource().getServer().getPlayerNames(), b))
                .requires(src -> src.getPlayer() == null || PermissionAPI.hasPermission(src.getPlayer().getUUID(), "neoessentials.tptoggle.others"))
                .executes(ctx -> executeTpToggle(ctx, StringArgumentType.getString(ctx, "target"), null))
                .then(Commands.literal("on").executes(ctx -> executeTpToggle(ctx, StringArgumentType.getString(ctx, "target"), true)))
                .then(Commands.literal("off").executes(ctx -> executeTpToggle(ctx, StringArgumentType.getString(ctx, "target"), false)))
            )
        );
    }

    private static int executeTpToggle(CommandContext<CommandSourceStack> ctx, String targetName, Boolean enable) {
        var src = ctx.getSource();
        ServerPlayer target = resolveTarget(src, targetName);
        if (target == null) return 0;
        boolean cur = tpToggleState.getOrDefault(target.getUUID(), true);
        boolean newState = enable != null ? enable : !cur;
        tpToggleState.put(target.getUUID(), newState);
        String label = newState ? "§aenabled" : "§cdisabled";
        boolean isOther = src.getPlayer() == null || !src.getPlayer().getUUID().equals(target.getUUID());
        if (isOther) {
            src.sendSuccess(() -> MessageUtil.success("commands.neoessentials.tptoggle.other",
                target.getName().getString(), label), false);
            target.sendSystemMessage(MessageUtil.info("commands.neoessentials.tptoggle.self", label));
        } else {
            src.sendSuccess(() -> MessageUtil.success("commands.neoessentials.tptoggle.self", label), false);
        }
        return 1;
    }

    /** Returns true if the player is currently accepting teleport requests. */
    public static boolean isTpToggleAllowed(UUID uuid) {
        return tpToggleState.getOrDefault(uuid, true);
    }

    // ── /gc — server info (memory, TPS, uptime) ───────────────────────────────
    private static void registerGc(CommandDispatcher<CommandSourceStack> d) {
        d.register(Commands.literal("gc")
            .requires(src -> { var p = src.getPlayer(); return p == null || PermissionAPI.hasPermission(p.getUUID(), "neoessentials.gc"); })
            .executes(ctx -> executeGc(ctx))
        );
        // alias /mem — just re-runs the gc executor
        d.register(Commands.literal("mem")
            .requires(src -> { var p = src.getPlayer(); return p == null || PermissionAPI.hasPermission(p.getUUID(), "neoessentials.gc"); })
            .executes(ctx -> executeGc(ctx))
        );
    }

    private static int executeGc(CommandContext<CommandSourceStack> ctx) {
        var src = ctx.getSource();
        Runtime rt = Runtime.getRuntime();
        long maxMb = rt.maxMemory() / 1024 / 1024;
        long totalMb = rt.totalMemory() / 1024 / 1024;
        long freeMb = rt.freeMemory() / 1024 / 1024;
        long usedMb = totalMb - freeMb;
        long uptimeMs = java.lang.management.ManagementFactory.getRuntimeMXBean().getUptime();
        String uptime = formatUptime(uptimeMs);
        // NeoForge 1.21.1: use server.getAverageTickTimeNanos() for TPS
        double tps = 20.0;
        try {
            var server = src.getServer();
            // getAverageTickTimeNanos returns nanoseconds per tick average
            double avgNs = server.getAverageTickTimeNanos();
            if (avgNs > 0) {
                double avgMs = avgNs / 1_000_000.0;
                tps = Math.min(20.0, 1000.0 / avgMs);
            }
        } catch (Exception ignored) {}
        String tpsColor = tps >= 18 ? "§a" : tps >= 15 ? "§e" : "§c";
        int loaded = 0;
        for (var level : src.getServer().getAllLevels()) loaded += level.getChunkSource().getLoadedChunksCount();
        final double ftps = Math.round(tps * 100.0) / 100.0;
        final int fChunks = loaded;
        final String fUptime = uptime;
        final long fUsed = usedMb, fTotal = totalMb, fMax = maxMb;
        final String fTpsColor = tpsColor;
        src.sendSuccess(() -> MessageUtil.info("commands.neoessentials.gc.info",
            fUptime, fTpsColor + ftps, fUsed, fTotal, fMax, fChunks), false);
        return 1;
    }

    private static String formatUptime(long ms) {
        long s = ms / 1000, m = s / 60, h = m / 60, d = h / 24;
        if (d > 0) return d + "d " + (h % 24) + "h " + (m % 60) + "m";
        if (h > 0) return h + "h " + (m % 60) + "m " + (s % 60) + "s";
        if (m > 0) return m + "m " + (s % 60) + "s";
        return s + "s";
    }

    // ── /lightning [player] / /smite [player] ─────────────────────────────────
    // Essentials: strikes lightning at target's location or sender's look-target.
    private static void registerLightning(CommandDispatcher<CommandSourceStack> d) {
        d.register(Commands.literal("lightning")
            .requires(src -> { var p = src.getPlayer(); return p == null || PermissionAPI.hasPermission(p.getUUID(), "neoessentials.lightning"); })
            .executes(ctx -> executeLightning(ctx, null))
            .then(Commands.argument("target", StringArgumentType.word())
                .suggests((ctx, b) -> SharedSuggestionProvider.suggest(ctx.getSource().getServer().getPlayerNames(), b))
                .requires(src -> src.getPlayer() == null || PermissionAPI.hasPermission(src.getPlayer().getUUID(), "neoessentials.lightning.others"))
                .executes(ctx -> executeLightning(ctx, StringArgumentType.getString(ctx, "target")))
            )
        );
        // alias /smite
        d.register(Commands.literal("smite")
            .requires(src -> { var p = src.getPlayer(); return p == null || PermissionAPI.hasPermission(p.getUUID(), "neoessentials.lightning"); })
            .executes(ctx -> executeLightning(ctx, null))
            .then(Commands.argument("target", StringArgumentType.word())
                .suggests((ctx, b) -> SharedSuggestionProvider.suggest(ctx.getSource().getServer().getPlayerNames(), b))
                .requires(src -> src.getPlayer() == null || PermissionAPI.hasPermission(src.getPlayer().getUUID(), "neoessentials.lightning.others"))
                .executes(ctx -> executeLightning(ctx, StringArgumentType.getString(ctx, "target")))
            )
        );
    }

    private static int executeLightning(CommandContext<CommandSourceStack> ctx, String targetName) {
        var src = ctx.getSource();
        if (targetName != null) {
            // Strike at named player
            ServerPlayer target = src.getServer().getPlayerList().getPlayerByName(targetName);
            if (target == null) {
                src.sendFailure(MessageUtil.error("commands.neoessentials.general.player_not_found", targetName));
                return 0;
            }
            strikeLightning(com.zerog.neoessentials.util.LevelCompat.of(target), target.getX(), target.getY(), target.getZ());
            src.sendSuccess(() -> MessageUtil.success("commands.neoessentials.lightning.struck", targetName), true);
        } else {
            // Strike at self's look target
            var self = src.getPlayer();
            if (self == null) { src.sendFailure(MessageUtil.error("commands.neoessentials.general.player_only")); return 0; }
            var hit = self.pick(100, 1.0f, false);
            var pos = hit.getLocation();
            strikeLightning(com.zerog.neoessentials.util.LevelCompat.of(self), pos.x, pos.y, pos.z);
            src.sendSuccess(() -> MessageUtil.success("commands.neoessentials.lightning.self"), false);
        }
        return 1;
    }

    private static void strikeLightning(net.minecraft.server.level.ServerLevel level, double x, double y, double z) {
        LightningBolt bolt = com.zerog.neoessentials.util.EntityTypeCompat.create(EntityType.LIGHTNING_BOLT, level);
        if (bolt != null) {
            bolt.snapTo(x, y, z);
            level.addFreshEntity(bolt);
        }
    }

    // ── /skull [player] ───────────────────────────────────────────────────────
    // Essentials: gives a player head with the specified player's texture.
    private static void registerSkull(CommandDispatcher<CommandSourceStack> d) {
        d.register(Commands.literal("skull")
            .requires(src -> { var p = src.getPlayer(); return p == null || PermissionAPI.hasPermission(p.getUUID(), "neoessentials.skull"); })
            .executes(ctx -> executeSkull(ctx, null))
            .then(Commands.argument("player", StringArgumentType.word())
                .suggests((ctx, b) -> SharedSuggestionProvider.suggest(ctx.getSource().getServer().getPlayerNames(), b))
                .executes(ctx -> executeSkull(ctx, StringArgumentType.getString(ctx, "player")))
            )
        );
    }

    private static int executeSkull(CommandContext<CommandSourceStack> ctx, String targetName) {
        var src = ctx.getSource();
        var self = src.getPlayer();
        if (self == null) { src.sendFailure(MessageUtil.error("commands.neoessentials.general.player_only")); return 0; }
        String ownerName = targetName != null ? targetName : self.getName().getString();

        ItemStack skull = new ItemStack(Items.PLAYER_HEAD);
        // 26.1 port: GameProfileCache was replaced by Services.profileResolver(), which
        // resolves the full GameProfile (including textures) by name in one call; and
        // ResolvableProfile is now abstract/sealed, constructed via createResolved(...).
        src.getServer().services().profileResolver().fetchByName(ownerName)
            .ifPresent(profile -> skull.set(DataComponents.PROFILE,
                net.minecraft.world.item.component.ResolvableProfile.createResolved(profile)));
        // If not resolvable, at minimum set the name so it resolves on hover
        if (!skull.has(DataComponents.PROFILE)) {
            skull.set(DataComponents.PROFILE,
                net.minecraft.world.item.component.ResolvableProfile.createResolved(
                    new com.mojang.authlib.GameProfile(UUID.randomUUID(), ownerName)));
        }

        if (!self.getInventory().add(skull)) self.drop(skull, false);
        final String fn = ownerName;
        src.sendSuccess(() -> MessageUtil.success("commands.neoessentials.skull.success", fn), false);
        return 1;
    }

    // ── /itemname [name|-] ────────────────────────────────────────────────────
    // Essentials: renames the held item. "-" or empty clears the name.
    private static void registerItemName(CommandDispatcher<CommandSourceStack> d) {
        d.register(Commands.literal("itemname")
            .requires(src -> { var p = src.getPlayer(); return p == null || PermissionAPI.hasPermission(p.getUUID(), "neoessentials.itemname"); })
            // /itemname  — clear name
            .executes(ctx -> executeItemName(ctx, null))
            // /itemname <name>
            .then(Commands.argument("name", StringArgumentType.greedyString())
                .executes(ctx -> executeItemName(ctx, StringArgumentType.getString(ctx, "name")))
            )
        );
        // alias /rename
        d.register(Commands.literal("rename")
            .requires(src -> { var p = src.getPlayer(); return p == null || PermissionAPI.hasPermission(p.getUUID(), "neoessentials.itemname"); })
            .executes(ctx -> executeItemName(ctx, null))
            .then(Commands.argument("name", StringArgumentType.greedyString())
                .executes(ctx -> executeItemName(ctx, StringArgumentType.getString(ctx, "name")))
            )
        );
    }

    private static int executeItemName(CommandContext<CommandSourceStack> ctx, String name) {
        var src = ctx.getSource();
        var player = src.getPlayer();
        if (player == null) { src.sendFailure(MessageUtil.error("commands.neoessentials.general.player_only")); return 0; }
        ItemStack held = player.getMainHandItem();
        if (held.isEmpty()) { src.sendFailure(MessageUtil.error("commands.neoessentials.itemname.no_item")); return 0; }
        // Clear name
        if (name == null || name.equals("-") || name.isBlank()) {
            held.remove(DataComponents.CUSTOM_NAME);
            src.sendSuccess(() -> MessageUtil.success("commands.neoessentials.itemname.cleared"), false);
        } else {
            held.set(DataComponents.CUSTOM_NAME, MessageUtil.coloredText(name));
            src.sendSuccess(() -> MessageUtil.success("commands.neoessentials.itemname.set", name), false);
        }
        return 1;
    }

    // ── /itemlore add|set <line> <text>|clear|remove <line> ──────────────────
    private static void registerItemLore(CommandDispatcher<CommandSourceStack> d) {
        d.register(Commands.literal("itemlore")
            .requires(src -> { var p = src.getPlayer(); return p == null || PermissionAPI.hasPermission(p.getUUID(), "neoessentials.itemlore"); })
            // /itemlore add <text>
            .then(Commands.literal("add")
                .then(Commands.argument("text", StringArgumentType.greedyString())
                    .executes(ctx -> executeItemLore(ctx, "add", -1, StringArgumentType.getString(ctx, "text")))
                )
            )
            // /itemlore set <line> <text>
            .then(Commands.literal("set")
                .then(Commands.argument("line", IntegerArgumentType.integer(1))
                    .then(Commands.argument("text", StringArgumentType.greedyString())
                        .executes(ctx -> executeItemLore(ctx, "set",
                            IntegerArgumentType.getInteger(ctx, "line"),
                            StringArgumentType.getString(ctx, "text")))
                    )
                )
            )
            // /itemlore remove <line>
            .then(Commands.literal("remove")
                .then(Commands.argument("line", IntegerArgumentType.integer(1))
                    .executes(ctx -> executeItemLore(ctx, "remove",
                        IntegerArgumentType.getInteger(ctx, "line"), null))
                )
            )
            // /itemlore clear
            .then(Commands.literal("clear")
                .executes(ctx -> executeItemLore(ctx, "clear", -1, null))
            )
        );
    }

    private static int executeItemLore(CommandContext<CommandSourceStack> ctx, String action, int line, String text) {
        var src = ctx.getSource();
        var player = src.getPlayer();
        if (player == null) { src.sendFailure(MessageUtil.error("commands.neoessentials.general.player_only")); return 0; }
        ItemStack held = player.getMainHandItem();
        if (held.isEmpty()) { src.sendFailure(MessageUtil.error("commands.neoessentials.itemlore.no_item")); return 0; }

        // Get existing lore list (mutable)
        ItemLore existing = held.getOrDefault(DataComponents.LORE, ItemLore.EMPTY);
        List<Component> lore = new ArrayList<>(existing.lines());

        switch (action) {
            case "add" -> {
                lore.add(MessageUtil.coloredText("§r" + text));
                src.sendSuccess(() -> MessageUtil.success("commands.neoessentials.itemlore.added", text), false);
            }
            case "set" -> {
                int idx = line - 1;
                if (idx < 0 || idx >= lore.size()) {
                    src.sendFailure(MessageUtil.error("commands.neoessentials.itemlore.invalid_line", line));
                    return 0;
                }
                lore.set(idx, MessageUtil.coloredText("§r" + text));
                src.sendSuccess(() -> MessageUtil.success("commands.neoessentials.itemlore.set", line, text), false);
            }
            case "remove" -> {
                int idx = line - 1;
                if (idx < 0 || idx >= lore.size()) {
                    src.sendFailure(MessageUtil.error("commands.neoessentials.itemlore.invalid_line", line));
                    return 0;
                }
                lore.remove(idx);
                src.sendSuccess(() -> MessageUtil.success("commands.neoessentials.itemlore.removed", line), false);
            }
            case "clear" -> {
                lore.clear();
                src.sendSuccess(() -> MessageUtil.success("commands.neoessentials.itemlore.cleared"), false);
            }
        }

        held.set(DataComponents.LORE, new ItemLore(Collections.unmodifiableList(lore)));
        return 1;
    }

    // ── /remove <type> [radius] ───────────────────────────────────────────────
    // Types: all, items, mobs, animals, monsters, arrows, xp, paintings, boats, minecarts
    private static void registerRemove(CommandDispatcher<CommandSourceStack> d) {
        d.register(Commands.literal("remove")
            .requires(src -> { var p = src.getPlayer(); return p == null || PermissionAPI.hasPermission(p.getUUID(), "neoessentials.remove"); })
            .then(Commands.argument("type", StringArgumentType.word())
                .suggests((ctx, b) -> SharedSuggestionProvider.suggest(
                    List.of("all","items","drops","mobs","animals","monsters",
                        "arrows","xp","paintings","boats","minecarts","tnt","boats"), b))
                .executes(ctx -> executeRemove(ctx, StringArgumentType.getString(ctx, "type"), 200))
                .then(Commands.argument("radius", IntegerArgumentType.integer(1, 10000))
                    .executes(ctx -> executeRemove(ctx,
                        StringArgumentType.getString(ctx, "type"),
                        IntegerArgumentType.getInteger(ctx, "radius")))
                )
            )
        );
    }

    private static int executeRemove(CommandContext<CommandSourceStack> ctx, String type, int radius) {
        var src = ctx.getSource();
        var player = src.getPlayer();
        if (player == null) { src.sendFailure(MessageUtil.error("commands.neoessentials.general.player_only")); return 0; }

        var level = com.zerog.neoessentials.util.LevelCompat.of(player);
        var pos = player.blockPosition();
        int removed = 0;

        for (Entity entity : level.getEntities(null, player.getBoundingBox().inflate(radius))) {
            if (entity instanceof ServerPlayer) continue; // never remove players
            boolean match = switch (type.toLowerCase()) {
                case "all" -> true;
                case "items", "drops" -> entity instanceof net.minecraft.world.entity.item.ItemEntity;
                case "mobs" -> entity instanceof net.minecraft.world.entity.Mob;
                case "animals" -> entity instanceof net.minecraft.world.entity.animal.Animal;
                case "monsters" -> entity instanceof net.minecraft.world.entity.monster.Monster;
                case "arrows" -> entity instanceof net.minecraft.world.entity.projectile.arrow.Arrow;
                case "xp" -> entity instanceof net.minecraft.world.entity.ExperienceOrb;
                case "boats" -> entity instanceof net.minecraft.world.entity.vehicle.boat.Boat;
                case "minecarts" -> entity instanceof net.minecraft.world.entity.vehicle.minecart.AbstractMinecart;
                case "tnt" -> entity instanceof net.minecraft.world.entity.item.PrimedTnt;
                case "paintings" -> entity instanceof net.minecraft.world.entity.decoration.painting.Painting;
                default -> false;
            };
            if (match) { entity.discard(); removed++; }
        }

        final int fr = removed;
        src.sendSuccess(() -> MessageUtil.success("commands.neoessentials.remove.success", fr, type, radius), true);
        LOGGER.info("{} removed {} {} entities within {}r", senderName(src), fr, type, radius);
        return 1;
    }

    // ── /loom ─────────────────────────────────────────────────────────────────
    private static void registerLoom(CommandDispatcher<CommandSourceStack> d) {
        d.register(Commands.literal("loom")
            .requires(src -> { var p = src.getPlayer(); return p == null || PermissionAPI.hasPermission(p.getUUID(), "neoessentials.loom"); })
            .executes(ctx -> {
                var src = ctx.getSource();
                var player = src.getPlayer();
                if (player == null) { src.sendFailure(MessageUtil.error("commands.neoessentials.general.player_only")); return 0; }
                player.openMenu(new MenuProvider() {
                    @Override @Nonnull public Component getDisplayName() { return Component.literal("Loom"); }
                    @Override @Nonnull public AbstractContainerMenu createMenu(int id, @Nonnull Inventory inv, @Nonnull Player p) {
                        return new LoomMenu(id, inv, ContainerLevelAccess.create(p.level(), p.blockPosition()));
                    }
                });
                src.sendSuccess(() -> MessageUtil.success("commands.neoessentials.loom.opened"), false);
                return 1;
            })
        );
    }

    // ── /cartography ──────────────────────────────────────────────────────────
    private static void registerCartography(CommandDispatcher<CommandSourceStack> d) {
        d.register(Commands.literal("cartography")
            .requires(src -> { var p = src.getPlayer(); return p == null || PermissionAPI.hasPermission(p.getUUID(), "neoessentials.cartography"); })
            .executes(ctx -> openCartography(ctx))
        );
        d.register(Commands.literal("cartographytable")
            .requires(src -> { var p = src.getPlayer(); return p == null || PermissionAPI.hasPermission(p.getUUID(), "neoessentials.cartography"); })
            .executes(ctx -> openCartography(ctx))
        );
    }

    private static int openCartography(CommandContext<CommandSourceStack> ctx) {
        var src = ctx.getSource();
        var player = src.getPlayer();
        if (player == null) { src.sendFailure(MessageUtil.error("commands.neoessentials.general.player_only")); return 0; }
        player.openMenu(new MenuProvider() {
            @Override @Nonnull public Component getDisplayName() { return Component.literal("Cartography Table"); }
            @Override @Nonnull public AbstractContainerMenu createMenu(int id, @Nonnull Inventory inv, @Nonnull Player p) {
                return new CartographyTableMenu(id, inv, ContainerLevelAccess.create(p.level(), p.blockPosition()));
            }
        });
        src.sendSuccess(() -> MessageUtil.success("commands.neoessentials.cartography.opened"), false);
        return 1;
    }

    // ── Helpers ───────────────────────────────────────────────────────────────
    private static ServerPlayer resolveTarget(CommandSourceStack src, String targetName) {
        if (targetName != null) {
            ServerPlayer p = src.getServer().getPlayerList().getPlayerByName(targetName);
            if (p == null) src.sendFailure(MessageUtil.error("commands.neoessentials.general.player_not_found", targetName));
            return p;
        }
        ServerPlayer self = src.getPlayer();
        if (self == null) src.sendFailure(MessageUtil.error("commands.neoessentials.general.player_only"));
        return self;
    }

    private static String senderName(CommandSourceStack src) {
        return src.getPlayer() != null ? src.getPlayer().getName().getString() : "Console";
    }
}







