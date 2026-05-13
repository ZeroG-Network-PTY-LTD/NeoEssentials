package com.zerog.neoessentials.hologram.command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.zerog.neoessentials.api.permissions.PermissionAPI;
import com.zerog.neoessentials.hologram.*;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
/**
 * /hologram (alias /holo) â€” admin command for managing holographic displays.
 *
 * Subcommands:
 *   create <id> <x> <y> <z> [world]
 *   delete <id>
 *   copy <id> <newid>
 *   addline <id> <text...>
 *   insertline <id> <index> <text...>
 *   setline <id> <index> <text...>
 *   removeline <id> <index>
 *   addframes <id> <lineIndex> <intervalTicks> <frame1|frame2|...>
 *   removeframes <id> <lineIndex>
 *   moveto <id> <x> <y> <z>
 *   movehere <id>
 *   near [radius]
 *   setrefresh <id> <seconds>
 *   toggle <id>
 *   billboard <id> <fixed|vertical|horizontal|center>
 *   spin <id> <on|off> [speed] [axis]
 *   hover <id> <on|off> [amplitude] [speed]
 *   scale <id> <scale>
 *   linespacing <id> <spacing>
 *   shadow <id> <on|off>
 *   opacity <id> <0-255>
 *   background <id> <#RRGGBB:AA|transparent>
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
            .then(Commands.literal("copy")
                .then(Commands.argument("id", StringArgumentType.word())
                    .then(Commands.argument("newid", StringArgumentType.word())
                        .executes(ctx -> cmdCopy(ctx.getSource(),
                            StringArgumentType.getString(ctx, "id"),
                            StringArgumentType.getString(ctx, "newid"))))))
            .then(Commands.literal("addline")
                .then(Commands.argument("id", StringArgumentType.word())
                    .then(Commands.argument("text", StringArgumentType.greedyString())
                        .executes(ctx -> cmdAddLine(ctx.getSource(),
                            StringArgumentType.getString(ctx, "id"),
                            StringArgumentType.getString(ctx, "text"))))))
            .then(Commands.literal("insertline")
                .then(Commands.argument("id", StringArgumentType.word())
                    .then(Commands.argument("index", IntegerArgumentType.integer(0))
                        .then(Commands.argument("text", StringArgumentType.greedyString())
                            .executes(ctx -> cmdInsertLine(ctx.getSource(),
                                StringArgumentType.getString(ctx, "id"),
                                IntegerArgumentType.getInteger(ctx, "index"),
                                StringArgumentType.getString(ctx, "text")))))))
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
            // /hologram addframes <id> <lineIndex> <intervalTicks> <frame1|frame2|...>
            .then(Commands.literal("addframes")
                .then(Commands.argument("id", StringArgumentType.word())
                    .then(Commands.argument("lineIndex", IntegerArgumentType.integer(0))
                        .then(Commands.argument("intervalTicks", IntegerArgumentType.integer(1, 200))
                            .then(Commands.argument("frames", StringArgumentType.greedyString())
                                .executes(ctx -> cmdAddFrames(ctx.getSource(),
                                    StringArgumentType.getString(ctx, "id"),
                                    IntegerArgumentType.getInteger(ctx, "lineIndex"),
                                    IntegerArgumentType.getInteger(ctx, "intervalTicks"),
                                    StringArgumentType.getString(ctx, "frames"))))))))
            // /hologram removeframes <id> <lineIndex>
            .then(Commands.literal("removeframes")
                .then(Commands.argument("id", StringArgumentType.word())
                    .then(Commands.argument("lineIndex", IntegerArgumentType.integer(0))
                        .executes(ctx -> cmdRemoveFrames(ctx.getSource(),
                            StringArgumentType.getString(ctx, "id"),
                            IntegerArgumentType.getInteger(ctx, "lineIndex"))))))
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
            .then(Commands.literal("movehere")
                .then(Commands.argument("id", StringArgumentType.word())
                    .executes(ctx -> cmdMoveHere(ctx.getSource(), StringArgumentType.getString(ctx, "id")))))
            .then(Commands.literal("near")
                .executes(ctx -> cmdNear(ctx.getSource(), 20.0))
                .then(Commands.argument("radius", DoubleArgumentType.doubleArg(1, 1000))
                    .executes(ctx -> cmdNear(ctx.getSource(), DoubleArgumentType.getDouble(ctx, "radius")))))
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
                .executes(ctx -> cmdReload(ctx.getSource())))
            // â”€â”€ Billboard / rotation sub-commands â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
            .then(Commands.literal("billboard")
                .then(Commands.argument("id", StringArgumentType.word())
                    .then(Commands.argument("mode", StringArgumentType.word())
                        .executes(ctx -> cmdBillboard(ctx.getSource(),
                            StringArgumentType.getString(ctx, "id"),
                            StringArgumentType.getString(ctx, "mode"))))))
            .then(Commands.literal("spin")
                .then(Commands.argument("id", StringArgumentType.word())
                    .then(Commands.literal("off")
                        .executes(ctx -> cmdSpinOff(ctx.getSource(), StringArgumentType.getString(ctx, "id"))))
                    .then(Commands.literal("on")
                        .executes(ctx -> cmdSpinOn(ctx.getSource(),
                            StringArgumentType.getString(ctx, "id"), 3.0f, "Y"))
                        .then(Commands.argument("speed", FloatArgumentType.floatArg(0.1f, 30f))
                            .executes(ctx -> cmdSpinOn(ctx.getSource(),
                                StringArgumentType.getString(ctx, "id"),
                                FloatArgumentType.getFloat(ctx, "speed"), "Y"))
                            .then(Commands.argument("axis", StringArgumentType.word())
                                .executes(ctx -> cmdSpinOn(ctx.getSource(),
                                    StringArgumentType.getString(ctx, "id"),
                                    FloatArgumentType.getFloat(ctx, "speed"),
                                    StringArgumentType.getString(ctx, "axis"))))))))
            .then(Commands.literal("hover")
                .then(Commands.argument("id", StringArgumentType.word())
                    .then(Commands.literal("off")
                        .executes(ctx -> cmdHoverOff(ctx.getSource(), StringArgumentType.getString(ctx, "id"))))
                    .then(Commands.literal("on")
                        .executes(ctx -> cmdHoverOn(ctx.getSource(),
                            StringArgumentType.getString(ctx, "id"), 0.08f, 1.5f))
                        .then(Commands.argument("amplitude", FloatArgumentType.floatArg(0.01f, 2.0f))
                            .executes(ctx -> cmdHoverOn(ctx.getSource(),
                                StringArgumentType.getString(ctx, "id"),
                                FloatArgumentType.getFloat(ctx, "amplitude"), 1.5f))
                            .then(Commands.argument("speed", FloatArgumentType.floatArg(0.1f, 10f))
                                .executes(ctx -> cmdHoverOn(ctx.getSource(),
                                    StringArgumentType.getString(ctx, "id"),
                                    FloatArgumentType.getFloat(ctx, "amplitude"),
                                    FloatArgumentType.getFloat(ctx, "speed"))))))))
            // â”€â”€ Visual appearance sub-commands â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
            .then(Commands.literal("scale")
                .then(Commands.argument("id", StringArgumentType.word())
                    .then(Commands.argument("scale", FloatArgumentType.floatArg(0.1f, 10.0f))
                        .executes(ctx -> cmdScale(ctx.getSource(),
                            StringArgumentType.getString(ctx, "id"),
                            FloatArgumentType.getFloat(ctx, "scale"))))))
            .then(Commands.literal("linespacing")
                .then(Commands.argument("id", StringArgumentType.word())
                    .then(Commands.argument("spacing", FloatArgumentType.floatArg(0.05f, 3.0f))
                        .executes(ctx -> cmdLineSpacing(ctx.getSource(),
                            StringArgumentType.getString(ctx, "id"),
                            FloatArgumentType.getFloat(ctx, "spacing"))))))
            .then(Commands.literal("shadow")
                .then(Commands.argument("id", StringArgumentType.word())
                    .then(Commands.literal("on")
                        .executes(ctx -> cmdShadow(ctx.getSource(), StringArgumentType.getString(ctx, "id"), true)))
                    .then(Commands.literal("off")
                        .executes(ctx -> cmdShadow(ctx.getSource(), StringArgumentType.getString(ctx, "id"), false)))))
            .then(Commands.literal("opacity")
                .then(Commands.argument("id", StringArgumentType.word())
                    .then(Commands.argument("opacity", IntegerArgumentType.integer(0, 255))
                        .executes(ctx -> cmdOpacity(ctx.getSource(),
                            StringArgumentType.getString(ctx, "id"),
                            IntegerArgumentType.getInteger(ctx, "opacity"))))))
            .then(Commands.literal("background")
                .then(Commands.argument("id", StringArgumentType.word())
                    .then(Commands.argument("color", StringArgumentType.word())
                        .executes(ctx -> cmdBackground(ctx.getSource(),
                            StringArgumentType.getString(ctx, "id"),
                            StringArgumentType.getString(ctx, "color"))))));

        dispatcher.register(root);
        // Alias /holo
        dispatcher.register(Commands.literal("holo")
            .requires(src -> hasPermission(src))
            .redirect(dispatcher.getRoot().getChild("hologram")));
    }
    // â”€â”€ Subcommand implementations â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
    private static int cmdCreate(CommandSourceStack src, String id, double x, double y, double z, String worldArg) {
        try {
            if (HologramManager.getInstance().exists(id)) {
                src.sendFailure(Component.literal("Â§cHologram 'Â§e" + id + "Â§c' already exists."));
                return 0;
            }
            HologramData data = new HologramData();
            data.id = id.toLowerCase();
            data.x = x; data.y = y; data.z = z;
            data.world = resolveWorld(src, worldArg);
            data.refreshInterval = 5;
            HologramManager.getInstance().registerHologram(data);
            ServerLevel level = getLevel(src, data.world);
            if (level != null) HologramRenderer.spawn(data, level);
            src.sendSuccess(() -> Component.literal("Â§aâœ“ Hologram 'Â§e" + id + "Â§a' created at Â§7(" + fmt(x) + ", " + fmt(y) + ", " + fmt(z) + ")Â§a in Â§7" + data.world + "Â§a.\nÂ§7Use Â§f/hologram addline " + id + " <text> Â§7to add lines."), true);
        } catch (Exception e) {
            src.sendFailure(Component.literal("Â§cError: " + e.getMessage()));
        }
        return 1;
    }
    private static int cmdDelete(CommandSourceStack src, String id) {
        HologramData data = HologramManager.getInstance().getHologram(id);
        if (data == null) { src.sendFailure(Component.literal("Â§cHologram 'Â§e" + id + "Â§c' not found.")); return 0; }
        ServerLevel level = getLevel(src, data.world);
        if (level != null) HologramRenderer.despawn(data, level);
        HologramManager.getInstance().removeHologram(id);
        src.sendSuccess(() -> Component.literal("Â§aâœ“ Hologram 'Â§e" + id + "Â§a' deleted."), true);
        return 1;
    }
    private static int cmdCopy(CommandSourceStack src, String id, String newId) {
        HologramData src2 = HologramManager.getInstance().getHologram(id);
        if (src2 == null) { src.sendFailure(Component.literal("Â§cHologram 'Â§e" + id + "Â§c' not found.")); return 0; }
        if (HologramManager.getInstance().exists(newId)) {
            src.sendFailure(Component.literal("Â§cHologram 'Â§e" + newId + "Â§c' already exists."));
            return 0;
        }
        // Deep-copy via Gson round-trip to avoid shared mutable state
        com.google.gson.Gson gson = new com.google.gson.Gson();
        HologramData copy = gson.fromJson(gson.toJson(src2), HologramData.class);
        copy.id = newId.toLowerCase();
        copy.entityUUIDs = new ArrayList<>();
        HologramManager.getInstance().registerHologram(copy);
        ServerLevel level = getLevel(src, copy.world);
        if (level != null) HologramRenderer.spawn(copy, level);
        src.sendSuccess(() -> Component.literal("Â§aâœ“ Hologram 'Â§e" + id + "Â§a' copied to 'Â§e" + newId + "Â§a'."), true);
        return 1;
    }
    private static int cmdAddLine(CommandSourceStack src, String id, String text) {
        HologramData data = HologramManager.getInstance().getHologram(id);
        if (data == null) { src.sendFailure(Component.literal("Â§cHologram 'Â§e" + id + "Â§c' not found.")); return 0; }
        data.lines.add(new HologramLine(text));
        HologramManager.getInstance().registerHologram(data);
        respawn(src, data);
        src.sendSuccess(() -> Component.literal("Â§aâœ“ Line added to 'Â§e" + id + "Â§a'. Total lines: " + data.lines.size()), true);
        return 1;
    }
    private static int cmdInsertLine(CommandSourceStack src, String id, int index, String text) {
        HologramData data = HologramManager.getInstance().getHologram(id);
        if (data == null) { src.sendFailure(Component.literal("Â§cHologram 'Â§e" + id + "Â§c' not found.")); return 0; }
        // Allow inserting at the end (index == size) as well as within the list
        if (index < 0 || index > data.lines.size()) {
            src.sendFailure(Component.literal("Â§cLine index out of range (0â€“" + data.lines.size() + ")."));
            return 0;
        }
        data.lines.add(index, new HologramLine(text));
        HologramManager.getInstance().registerHologram(data);
        respawn(src, data);
        src.sendSuccess(() -> Component.literal("Â§aâœ“ Line inserted at Â§e" + index + "Â§a in 'Â§e" + id + "Â§a'. Total lines: " + data.lines.size()), true);
        return 1;
    }
    private static int cmdSetLine(CommandSourceStack src, String id, int index, String text) {
        HologramData data = HologramManager.getInstance().getHologram(id);
        if (data == null) { src.sendFailure(Component.literal("Â§cHologram 'Â§e" + id + "Â§c' not found.")); return 0; }
        if (index < 0 || index >= data.lines.size()) { src.sendFailure(Component.literal("Â§cLine index out of range (0â€“" + (data.lines.size()-1) + ").")); return 0; }
        data.lines.get(index).text = text;
        HologramManager.getInstance().registerHologram(data);
        respawn(src, data);
        src.sendSuccess(() -> Component.literal("Â§aâœ“ Line Â§e" + index + "Â§a updated."), true);
        return 1;
    }
    private static int cmdRemoveLine(CommandSourceStack src, String id, int index) {
        HologramData data = HologramManager.getInstance().getHologram(id);
        if (data == null) { src.sendFailure(Component.literal("Â§cHologram 'Â§e" + id + "Â§c' not found.")); return 0; }
        if (index < 0 || index >= data.lines.size()) { src.sendFailure(Component.literal("Â§cLine index out of range.")); return 0; }
        data.lines.remove(index);
        HologramManager.getInstance().registerHologram(data);
        respawn(src, data);
        src.sendSuccess(() -> Component.literal("Â§aâœ“ Line Â§e" + index + "Â§a removed."), true);
        return 1;
    }
    /**
     * /hologram addframes &lt;id&gt; &lt;lineIndex&gt; &lt;intervalTicks&gt; &lt;frame1|frame2|...&gt;
     * Frames are separated by {@code |}.
     */
    private static int cmdAddFrames(CommandSourceStack src, String id, int lineIndex, int intervalTicks, String framesRaw) {
        HologramData data = HologramManager.getInstance().getHologram(id);
        if (data == null) { src.sendFailure(Component.literal("Â§cHologram 'Â§e" + id + "Â§c' not found.")); return 0; }
        if (lineIndex < 0 || lineIndex >= data.lines.size()) {
            src.sendFailure(Component.literal("Â§cLine index out of range (0â€“" + (data.lines.size()-1) + ")."));
            return 0;
        }
        String[] parts = framesRaw.split("\\|");
        if (parts.length < 2) {
            src.sendFailure(Component.literal("Â§cProvide at least 2 frames separated by Â§f|Â§c. Example: Â§fHello|World"));
            return 0;
        }
        HologramLine line = data.lines.get(lineIndex);
        line.frames.clear();
        for (String part : parts) line.frames.add(part.trim());
        line.animFrameIntervalTicks = intervalTicks;
        line.currentFrame = 0;
        line.animTickCount = 0;
        HologramManager.getInstance().registerHologram(data);
        respawn(src, data);
        int fc = line.frames.size();
        src.sendSuccess(() -> Component.literal("Â§aâœ“ Â§e" + fc + "Â§a frame(s) set on line Â§e" + lineIndex + "Â§a of 'Â§e" + id + "Â§a' (every Â§e" + intervalTicks + "Â§a ticks)."), true);
        return 1;
    }
    private static int cmdRemoveFrames(CommandSourceStack src, String id, int lineIndex) {
        HologramData data = HologramManager.getInstance().getHologram(id);
        if (data == null) { src.sendFailure(Component.literal("Â§cHologram 'Â§e" + id + "Â§c' not found.")); return 0; }
        if (lineIndex < 0 || lineIndex >= data.lines.size()) {
            src.sendFailure(Component.literal("Â§cLine index out of range (0â€“" + (data.lines.size()-1) + ")."));
            return 0;
        }
        HologramLine line = data.lines.get(lineIndex);
        line.frames.clear();
        line.animFrameIntervalTicks = 0;
        line.currentFrame = 0;
        HologramManager.getInstance().registerHologram(data);
        respawn(src, data);
        src.sendSuccess(() -> Component.literal("Â§aâœ“ Frame animation removed from line Â§e" + lineIndex + "Â§a of 'Â§e" + id + "Â§a'."), true);
        return 1;
    }
    private static int cmdMoveTo(CommandSourceStack src, String id, double x, double y, double z) {
        HologramData data = HologramManager.getInstance().getHologram(id);
        if (data == null) { src.sendFailure(Component.literal("Â§cHologram 'Â§e" + id + "Â§c' not found.")); return 0; }
        ServerLevel level = getLevel(src, data.world);
        if (level != null) HologramRenderer.despawn(data, level);
        data.x = x; data.y = y; data.z = z;
        HologramManager.getInstance().registerHologram(data);
        if (level != null) HologramRenderer.spawn(data, level);
        src.sendSuccess(() -> Component.literal("Â§aâœ“ Hologram 'Â§e" + id + "Â§a' moved to Â§7(" + fmt(x) + ", " + fmt(y) + ", " + fmt(z) + ")Â§a."), true);
        return 1;
    }
    private static int cmdMoveHere(CommandSourceStack src, String id) {
        if (!(src.getEntity() instanceof ServerPlayer player)) {
            src.sendFailure(Component.literal("Â§cThis command can only be run by a player."));
            return 0;
        }
        HologramData data = HologramManager.getInstance().getHologram(id);
        if (data == null) { src.sendFailure(Component.literal("Â§cHologram 'Â§e" + id + "Â§c' not found.")); return 0; }
        ServerLevel level = getLevel(src, data.world);
        if (level != null) HologramRenderer.despawn(data, level);
        data.x = player.getX();
        data.y = player.getY() + 1.5; // eye-level ~ feet+1.5
        data.z = player.getZ();
        HologramManager.getInstance().registerHologram(data);
        if (level != null) HologramRenderer.spawn(data, level);
        double nx = data.x, ny = data.y, nz = data.z;
        src.sendSuccess(() -> Component.literal("Â§aâœ“ Hologram 'Â§e" + id + "Â§a' moved to your position Â§7(" + fmt(nx) + ", " + fmt(ny) + ", " + fmt(nz) + ")Â§a."), true);
        return 1;
    }
    private static int cmdNear(CommandSourceStack src, double radius) {
        if (!(src.getEntity() instanceof ServerPlayer player)) {
            src.sendFailure(Component.literal("Â§cThis command can only be run by a player."));
            return 0;
        }
        String dimKey = HologramRenderer.dimensionKey(player.serverLevel());
        double px = player.getX(), pz = player.getZ();
        List<HologramData> nearby = new ArrayList<>();
        for (HologramData d : HologramManager.getInstance().getAllHolograms()) {
            if (dimKey.equals(d.world) && d.distanceXZ(px, pz) <= radius) nearby.add(d);
        }
        if (nearby.isEmpty()) {
            src.sendSuccess(() -> Component.literal("Â§7No holograms within Â§e" + fmt(radius) + "Â§7 blocks."), false);
            return 1;
        }
        nearby.sort((a, b) -> Double.compare(a.distanceXZ(px, pz), b.distanceXZ(px, pz)));
        StringBuilder sb = new StringBuilder("Â§6Nearby holograms (").append(nearby.size()).append(") within Â§e").append(fmt(radius)).append("Â§6 blocks:\n");
        for (HologramData d : nearby) {
            double dist = d.distanceXZ(px, pz);
            sb.append("  Â§e").append(d.id)
              .append(" Â§7â€” Â§f(").append(fmt(d.x)).append(", ").append(fmt(d.y)).append(", ").append(fmt(d.z)).append(")")
              .append(" Â§8[").append(fmt(dist)).append("m]")
              .append(d.visible ? "" : " Â§8[hidden]").append("\n");
        }
        src.sendSuccess(() -> Component.literal(sb.toString().trim()), false);
        return 1;
    }
    private static int cmdSetRefresh(CommandSourceStack src, String id, int seconds) {
        HologramData data = HologramManager.getInstance().getHologram(id);
        if (data == null) { src.sendFailure(Component.literal("Â§cHologram 'Â§e" + id + "Â§c' not found.")); return 0; }
        data.refreshInterval = seconds;
        HologramManager.getInstance().registerHologram(data);
        String msg = seconds == 0 ? "Â§aâœ“ Refresh disabled for 'Â§e" + id + "Â§a'." : "Â§aâœ“ Refresh for 'Â§e" + id + "Â§a' set to Â§e" + seconds + "sÂ§a.";
        src.sendSuccess(() -> Component.literal(msg), true);
        return 1;
    }
    private static int cmdToggle(CommandSourceStack src, String id) {
        HologramData data = HologramManager.getInstance().getHologram(id);
        if (data == null) { src.sendFailure(Component.literal("Â§cHologram 'Â§e" + id + "Â§c' not found.")); return 0; }
        data.visible = !data.visible;
        HologramManager.getInstance().registerHologram(data);
        ServerLevel level = getLevel(src, data.world);
        if (level != null) {
            if (data.visible) HologramRenderer.spawn(data, level);
            else HologramRenderer.despawn(data, level);
        }
        boolean v = data.visible;
        src.sendSuccess(() -> Component.literal("Â§aHologram 'Â§e" + id + "Â§a' is now " + (v ? "Â§2visible" : "Â§7hidden") + "Â§a."), true);
        return 1;
    }
    private static int cmdList(CommandSourceStack src) {
        Collection<HologramData> all = HologramManager.getInstance().getAllHolograms();
        if (all.isEmpty()) { src.sendSuccess(() -> Component.literal("Â§7No holograms found."), false); return 1; }
        StringBuilder sb = new StringBuilder("Â§6Holograms (").append(all.size()).append("):\n");
        for (HologramData d : all) {
            sb.append("  Â§e").append(d.id).append(" Â§7â€” Â§f").append(d.world)
              .append(" Â§7(").append(fmt(d.x)).append(", ").append(fmt(d.y)).append(", ").append(fmt(d.z))
              .append(") Â§7lines=Â§f").append(d.lines.size())
              .append(d.visible ? "" : " Â§8[hidden]").append("\n");
        }
        src.sendSuccess(() -> Component.literal(sb.toString().trim()), false);
        return 1;
    }
    private static int cmdInfo(CommandSourceStack src, String id) {
        HologramData data = HologramManager.getInstance().getHologram(id);
        if (data == null) { src.sendFailure(Component.literal("Â§cHologram 'Â§e" + id + "Â§c' not found.")); return 0; }
        String[] bbNames = {"FIXED", "VERTICAL", "HORIZONTAL", "CENTER"};
        String bbName = (data.billboardMode >= 0 && data.billboardMode < bbNames.length)
            ? bbNames[data.billboardMode] : "UNKNOWN";
        StringBuilder sb = new StringBuilder();
        sb.append("Â§6=== Hologram: Â§e").append(data.id).append(" Â§6===\n");
        sb.append("Â§7World: Â§f").append(data.world).append("\n");
        sb.append("Â§7Position: Â§f(").append(fmt(data.x)).append(", ").append(fmt(data.y)).append(", ").append(fmt(data.z)).append(")\n");
        sb.append("Â§7Visible: ").append(data.visible ? "Â§ayes" : "Â§cno").append("\n");
        sb.append("Â§7Refresh: Â§f").append(data.refreshInterval == 0 ? "disabled" : data.refreshInterval + "s").append("\n");
        sb.append("Â§7Billboard: Â§e").append(bbName).append("\n");
        sb.append("Â§7Scale: Â§f").append(data.scale).append("x\n");
        sb.append("Â§7Line spacing: Â§f").append(data.lineSpacing).append(" blocks\n");
        sb.append("Â§7Shadow: ").append(data.textShadow ? "Â§aon" : "Â§8off").append("\n");
        sb.append("Â§7Opacity: Â§f").append(data.textOpacity).append("/255\n");
        sb.append("Â§7Background: Â§f").append(String.format("0x%08X", data.backgroundColorArgb)).append("\n");
        if (data.spinEnabled) {
            sb.append("Â§7Spin: Â§eon Â§7(Â§f").append(data.spinSpeedDegrees).append("Â°/tickÂ§7, axis=Â§f").append(data.spinAxis).append("Â§7)\n");
        } else {
            sb.append("Â§7Spin: Â§8off\n");
        }
        if (data.hoverEnabled) {
            sb.append("Â§7Hover: Â§eon Â§7(amplitude=Â§f").append(data.hoverAmplitude).append("Â§7 blocks, speed=Â§f").append(data.hoverSpeedDegrees).append("Â§7Â°/tick)\n");
        } else {
            sb.append("Â§7Hover: Â§8off\n");
        }
        sb.append("Â§7Lines (").append(data.lines.size()).append("):\n");
        for (int i = 0; i < data.lines.size(); i++) {
            HologramLine line = data.lines.get(i);
            sb.append("  Â§e").append(i).append("Â§7: Â§f").append(line.currentText());
            if (!line.frames.isEmpty()) sb.append(" Â§8[animated, ").append(line.frames.size()).append(" frames, every ").append(line.animFrameIntervalTicks).append("t]");
            sb.append("\n");
        }
        src.sendSuccess(() -> Component.literal(sb.toString().trim()), false);
        return 1;
    }
    // â”€â”€ Billboard / spin / hover â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
    private static int cmdBillboard(CommandSourceStack src, String id, String mode) {
        HologramData data = HologramManager.getInstance().getHologram(id);
        if (data == null) { src.sendFailure(Component.literal("Â§cHologram 'Â§e" + id + "Â§c' not found.")); return 0; }
        int modeVal = switch (mode.toLowerCase()) {
            case "fixed"      -> 0;
            case "vertical"   -> 1;
            case "horizontal" -> 2;
            case "center"     -> 3;
            default -> -1;
        };
        if (modeVal < 0) {
            src.sendFailure(Component.literal("Â§cInvalid billboard mode. Use: Â§ffixedÂ§c, Â§fverticalÂ§c, Â§fhorizontalÂ§c, Â§fcenterÂ§c."));
            return 0;
        }
        data.billboardMode = modeVal;
        HologramManager.getInstance().registerHologram(data);
        respawn(src, data);
        String modeName = new String[]{"FIXED", "VERTICAL", "HORIZONTAL", "CENTER"}[modeVal];
        src.sendSuccess(() -> Component.literal("Â§aâœ“ Hologram 'Â§e" + id + "Â§a' billboard set to Â§e" + modeName + "Â§a."), true);
        return 1;
    }
    private static int cmdSpinOn(CommandSourceStack src, String id, float speed, String axis) {
        HologramData data = HologramManager.getInstance().getHologram(id);
        if (data == null) { src.sendFailure(Component.literal("Â§cHologram 'Â§e" + id + "Â§c' not found.")); return 0; }
        String axisUpper = axis.toUpperCase();
        if (!axisUpper.equals("X") && !axisUpper.equals("Y") && !axisUpper.equals("Z")) {
            src.sendFailure(Component.literal("Â§cInvalid axis 'Â§e" + axis + "Â§c'. Use X, Y, or Z."));
            return 0;
        }
        data.spinEnabled      = true;
        data.spinSpeedDegrees = speed;
        data.spinAxis         = axisUpper;
        HologramManager.getInstance().registerHologram(data);
        respawn(src, data);
        src.sendSuccess(() -> Component.literal("Â§aâœ“ Spin enabled for 'Â§e" + id + "Â§a': Â§e" + speed + "Â°/tickÂ§a on Â§e" + axisUpper + "Â§a axis."), true);
        return 1;
    }
    private static int cmdSpinOff(CommandSourceStack src, String id) {
        HologramData data = HologramManager.getInstance().getHologram(id);
        if (data == null) { src.sendFailure(Component.literal("Â§cHologram 'Â§e" + id + "Â§c' not found.")); return 0; }
        data.spinEnabled = false;
        data.currentSpinAngle = 0f;
        HologramManager.getInstance().registerHologram(data);
        respawn(src, data);
        src.sendSuccess(() -> Component.literal("Â§aâœ“ Spin disabled for 'Â§e" + id + "Â§a'."), true);
        return 1;
    }
    private static int cmdHoverOn(CommandSourceStack src, String id, float amplitude, float speed) {
        HologramData data = HologramManager.getInstance().getHologram(id);
        if (data == null) { src.sendFailure(Component.literal("Â§cHologram 'Â§e" + id + "Â§c' not found.")); return 0; }
        data.hoverEnabled       = true;
        data.hoverAmplitude     = amplitude;
        data.hoverSpeedDegrees  = speed;
        HologramManager.getInstance().registerHologram(data);
        respawn(src, data);
        src.sendSuccess(() -> Component.literal("Â§aâœ“ Hover enabled for 'Â§e" + id + "Â§a': Â§eamplitude=Â§f" + amplitude + "Â§a blocks, Â§espeed=Â§f" + speed + "Â§aÂ°/tick."), true);
        return 1;
    }
    private static int cmdHoverOff(CommandSourceStack src, String id) {
        HologramData data = HologramManager.getInstance().getHologram(id);
        if (data == null) { src.sendFailure(Component.literal("Â§cHologram 'Â§e" + id + "Â§c' not found.")); return 0; }
        data.hoverEnabled = false;
        data.hoverPhase   = 0f;
        HologramManager.getInstance().registerHologram(data);
        respawn(src, data);
        src.sendSuccess(() -> Component.literal("Â§aâœ“ Hover disabled for 'Â§e" + id + "Â§a'."), true);
        return 1;
    }
    // â”€â”€ Visual appearance commands â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
    private static int cmdScale(CommandSourceStack src, String id, float scale) {
        HologramData data = HologramManager.getInstance().getHologram(id);
        if (data == null) { src.sendFailure(Component.literal("Â§cHologram 'Â§e" + id + "Â§c' not found.")); return 0; }
        data.scale = scale;
        HologramManager.getInstance().registerHologram(data);
        respawn(src, data);
        src.sendSuccess(() -> Component.literal("Â§aâœ“ Hologram 'Â§e" + id + "Â§a' scale set to Â§e" + scale + "xÂ§a."), true);
        return 1;
    }
    private static int cmdLineSpacing(CommandSourceStack src, String id, float spacing) {
        HologramData data = HologramManager.getInstance().getHologram(id);
        if (data == null) { src.sendFailure(Component.literal("Â§cHologram 'Â§e" + id + "Â§c' not found.")); return 0; }
        data.lineSpacing = spacing;
        HologramManager.getInstance().registerHologram(data);
        respawn(src, data);
        src.sendSuccess(() -> Component.literal("Â§aâœ“ Hologram 'Â§e" + id + "Â§a' line spacing set to Â§e" + spacing + "Â§a blocks."), true);
        return 1;
    }
    private static int cmdShadow(CommandSourceStack src, String id, boolean on) {
        HologramData data = HologramManager.getInstance().getHologram(id);
        if (data == null) { src.sendFailure(Component.literal("Â§cHologram 'Â§e" + id + "Â§c' not found.")); return 0; }
        data.textShadow = on;
        HologramManager.getInstance().registerHologram(data);
        respawn(src, data);
        src.sendSuccess(() -> Component.literal("Â§aâœ“ Text shadow " + (on ? "Â§aenabled" : "Â§7disabled") + "Â§a for 'Â§e" + id + "Â§a'."), true);
        return 1;
    }
    private static int cmdOpacity(CommandSourceStack src, String id, int opacity) {
        HologramData data = HologramManager.getInstance().getHologram(id);
        if (data == null) { src.sendFailure(Component.literal("Â§cHologram 'Â§e" + id + "Â§c' not found.")); return 0; }
        data.textOpacity = Math.max(0, Math.min(255, opacity));
        HologramManager.getInstance().registerHologram(data);
        respawn(src, data);
        src.sendSuccess(() -> Component.literal("Â§aâœ“ Hologram 'Â§e" + id + "Â§a' opacity set to Â§e" + data.textOpacity + "/255Â§a."), true);
        return 1;
    }
    /**
     * /hologram background &lt;id&gt; &lt;transparent|#RRGGBB|#AARRGGBB&gt;
     * Examples: transparent, #000000, #40000000
     */
    private static int cmdBackground(CommandSourceStack src, String id, String colorStr) {
        HologramData data = HologramManager.getInstance().getHologram(id);
        if (data == null) { src.sendFailure(Component.literal("Â§cHologram 'Â§e" + id + "Â§c' not found.")); return 0; }
        int argb;
        try {
            if (colorStr.equalsIgnoreCase("transparent") || colorStr.equals("0")) {
                argb = 0x00000000;
            } else {
                String hex = colorStr.startsWith("#") ? colorStr.substring(1) : colorStr;
                if (hex.length() == 6) hex = "00" + hex;   // opaque (alpha=0 means transparent â€” default MC bg)
                else if (hex.length() == 8) { /* use full ARGB */ }
                else { src.sendFailure(Component.literal("Â§cInvalid colour. Use Â§ftransparentÂ§c, Â§f#RRGGBBÂ§c, or Â§f#AARRGGBBÂ§c.")); return 0; }
                argb = (int) Long.parseLong(hex, 16);
            }
        } catch (NumberFormatException ex) {
            src.sendFailure(Component.literal("Â§cInvalid colour value 'Â§e" + colorStr + "Â§c'. Use Â§ftransparentÂ§c, Â§f#RRGGBBÂ§c, or Â§f#AARRGGBBÂ§c."));
            return 0;
        }
        data.backgroundColorArgb = argb;
        HologramManager.getInstance().registerHologram(data);
        respawn(src, data);
        final String hex = String.format("0x%08X", argb);
        src.sendSuccess(() -> Component.literal("Â§aâœ“ Hologram 'Â§e" + id + "Â§a' background set to Â§e" + hex + "Â§a."), true);
        return 1;
    }
    private static int cmdReload(CommandSourceStack src) {        try {
            net.minecraft.server.MinecraftServer server = src.getServer();
            for (ServerLevel level : server.getAllLevels()) {
                String dimKey = HologramRenderer.dimensionKey(level);
                HologramRenderer.despawnAllForWorld(level, dimKey);
            }
            HologramManager.getInstance().initialize();
            for (ServerLevel level : server.getAllLevels()) {
                String dimKey = HologramRenderer.dimensionKey(level);
                HologramRenderer.spawnAllForWorld(level, dimKey);
            }
            src.sendSuccess(() -> Component.literal("Â§aâœ“ Holograms reloaded. " + HologramManager.getInstance().getAllHolograms().size() + " hologram(s) active."), true);
        } catch (Exception e) {
            src.sendFailure(Component.literal("Â§cReload failed: " + e.getMessage()));
        }
        return 1;
    }
    // â”€â”€ Helpers â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
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
