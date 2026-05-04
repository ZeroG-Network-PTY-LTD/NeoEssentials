package com.zerog.neoessentials.hologram.command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.zerog.neoessentials.api.permissions.PermissionAPI;
import com.zerog.neoessentials.hologram.*;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import java.util.Collection;
import java.util.UUID;
/**
 * /hologram (alias /holo) — admin command for managing holographic displays.
 *
 * Subcommands:
 *   create <id> <x> <y> <z> [world]
 *   delete <id>
 *   addline <id> <text...>
 *   setline <id> <index> <text...>
 *   removeline <id> <index>
 *   moveto <id> <x> <y> <z>
 *   setrefresh <id> <seconds>
 *   toggle <id>
 *   list
 *   info <id>
 *   reload
 */
public class HologramCommand {
    private static final String PERM = "neoessentials.hologram.admin";
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        var root = Commands.literal("hologram")
            .requires(src -> hasPermission(src))
            .then(Commands.literal("create")
                .then(Commands.argument("id", StringArgumentType.word())
                    .then(Commands.argument("x", DoubleArgumentType.doubleArg())
                        .then(Commands.argument("y", DoubleArgumentType.doubleArg())
                            .then(Commands.argument("z", DoubleArgumentType.doubleArg())
                                .executes(ctx -> cmdCreate(ctx.getSource(),
                                    StringArgumentType.getString(ctx, "id"),
                                    DoubleArgumentType.getDouble(ctx, "x"),
                                    DoubleArgumentType.getDouble(ctx, "y"),
                                    DoubleArgumentType.getDouble(ctx, "z"),
                                    null))
                                .then(Commands.argument("world", StringArgumentType.word())
                                    .executes(ctx -> cmdCreate(ctx.getSource(),
                                        StringArgumentType.getString(ctx, "id"),
                                        DoubleArgumentType.getDouble(ctx, "x"),
                                        DoubleArgumentType.getDouble(ctx, "y"),
                                        DoubleArgumentType.getDouble(ctx, "z"),
                                        StringArgumentType.getString(ctx, "world")))))))))
            .then(Commands.literal("delete")
                .then(Commands.argument("id", StringArgumentType.word())
                    .executes(ctx -> cmdDelete(ctx.getSource(), StringArgumentType.getString(ctx, "id")))))
            .then(Commands.literal("addline")
                .then(Commands.argument("id", StringArgumentType.word())
                    .then(Commands.argument("text", StringArgumentType.greedyString())
                        .executes(ctx -> cmdAddLine(ctx.getSource(),
                            StringArgumentType.getString(ctx, "id"),
                            StringArgumentType.getString(ctx, "text"))))))
            .then(Commands.literal("setline")
                .then(Commands.argument("id", StringArgumentType.word())
                    .then(Commands.argument("index", IntegerArgumentType.integer(0))
                        .then(Commands.argument("text", StringArgumentType.greedyString())
                            .executes(ctx -> cmdSetLine(ctx.getSource(),
                                StringArgumentType.getString(ctx, "id"),
                                IntegerArgumentType.getInteger(ctx, "index"),
                                StringArgumentType.getString(ctx, "text")))))))
            .then(Commands.literal("removeline")
                .then(Commands.argument("id", StringArgumentType.word())
                    .then(Commands.argument("index", IntegerArgumentType.integer(0))
                        .executes(ctx -> cmdRemoveLine(ctx.getSource(),
                            StringArgumentType.getString(ctx, "id"),
                            IntegerArgumentType.getInteger(ctx, "index"))))))
            .then(Commands.literal("moveto")
                .then(Commands.argument("id", StringArgumentType.word())
                    .then(Commands.argument("x", DoubleArgumentType.doubleArg())
                        .then(Commands.argument("y", DoubleArgumentType.doubleArg())
                            .then(Commands.argument("z", DoubleArgumentType.doubleArg())
                                .executes(ctx -> cmdMoveTo(ctx.getSource(),
                                    StringArgumentType.getString(ctx, "id"),
                                    DoubleArgumentType.getDouble(ctx, "x"),
                                    DoubleArgumentType.getDouble(ctx, "y"),
                                    DoubleArgumentType.getDouble(ctx, "z"))))))))
            .then(Commands.literal("setrefresh")
                .then(Commands.argument("id", StringArgumentType.word())
                    .then(Commands.argument("seconds", IntegerArgumentType.integer(0))
                        .executes(ctx -> cmdSetRefresh(ctx.getSource(),
                            StringArgumentType.getString(ctx, "id"),
                            IntegerArgumentType.getInteger(ctx, "seconds"))))))
            .then(Commands.literal("toggle")
                .then(Commands.argument("id", StringArgumentType.word())
                    .executes(ctx -> cmdToggle(ctx.getSource(), StringArgumentType.getString(ctx, "id")))))
            .then(Commands.literal("list")
                .executes(ctx -> cmdList(ctx.getSource())))
            .then(Commands.literal("info")
                .then(Commands.argument("id", StringArgumentType.word())
                    .executes(ctx -> cmdInfo(ctx.getSource(), StringArgumentType.getString(ctx, "id")))))
            .then(Commands.literal("reload")
                .executes(ctx -> cmdReload(ctx.getSource())));
        dispatcher.register(root);
        // Alias /holo
        dispatcher.register(Commands.literal("holo")
            .requires(src -> hasPermission(src))
            .redirect(dispatcher.getRoot().getChild("hologram")));
    }
    // ── Subcommand implementations ─────────────────────────────────────────────
    private static int cmdCreate(CommandSourceStack src, String id, double x, double y, double z, String worldArg) {
        try {
            if (HologramManager.getInstance().exists(id)) {
                src.sendFailure(Component.literal("§cHologram '§e" + id + "§c' already exists."));
                return 0;
            }
            HologramData data = new HologramData();
            data.id = id.toLowerCase();
            data.x = x; data.y = y; data.z = z;
            data.world = resolveWorld(src, worldArg);
            data.refreshInterval = 5;
            HologramManager.getInstance().registerHologram(data);
            // Spawn in appropriate level
            ServerLevel level = getLevel(src, data.world);
            if (level != null) HologramRenderer.spawn(data, level);
            src.sendSuccess(() -> Component.literal("§a✓ Hologram '§e" + id + "§a' created at §7(" + fmt(x) + ", " + fmt(y) + ", " + fmt(z) + ")§a in §7" + data.world + "§a.\n§7Use §f/hologram addline " + id + " <text> §7to add lines."), true);
        } catch (Exception e) {
            src.sendFailure(Component.literal("§cError: " + e.getMessage()));
        }
        return 1;
    }
    private static int cmdDelete(CommandSourceStack src, String id) {
        HologramData data = HologramManager.getInstance().getHologram(id);
        if (data == null) { src.sendFailure(Component.literal("§cHologram '§e" + id + "§c' not found.")); return 0; }
        ServerLevel level = getLevel(src, data.world);
        if (level != null) HologramRenderer.despawn(data, level);
        HologramManager.getInstance().removeHologram(id);
        src.sendSuccess(() -> Component.literal("§a✓ Hologram '§e" + id + "§a' deleted."), true);
        return 1;
    }
    private static int cmdAddLine(CommandSourceStack src, String id, String text) {
        HologramData data = HologramManager.getInstance().getHologram(id);
        if (data == null) { src.sendFailure(Component.literal("§cHologram '§e" + id + "§c' not found.")); return 0; }
        data.lines.add(new HologramLine(text));
        HologramManager.getInstance().registerHologram(data);
        respawn(src, data);
        src.sendSuccess(() -> Component.literal("§a✓ Line added to '§e" + id + "§a'. Total lines: " + data.lines.size()), true);
        return 1;
    }
    private static int cmdSetLine(CommandSourceStack src, String id, int index, String text) {
        HologramData data = HologramManager.getInstance().getHologram(id);
        if (data == null) { src.sendFailure(Component.literal("§cHologram '§e" + id + "§c' not found.")); return 0; }
        if (index < 0 || index >= data.lines.size()) { src.sendFailure(Component.literal("§cLine index out of range (0–" + (data.lines.size()-1) + ").")); return 0; }
        data.lines.get(index).text = text;
        HologramManager.getInstance().registerHologram(data);
        respawn(src, data);
        src.sendSuccess(() -> Component.literal("§a✓ Line §e" + index + "§a updated."), true);
        return 1;
    }
    private static int cmdRemoveLine(CommandSourceStack src, String id, int index) {
        HologramData data = HologramManager.getInstance().getHologram(id);
        if (data == null) { src.sendFailure(Component.literal("§cHologram '§e" + id + "§c' not found.")); return 0; }
        if (index < 0 || index >= data.lines.size()) { src.sendFailure(Component.literal("§cLine index out of range.")); return 0; }
        data.lines.remove(index);
        HologramManager.getInstance().registerHologram(data);
        respawn(src, data);
        src.sendSuccess(() -> Component.literal("§a✓ Line §e" + index + "§a removed."), true);
        return 1;
    }
    private static int cmdMoveTo(CommandSourceStack src, String id, double x, double y, double z) {
        HologramData data = HologramManager.getInstance().getHologram(id);
        if (data == null) { src.sendFailure(Component.literal("§cHologram '§e" + id + "§c' not found.")); return 0; }
        ServerLevel level = getLevel(src, data.world);
        if (level != null) HologramRenderer.despawn(data, level);
        data.x = x; data.y = y; data.z = z;
        HologramManager.getInstance().registerHologram(data);
        if (level != null) HologramRenderer.spawn(data, level);
        src.sendSuccess(() -> Component.literal("§a✓ Hologram '§e" + id + "§a' moved to §7(" + fmt(x) + ", " + fmt(y) + ", " + fmt(z) + ")§a."), true);
        return 1;
    }
    private static int cmdSetRefresh(CommandSourceStack src, String id, int seconds) {
        HologramData data = HologramManager.getInstance().getHologram(id);
        if (data == null) { src.sendFailure(Component.literal("§cHologram '§e" + id + "§c' not found.")); return 0; }
        data.refreshInterval = seconds;
        HologramManager.getInstance().registerHologram(data);
        String msg = seconds == 0 ? "§a✓ Refresh disabled for '§e" + id + "§a'." : "§a✓ Refresh for '§e" + id + "§a' set to §e" + seconds + "s§a.";
        src.sendSuccess(() -> Component.literal(msg), true);
        return 1;
    }
    private static int cmdToggle(CommandSourceStack src, String id) {
        HologramData data = HologramManager.getInstance().getHologram(id);
        if (data == null) { src.sendFailure(Component.literal("§cHologram '§e" + id + "§c' not found.")); return 0; }
        data.visible = !data.visible;
        HologramManager.getInstance().registerHologram(data);
        ServerLevel level = getLevel(src, data.world);
        if (level != null) {
            if (data.visible) HologramRenderer.spawn(data, level);
            else HologramRenderer.despawn(data, level);
        }
        boolean v = data.visible;
        src.sendSuccess(() -> Component.literal("§aHologram '§e" + id + "§a' is now " + (v ? "§2visible" : "§7hidden") + "§a."), true);
        return 1;
    }
    private static int cmdList(CommandSourceStack src) {
        Collection<HologramData> all = HologramManager.getInstance().getAllHolograms();
        if (all.isEmpty()) { src.sendSuccess(() -> Component.literal("§7No holograms found."), false); return 1; }
        StringBuilder sb = new StringBuilder("§6Holograms (").append(all.size()).append("):\n");
        for (HologramData d : all) {
            sb.append("  §e").append(d.id).append(" §7— §f").append(d.world)
              .append(" §7(").append(fmt(d.x)).append(", ").append(fmt(d.y)).append(", ").append(fmt(d.z))
              .append(") §7lines=§f").append(d.lines.size())
              .append(d.visible ? "" : " §8[hidden]").append("\n");
        }
        src.sendSuccess(() -> Component.literal(sb.toString().trim()), false);
        return 1;
    }
    private static int cmdInfo(CommandSourceStack src, String id) {
        HologramData data = HologramManager.getInstance().getHologram(id);
        if (data == null) { src.sendFailure(Component.literal("§cHologram '§e" + id + "§c' not found.")); return 0; }
        StringBuilder sb = new StringBuilder();
        sb.append("§6=== Hologram: §e").append(data.id).append(" §6===\n");
        sb.append("§7World: §f").append(data.world).append("\n");
        sb.append("§7Position: §f(").append(fmt(data.x)).append(", ").append(fmt(data.y)).append(", ").append(fmt(data.z)).append(")\n");
        sb.append("§7Visible: ").append(data.visible ? "§ayes" : "§cno").append("\n");
        sb.append("§7Refresh: §f").append(data.refreshInterval == 0 ? "disabled" : data.refreshInterval + "s").append("\n");
        sb.append("§7Lines (").append(data.lines.size()).append("):\n");
        for (int i = 0; i < data.lines.size(); i++) {
            HologramLine line = data.lines.get(i);
            sb.append("  §e").append(i).append("§7: §f").append(line.currentText());
            if (!line.frames.isEmpty()) sb.append(" §8[animated, ").append(line.frames.size()).append(" frames]");
            sb.append("\n");
        }
        src.sendSuccess(() -> Component.literal(sb.toString().trim()), false);
        return 1;
    }
    private static int cmdReload(CommandSourceStack src) {
        try {
            // Despawn all
            net.minecraft.server.MinecraftServer server = src.getServer();
            for (ServerLevel level : server.getAllLevels()) {
                String dimKey = HologramRenderer.dimensionKey(level);
                HologramRenderer.despawnAllForWorld(level, dimKey);
            }
            // Reload from disk
            HologramManager.getInstance().initialize();
            // Respawn all
            for (ServerLevel level : server.getAllLevels()) {
                String dimKey = HologramRenderer.dimensionKey(level);
                HologramRenderer.spawnAllForWorld(level, dimKey);
            }
            src.sendSuccess(() -> Component.literal("§a✓ Holograms reloaded. " + HologramManager.getInstance().getAllHolograms().size() + " hologram(s) active."), true);
        } catch (Exception e) {
            src.sendFailure(Component.literal("§cReload failed: " + e.getMessage()));
        }
        return 1;
    }
    // ── Helpers ───────────────────────────────────────────────────────────────
    private static boolean hasPermission(CommandSourceStack src) {
        try {
            if (src.getEntity() instanceof ServerPlayer player) {
                return player.hasPermissions(4) ||
                    PermissionAPI.hasPermission(player.getUUID(), PERM) ||
                    PermissionAPI.hasPermission(player.getUUID(), "neoessentials.admin.*");
            }
            return true; // console
        } catch (Exception e) { return true; }
    }
    private static String resolveWorld(CommandSourceStack src, String worldArg) {
        if (worldArg != null && !worldArg.isEmpty()) {
            if (!worldArg.contains(":")) worldArg = "minecraft:" + worldArg;
            return worldArg;
        }
        if (src.getLevel() instanceof ServerLevel level) return HologramRenderer.dimensionKey(level);
        return "minecraft:overworld";
    }
    private static ServerLevel getLevel(CommandSourceStack src, String dimensionKey) {
        try {
            for (ServerLevel level : src.getServer().getAllLevels()) {
                if (HologramRenderer.dimensionKey(level).equals(dimensionKey)) return level;
            }
        } catch (Exception ignored) {}
        return null;
    }
    private static void respawn(CommandSourceStack src, HologramData data) {
        try {
            ServerLevel level = getLevel(src, data.world);
            if (level != null) HologramRenderer.spawn(data, level);
        } catch (Exception ignored) {}
    }
    private static String fmt(double v) {
        return String.format("%.1f", v);
    }
}
