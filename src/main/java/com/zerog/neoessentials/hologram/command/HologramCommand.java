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
 * /hologram (alias /holo) — admin command for managing holographic displays.
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

    /** Tab-completion for an existing hologram's id. Not used on `create`, which types a new id. */
    private static java.util.concurrent.CompletableFuture<com.mojang.brigadier.suggestion.Suggestions> suggestHologramIds(
            com.mojang.brigadier.context.CommandContext<CommandSourceStack> ctx,
            com.mojang.brigadier.suggestion.SuggestionsBuilder builder) {
        java.util.List<String> ids = HologramManager.getInstance().getAllHolograms().stream()
            .map(d -> d.id)
            .sorted()
            .toList();
        return net.minecraft.commands.SharedSuggestionProvider.suggest(ids, builder);
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        var root = Commands.literal("hologram")
            .requires(src -> hasPermission(src))
            .then(Commands.literal("create")
                // /hologram create <id>  — creates at player's current position
                // (no id suggestions here: this is a *new* id being typed, not an existing one)
                .then(Commands.argument("id", StringArgumentType.word())
                    .executes(ctx -> cmdCreateHere(ctx.getSource(),
                        StringArgumentType.getString(ctx, "id")))
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
                .then(Commands.argument("id", StringArgumentType.word()).suggests(HologramCommand::suggestHologramIds)
                    .executes(ctx -> cmdDelete(ctx.getSource(), StringArgumentType.getString(ctx, "id")))))
            .then(Commands.literal("rename")
                .then(Commands.argument("id", StringArgumentType.word()).suggests(HologramCommand::suggestHologramIds)
                    .then(Commands.argument("newid", StringArgumentType.word())
                        .executes(ctx -> cmdRename(ctx.getSource(),
                            StringArgumentType.getString(ctx, "id"),
                            StringArgumentType.getString(ctx, "newid"))))))
            .then(Commands.literal("copy")
                .then(Commands.argument("id", StringArgumentType.word()).suggests(HologramCommand::suggestHologramIds)
                    .then(Commands.argument("newid", StringArgumentType.word())
                        .executes(ctx -> cmdCopy(ctx.getSource(),
                            StringArgumentType.getString(ctx, "id"),
                            StringArgumentType.getString(ctx, "newid"))))))
            .then(Commands.literal("addline")
                .then(Commands.argument("id", StringArgumentType.word()).suggests(HologramCommand::suggestHologramIds)
                    .then(Commands.argument("text", StringArgumentType.greedyString())
                        .executes(ctx -> cmdAddLine(ctx.getSource(),
                            StringArgumentType.getString(ctx, "id"),
                            StringArgumentType.getString(ctx, "text"))))))
            .then(Commands.literal("insertline")
                .then(Commands.argument("id", StringArgumentType.word()).suggests(HologramCommand::suggestHologramIds)
                    .then(Commands.argument("index", IntegerArgumentType.integer(1))
                        .then(Commands.argument("text", StringArgumentType.greedyString())
                            .executes(ctx -> cmdInsertLine(ctx.getSource(),
                                StringArgumentType.getString(ctx, "id"),
                                IntegerArgumentType.getInteger(ctx, "index"),
                                StringArgumentType.getString(ctx, "text")))))))
            .then(Commands.literal("setline")
                .then(Commands.argument("id", StringArgumentType.word()).suggests(HologramCommand::suggestHologramIds)
                    .then(Commands.argument("index", IntegerArgumentType.integer(1))
                        .then(Commands.argument("text", StringArgumentType.greedyString())
                            .executes(ctx -> cmdSetLine(ctx.getSource(),
                                StringArgumentType.getString(ctx, "id"),
                                IntegerArgumentType.getInteger(ctx, "index"),
                                StringArgumentType.getString(ctx, "text")))))))
            .then(Commands.literal("removeline")
                .then(Commands.argument("id", StringArgumentType.word()).suggests(HologramCommand::suggestHologramIds)
                    .then(Commands.argument("index", IntegerArgumentType.integer(1))
                        .executes(ctx -> cmdRemoveLine(ctx.getSource(),
                            StringArgumentType.getString(ctx, "id"),
                            IntegerArgumentType.getInteger(ctx, "index"))))))
            // /hologram addframes <id> <lineIndex> <intervalTicks> <frame1|frame2|...>
            .then(Commands.literal("addframes")
                .then(Commands.argument("id", StringArgumentType.word()).suggests(HologramCommand::suggestHologramIds)
                    .then(Commands.argument("lineIndex", IntegerArgumentType.integer(1))
                        .then(Commands.argument("intervalTicks", IntegerArgumentType.integer(1, 200))
                            .then(Commands.argument("frames", StringArgumentType.greedyString())
                                .executes(ctx -> cmdAddFrames(ctx.getSource(),
                                    StringArgumentType.getString(ctx, "id"),
                                    IntegerArgumentType.getInteger(ctx, "lineIndex"),
                                    IntegerArgumentType.getInteger(ctx, "intervalTicks"),
                                    StringArgumentType.getString(ctx, "frames"))))))))
            // /hologram removeframes <id> <lineIndex>
            .then(Commands.literal("removeframes")
                .then(Commands.argument("id", StringArgumentType.word()).suggests(HologramCommand::suggestHologramIds)
                    .then(Commands.argument("lineIndex", IntegerArgumentType.integer(1))
                        .executes(ctx -> cmdRemoveFrames(ctx.getSource(),
                            StringArgumentType.getString(ctx, "id"),
                            IntegerArgumentType.getInteger(ctx, "lineIndex"))))))
            .then(Commands.literal("moveto")
                .then(Commands.argument("id", StringArgumentType.word()).suggests(HologramCommand::suggestHologramIds)
                    .then(Commands.argument("x", DoubleArgumentType.doubleArg())
                        .then(Commands.argument("y", DoubleArgumentType.doubleArg())
                            .then(Commands.argument("z", DoubleArgumentType.doubleArg())
                                .executes(ctx -> cmdMoveTo(ctx.getSource(),
                                    StringArgumentType.getString(ctx, "id"),
                                    DoubleArgumentType.getDouble(ctx, "x"),
                                    DoubleArgumentType.getDouble(ctx, "y"),
                                    DoubleArgumentType.getDouble(ctx, "z"))))))))
            .then(Commands.literal("movehere")
                .then(Commands.argument("id", StringArgumentType.word()).suggests(HologramCommand::suggestHologramIds)
                    .executes(ctx -> cmdMoveHere(ctx.getSource(), StringArgumentType.getString(ctx, "id")))))
            .then(Commands.literal("align")
                .then(Commands.argument("id", StringArgumentType.word()).suggests(HologramCommand::suggestHologramIds)
                    .executes(ctx -> cmdAlign(ctx.getSource(), StringArgumentType.getString(ctx, "id")))))
            .then(Commands.literal("tp")
                .then(Commands.argument("id", StringArgumentType.word()).suggests(HologramCommand::suggestHologramIds)
                    .executes(ctx -> cmdTp(ctx.getSource(), StringArgumentType.getString(ctx, "id")))))
            .then(Commands.literal("near")
                .executes(ctx -> cmdNear(ctx.getSource(), 20.0))
                .then(Commands.argument("radius", DoubleArgumentType.doubleArg(1, 1000))
                    .executes(ctx -> cmdNear(ctx.getSource(), DoubleArgumentType.getDouble(ctx, "radius")))))
            .then(Commands.literal("setrefresh")
                .then(Commands.argument("id", StringArgumentType.word()).suggests(HologramCommand::suggestHologramIds)
                    .then(Commands.argument("seconds", IntegerArgumentType.integer(0))
                        .executes(ctx -> cmdSetRefresh(ctx.getSource(),
                            StringArgumentType.getString(ctx, "id"),
                            IntegerArgumentType.getInteger(ctx, "seconds"))))))
            .then(Commands.literal("toggle")
                .then(Commands.argument("id", StringArgumentType.word()).suggests(HologramCommand::suggestHologramIds)
                    .executes(ctx -> cmdToggle(ctx.getSource(), StringArgumentType.getString(ctx, "id")))))
            .then(Commands.literal("list")
                .executes(ctx -> cmdList(ctx.getSource())))
            .then(Commands.literal("info")
                .then(Commands.argument("id", StringArgumentType.word()).suggests(HologramCommand::suggestHologramIds)
                    .executes(ctx -> cmdInfo(ctx.getSource(), StringArgumentType.getString(ctx, "id")))))
            .then(Commands.literal("reload")
                .executes(ctx -> cmdReload(ctx.getSource())))
            // ── Billboard / rotation sub-commands ─────────────────────────────
            .then(Commands.literal("billboard")
                .then(Commands.argument("id", StringArgumentType.word()).suggests(HologramCommand::suggestHologramIds)
                    .then(Commands.argument("mode", StringArgumentType.word())
                        .executes(ctx -> cmdBillboard(ctx.getSource(),
                            StringArgumentType.getString(ctx, "id"),
                            StringArgumentType.getString(ctx, "mode"))))))
            .then(Commands.literal("textalign")
                .then(Commands.argument("id", StringArgumentType.word()).suggests(HologramCommand::suggestHologramIds)
                    .then(Commands.argument("align", StringArgumentType.word())
                        .executes(ctx -> cmdTextAlign(ctx.getSource(),
                            StringArgumentType.getString(ctx, "id"),
                            StringArgumentType.getString(ctx, "align"))))))
            .then(Commands.literal("spin")
                .then(Commands.argument("id", StringArgumentType.word()).suggests(HologramCommand::suggestHologramIds)
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
            // /hologram spintrack <id> <on|off>  — toggle player-tracking for Y-axis spin
            .then(Commands.literal("spintrack")
                .then(Commands.argument("id", StringArgumentType.word()).suggests(HologramCommand::suggestHologramIds)
                    .then(Commands.literal("on")
                        .executes(ctx -> cmdSpinTrack(ctx.getSource(),
                            StringArgumentType.getString(ctx, "id"), true)))
                    .then(Commands.literal("off")
                        .executes(ctx -> cmdSpinTrack(ctx.getSource(),
                            StringArgumentType.getString(ctx, "id"), false)))))
            .then(Commands.literal("hover")
                .then(Commands.argument("id", StringArgumentType.word()).suggests(HologramCommand::suggestHologramIds)
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
            // ── Visual appearance sub-commands ─────────────────────────────────
            .then(Commands.literal("scale")
                .then(Commands.argument("id", StringArgumentType.word()).suggests(HologramCommand::suggestHologramIds)
                    .then(Commands.argument("scale", FloatArgumentType.floatArg(0.1f, 10.0f))
                        .executes(ctx -> cmdScale(ctx.getSource(),
                            StringArgumentType.getString(ctx, "id"),
                            FloatArgumentType.getFloat(ctx, "scale"))))))
            .then(Commands.literal("linespacing")
                .then(Commands.argument("id", StringArgumentType.word()).suggests(HologramCommand::suggestHologramIds)
                    .then(Commands.argument("spacing", FloatArgumentType.floatArg(0.05f, 3.0f))
                        .executes(ctx -> cmdLineSpacing(ctx.getSource(),
                            StringArgumentType.getString(ctx, "id"),
                            FloatArgumentType.getFloat(ctx, "spacing"))))))
            .then(Commands.literal("shadow")
                .then(Commands.argument("id", StringArgumentType.word()).suggests(HologramCommand::suggestHologramIds)
                    .then(Commands.literal("on")
                        .executes(ctx -> cmdShadow(ctx.getSource(), StringArgumentType.getString(ctx, "id"), true)))
                    .then(Commands.literal("off")
                        .executes(ctx -> cmdShadow(ctx.getSource(), StringArgumentType.getString(ctx, "id"), false)))))
            .then(Commands.literal("seethrough")
                .then(Commands.argument("id", StringArgumentType.word()).suggests(HologramCommand::suggestHologramIds)
                    .then(Commands.literal("on")
                        .executes(ctx -> cmdSeeThrough(ctx.getSource(), StringArgumentType.getString(ctx, "id"), true)))
                    .then(Commands.literal("off")
                        .executes(ctx -> cmdSeeThrough(ctx.getSource(), StringArgumentType.getString(ctx, "id"), false)))))
            .then(Commands.literal("opacity")
                .then(Commands.argument("id", StringArgumentType.word()).suggests(HologramCommand::suggestHologramIds)
                    .then(Commands.argument("opacity", IntegerArgumentType.integer(0, 255))
                        .executes(ctx -> cmdOpacity(ctx.getSource(),
                            StringArgumentType.getString(ctx, "id"),
                            IntegerArgumentType.getInteger(ctx, "opacity"))))))
            .then(Commands.literal("viewrange")
                .then(Commands.argument("id", StringArgumentType.word()).suggests(HologramCommand::suggestHologramIds)
                    .then(Commands.argument("range", FloatArgumentType.floatArg(0.1f, 8.0f))
                        .executes(ctx -> cmdViewRange(ctx.getSource(),
                            StringArgumentType.getString(ctx, "id"),
                            FloatArgumentType.getFloat(ctx, "range"))))))
            .then(Commands.literal("linewidth")
                .then(Commands.argument("id", StringArgumentType.word()).suggests(HologramCommand::suggestHologramIds)
                    .then(Commands.argument("pixels", IntegerArgumentType.integer(1, 4096))
                        .executes(ctx -> cmdLineWidth(ctx.getSource(),
                            StringArgumentType.getString(ctx, "id"),
                            IntegerArgumentType.getInteger(ctx, "pixels"))))))
            .then(Commands.literal("clearlines")
                .then(Commands.argument("id", StringArgumentType.word()).suggests(HologramCommand::suggestHologramIds)
                    .executes(ctx -> cmdClearLines(ctx.getSource(), StringArgumentType.getString(ctx, "id")))))
            .then(Commands.literal("moveline")
                .then(Commands.argument("id", StringArgumentType.word()).suggests(HologramCommand::suggestHologramIds)
                    .then(Commands.argument("from", IntegerArgumentType.integer(1))
                        .then(Commands.argument("to", IntegerArgumentType.integer(1))
                            .executes(ctx -> cmdMoveLine(ctx.getSource(),
                                StringArgumentType.getString(ctx, "id"),
                                IntegerArgumentType.getInteger(ctx, "from"),
                                IntegerArgumentType.getInteger(ctx, "to")))))))
            .then(Commands.literal("background")
                .then(Commands.argument("id", StringArgumentType.word()).suggests(HologramCommand::suggestHologramIds)
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
    private static int cmdCopy(CommandSourceStack src, String id, String newId) {
        HologramData src2 = HologramManager.getInstance().getHologram(id);
        if (src2 == null) { src.sendFailure(Component.literal("§cHologram '§e" + id + "§c' not found.")); return 0; }
        if (HologramManager.getInstance().exists(newId)) {
            src.sendFailure(Component.literal("§cHologram '§e" + newId + "§c' already exists."));
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
        src.sendSuccess(() -> Component.literal("§a✓ Hologram '§e" + id + "§a' copied to '§e" + newId + "§a'."), true);
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
    private static int cmdInsertLine(CommandSourceStack src, String id, int userIndex, String text) {
        HologramData data = HologramManager.getInstance().getHologram(id);
        if (data == null) { src.sendFailure(Component.literal("§cHologram '§e" + id + "§c' not found.")); return 0; }
        // Line numbers are 1-based for players. Allow inserting after the last line too
        // (userIndex == size + 1), so valid input is 1..size+1.
        int index = userIndex - 1;
        if (index < 0 || index > data.lines.size()) {
            src.sendFailure(Component.literal("§cLine number out of range (1–" + (data.lines.size() + 1) + ")."));
            return 0;
        }
        data.lines.add(index, new HologramLine(text));
        HologramManager.getInstance().registerHologram(data);
        respawn(src, data);
        src.sendSuccess(() -> Component.literal("§a✓ Line inserted at §e" + userIndex + "§a in '§e" + id + "§a'. Total lines: " + data.lines.size()), true);
        return 1;
    }
    private static int cmdSetLine(CommandSourceStack src, String id, int userIndex, String text) {
        HologramData data = HologramManager.getInstance().getHologram(id);
        if (data == null) { src.sendFailure(Component.literal("§cHologram '§e" + id + "§c' not found.")); return 0; }
        int index = userIndex - 1;
        if (index < 0 || index >= data.lines.size()) { src.sendFailure(Component.literal("§cLine number out of range (1–" + data.lines.size() + ").")); return 0; }
        HologramLine line = data.lines.get(index);
        line.text = text;
        // Clear any frame animation so the new static text is shown immediately
        line.frames.clear();
        line.animFrameIntervalTicks = 0;
        line.currentFrame = 0;
        line.animTickCount = 0;
        HologramManager.getInstance().registerHologram(data);
        respawn(src, data);
        src.sendSuccess(() -> Component.literal("§a✔ Line §e" + userIndex + "§a updated."), true);
        return 1;
    }
    private static int cmdRemoveLine(CommandSourceStack src, String id, int userIndex) {
        HologramData data = HologramManager.getInstance().getHologram(id);
        if (data == null) { src.sendFailure(Component.literal("§cHologram '§e" + id + "§c' not found.")); return 0; }
        int index = userIndex - 1;
        if (index < 0 || index >= data.lines.size()) { src.sendFailure(Component.literal("§cLine number out of range (1–" + data.lines.size() + ").")); return 0; }
        data.lines.remove(index);
        HologramManager.getInstance().registerHologram(data);
        respawn(src, data);
        src.sendSuccess(() -> Component.literal("§a✓ Line §e" + userIndex + "§a removed."), true);
        return 1;
    }
    /**
     * /hologram addframes &lt;id&gt; &lt;lineIndex&gt; &lt;intervalTicks&gt; &lt;frame1|frame2|...&gt;
     * Frames are separated by {@code |}.
     */
    private static int cmdAddFrames(CommandSourceStack src, String id, int userLineIndex, int intervalTicks, String framesRaw) {
        HologramData data = HologramManager.getInstance().getHologram(id);
        if (data == null) { src.sendFailure(Component.literal("§cHologram '§e" + id + "§c' not found.")); return 0; }
        int lineIndex = userLineIndex - 1;
        if (lineIndex < 0 || lineIndex >= data.lines.size()) {
            src.sendFailure(Component.literal("§cLine number out of range (1–" + data.lines.size() + ")."));
            return 0;
        }
        String[] parts = framesRaw.split("\\|");
        if (parts.length < 2) {
            src.sendFailure(Component.literal("§cProvide at least 2 frames separated by §f|§c. Example: §fHello|World"));
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
        src.sendSuccess(() -> Component.literal("§a✓ §e" + fc + "§a frame(s) set on line §e" + userLineIndex + "§a of '§e" + id + "§a' (every §e" + intervalTicks + "§a ticks)."), true);
        return 1;
    }
    private static int cmdRemoveFrames(CommandSourceStack src, String id, int userLineIndex) {
        HologramData data = HologramManager.getInstance().getHologram(id);
        if (data == null) { src.sendFailure(Component.literal("§cHologram '§e" + id + "§c' not found.")); return 0; }
        int lineIndex = userLineIndex - 1;
        if (lineIndex < 0 || lineIndex >= data.lines.size()) {
            src.sendFailure(Component.literal("§cLine number out of range (1–" + data.lines.size() + ")."));
            return 0;
        }
        HologramLine line = data.lines.get(lineIndex);
        line.frames.clear();
        line.animFrameIntervalTicks = 0;
        line.currentFrame = 0;
        HologramManager.getInstance().registerHologram(data);
        respawn(src, data);
        src.sendSuccess(() -> Component.literal("§a✓ Frame animation removed from line §e" + userLineIndex + "§a of '§e" + id + "§a'."), true);
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
    private static int cmdMoveHere(CommandSourceStack src, String id) {
        if (!(src.getEntity() instanceof ServerPlayer player)) {
            src.sendFailure(Component.literal("§cThis command can only be run by a player."));
            return 0;
        }
        HologramData data = HologramManager.getInstance().getHologram(id);
        if (data == null) { src.sendFailure(Component.literal("§cHologram '§e" + id + "§c' not found.")); return 0; }
        ServerLevel level = getLevel(src, data.world);
        if (level != null) HologramRenderer.despawn(data, level);
        data.x = player.getX();
        data.y = player.getY() + 1.5; // eye-level ~ feet+1.5
        data.z = player.getZ();
        HologramManager.getInstance().registerHologram(data);
        if (level != null) HologramRenderer.spawn(data, level);
        double nx = data.x, ny = data.y, nz = data.z;
        src.sendSuccess(() -> Component.literal("§a✓ Hologram '§e" + id + "§a' moved to your position §7(" + fmt(nx) + ", " + fmt(ny) + ", " + fmt(nz) + ")§a."), true);
        return 1;
    }
    private static int cmdNear(CommandSourceStack src, double radius) {
        if (!(src.getEntity() instanceof ServerPlayer player)) {
            src.sendFailure(Component.literal("§cThis command can only be run by a player."));
            return 0;
        }
        String dimKey = HologramRenderer.dimensionKey(com.zerog.neoessentials.util.LevelCompat.of(player));
        double px = player.getX(), pz = player.getZ();
        List<HologramData> nearby = new ArrayList<>();
        for (HologramData d : HologramManager.getInstance().getAllHolograms()) {
            if (dimKey.equals(d.world) && d.distanceXZ(px, pz) <= radius) nearby.add(d);
        }
        if (nearby.isEmpty()) {
            src.sendSuccess(() -> Component.literal("§7No holograms within §e" + fmt(radius) + "§7 blocks."), false);
            return 1;
        }
        nearby.sort((a, b) -> Double.compare(a.distanceXZ(px, pz), b.distanceXZ(px, pz)));
        StringBuilder sb = new StringBuilder("§6Nearby holograms (").append(nearby.size()).append(") within §e").append(fmt(radius)).append("§6 blocks:\n");
        for (HologramData d : nearby) {
            double dist = d.distanceXZ(px, pz);
            sb.append("  §e").append(d.id)
              .append(" §7— §f(").append(fmt(d.x)).append(", ").append(fmt(d.y)).append(", ").append(fmt(d.z)).append(")")
              .append(" §8[").append(fmt(dist)).append("m]")
              .append(d.visible ? "" : " §8[hidden]").append("\n");
        }
        src.sendSuccess(() -> Component.literal(sb.toString().trim()), false);
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
        String[] bbNames = {"FIXED", "VERTICAL", "HORIZONTAL", "CENTER"};
        String bbName = (data.billboardMode >= 0 && data.billboardMode < bbNames.length)
            ? bbNames[data.billboardMode] : "UNKNOWN";
        StringBuilder sb = new StringBuilder();
        sb.append("§6=== Hologram: §e").append(data.id).append(" §6===\n");
        sb.append("§7World: §f").append(data.world).append("\n");
        sb.append("§7Position: §f(").append(fmt(data.x)).append(", ").append(fmt(data.y)).append(", ").append(fmt(data.z)).append(")\n");
        sb.append("§7Visible: ").append(data.visible ? "§ayes" : "§cno").append("\n");
        sb.append("§7Refresh: §f").append(data.refreshInterval == 0 ? "disabled" : data.refreshInterval + "s").append("\n");
        sb.append("§7Billboard: §e").append(bbName).append("\n");
        sb.append("§7Scale: §f").append(data.scale).append("x\n");
        sb.append("§7Line spacing: §f").append(data.lineSpacing).append(" blocks\n");
        sb.append("§7Shadow: ").append(data.textShadow ? "§aon" : "§8off").append("\n");
        sb.append("§7Opacity: §f").append(data.textOpacity).append("/255\n");
        sb.append("§7Background: §f").append(String.format("0x%08X", data.backgroundColorArgb)).append("\n");
        String[] alignNames = {"CENTER", "LEFT", "RIGHT"};
        String alignName = (data.textAlign >= 0 && data.textAlign < alignNames.length) ? alignNames[data.textAlign] : "UNKNOWN";
        sb.append("§7Text align: §e").append(alignName).append("\n");
        sb.append("§7See-through: ").append(data.seeThrough ? "§aon" : "§8off").append("\n");
        sb.append("§7Line width: §f").append(data.lineWidth).append("px\n");
        sb.append("§7View range: §f").append(data.viewRange).append("x (§f~").append(Math.round(data.viewRange * 64)).append(" blocks§7)\n");
        if (data.spinEnabled) {
            String trackNote = (data.spinAxis != null && data.spinAxis.equalsIgnoreCase("Y") && data.spinTrackPlayer)
                ? " \u00a78[player-tracking, billboard=FIXED]" : "";
            sb.append("\u00a77Spin: \u00a7eon \u00a77(\u00a7f").append(data.spinSpeedDegrees).append("\u00a7a\u00b0/tick\u00a77, axis=\u00a7f").append(data.spinAxis).append("\u00a77)").append(trackNote).append("\n");
        } else {
            sb.append("\u00a77Spin: \u00a78off\n");
        }
        if (data.hoverEnabled) {
            sb.append("§7Hover: §eon §7(amplitude=§f").append(data.hoverAmplitude).append("§7 blocks, speed=§f").append(data.hoverSpeedDegrees).append("§7°/tick)\n");
        } else {
            sb.append("§7Hover: §8off\n");
        }
        sb.append("§7Lines (").append(data.lines.size()).append("):\n");
        for (int i = 0; i < data.lines.size(); i++) {
            HologramLine line = data.lines.get(i);
            sb.append("  §e").append(i + 1).append("§7: §f").append(line.currentText());
            if (!line.frames.isEmpty()) sb.append(" §8[animated, ").append(line.frames.size()).append(" frames, every ").append(line.animFrameIntervalTicks).append("t]");
            sb.append("\n");
        }
        src.sendSuccess(() -> Component.literal(sb.toString().trim()), false);
        return 1;
    }
    // ── Billboard / spin / hover ───────────────────────────────────────────────
    private static int cmdBillboard(CommandSourceStack src, String id, String mode) {
        HologramData data = HologramManager.getInstance().getHologram(id);
        if (data == null) { src.sendFailure(Component.literal("§cHologram '§e" + id + "§c' not found.")); return 0; }
        int modeVal = switch (mode.toLowerCase()) {
            case "fixed"      -> 0;
            case "vertical"   -> 1;
            case "horizontal" -> 2;
            case "center"     -> 3;
            default -> -1;
        };
        if (modeVal < 0) {
            src.sendFailure(Component.literal("§cInvalid billboard mode. Use: §ffixed§c, §fvertical§c, §fhorizontal§c, §fcenter§c."));
            return 0;
        }
        data.billboardMode = modeVal;
        HologramManager.getInstance().registerHologram(data);
        respawn(src, data);
        String modeName = new String[]{"FIXED", "VERTICAL", "HORIZONTAL", "CENTER"}[modeVal];
        src.sendSuccess(() -> Component.literal("§a✓ Hologram '§e" + id + "§a' billboard set to §e" + modeName + "§a."), true);
        return 1;
    }
    private static int cmdSpinOn(CommandSourceStack src, String id, float speed, String axis) {
        HologramData data = HologramManager.getInstance().getHologram(id);
        if (data == null) { src.sendFailure(Component.literal("\u00a7cHologram '\u00a7e" + id + "\u00a7c' not found.")); return 0; }
        String axisUpper = axis.toUpperCase();
        if (!axisUpper.equals("X") && !axisUpper.equals("Y") && !axisUpper.equals("Z")) {
            src.sendFailure(Component.literal("\u00a7cInvalid axis '\u00a7e" + axis + "\u00a7c'. Use X, Y, or Z."));
            return 0;
        }
        data.spinEnabled      = true;
        data.spinSpeedDegrees = speed;
        data.spinAxis         = axisUpper;
        // Y-axis spin enables player-tracking by default so the text follows the viewer.
        // X / Z axis spins use the billboard setting directly (usually CENTER).
        if (axisUpper.equals("Y")) data.spinTrackPlayer = true;
        HologramManager.getInstance().registerHologram(data);
        respawn(src, data);
        String trackNote = axisUpper.equals("Y") ? " \u00a78[player-tracking ON]" : "";
        src.sendSuccess(() -> Component.literal("\u00a7a\u2714 Spin enabled for '\u00a7e" + id + "\u00a7a': \u00a7e" + speed
            + "\u00a7a\u00b0/tick on \u00a7e" + axisUpper + "\u00a7a axis." + trackNote), true);
        return 1;
    }
    /** /hologram spintrack <id> <on|off> — toggle player-tracking for Y-axis spin. */
    private static int cmdSpinTrack(CommandSourceStack src, String id, boolean on) {
        HologramData data = HologramManager.getInstance().getHologram(id);
        if (data == null) { src.sendFailure(Component.literal("\u00a7cHologram '\u00a7e" + id + "\u00a7c' not found.")); return 0; }
        data.spinTrackPlayer = on;
        HologramManager.getInstance().registerHologram(data);
        respawn(src, data);
        src.sendSuccess(() -> Component.literal("\u00a7a\u2714 Spin player-tracking " + (on ? "\u00a7aenabled" : "\u00a77disabled")
            + "\u00a7a for '\u00a7e" + id + "\u00a7a'." + (on ? " \u00a78(billboard auto-set to FIXED for Y axis)" : "")), true);
        return 1;
    }
    private static int cmdSpinOff(CommandSourceStack src, String id) {
        HologramData data = HologramManager.getInstance().getHologram(id);
        if (data == null) { src.sendFailure(Component.literal("§cHologram '§e" + id + "§c' not found.")); return 0; }
        data.spinEnabled = false;
        data.currentSpinAngle = 0f;
        HologramManager.getInstance().registerHologram(data);
        respawn(src, data);
        src.sendSuccess(() -> Component.literal("§a✓ Spin disabled for '§e" + id + "§a'."), true);
        return 1;
    }
    private static int cmdHoverOn(CommandSourceStack src, String id, float amplitude, float speed) {
        HologramData data = HologramManager.getInstance().getHologram(id);
        if (data == null) { src.sendFailure(Component.literal("§cHologram '§e" + id + "§c' not found.")); return 0; }
        data.hoverEnabled       = true;
        data.hoverAmplitude     = amplitude;
        data.hoverSpeedDegrees  = speed;
        HologramManager.getInstance().registerHologram(data);
        respawn(src, data);
        src.sendSuccess(() -> Component.literal("§a✓ Hover enabled for '§e" + id + "§a': §eamplitude=§f" + amplitude + "§a blocks, §espeed=§f" + speed + "§a°/tick."), true);
        return 1;
    }
    private static int cmdHoverOff(CommandSourceStack src, String id) {
        HologramData data = HologramManager.getInstance().getHologram(id);
        if (data == null) { src.sendFailure(Component.literal("§cHologram '§e" + id + "§c' not found.")); return 0; }
        data.hoverEnabled = false;
        data.hoverPhase   = 0f;
        HologramManager.getInstance().registerHologram(data);
        respawn(src, data);
        src.sendSuccess(() -> Component.literal("§a✓ Hover disabled for '§e" + id + "§a'."), true);
        return 1;
    }
    // ── Visual appearance commands ─────────────────────────────────────────────
    private static int cmdScale(CommandSourceStack src, String id, float scale) {
        HologramData data = HologramManager.getInstance().getHologram(id);
        if (data == null) { src.sendFailure(Component.literal("§cHologram '§e" + id + "§c' not found.")); return 0; }
        data.scale = scale;
        HologramManager.getInstance().registerHologram(data);
        respawn(src, data);
        src.sendSuccess(() -> Component.literal("§a✓ Hologram '§e" + id + "§a' scale set to §e" + scale + "x§a."), true);
        return 1;
    }
    private static int cmdLineSpacing(CommandSourceStack src, String id, float spacing) {
        HologramData data = HologramManager.getInstance().getHologram(id);
        if (data == null) { src.sendFailure(Component.literal("§cHologram '§e" + id + "§c' not found.")); return 0; }
        data.lineSpacing = spacing;
        HologramManager.getInstance().registerHologram(data);
        respawn(src, data);
        src.sendSuccess(() -> Component.literal("§a✓ Hologram '§e" + id + "§a' line spacing set to §e" + spacing + "§a blocks."), true);
        return 1;
    }
    private static int cmdShadow(CommandSourceStack src, String id, boolean on) {
        HologramData data = HologramManager.getInstance().getHologram(id);
        if (data == null) { src.sendFailure(Component.literal("§cHologram '§e" + id + "§c' not found.")); return 0; }
        data.textShadow = on;
        HologramManager.getInstance().registerHologram(data);
        respawn(src, data);
        src.sendSuccess(() -> Component.literal("\u00a7a\u2714 Text shadow " + (on ? "\u00a7aenabled" : "\u00a77disabled") + "\u00a7a for '\u00a7e" + id + "\u00a7a'."), true);
        return 1;
    }
    private static int cmdSeeThrough(CommandSourceStack src, String id, boolean on) {
        HologramData data = HologramManager.getInstance().getHologram(id);
        if (data == null) { src.sendFailure(Component.literal("\u00a7cHologram '\u00a7e" + id + "\u00a7c' not found.")); return 0; }
        data.seeThrough = on;
        HologramManager.getInstance().registerHologram(data);
        respawn(src, data);
        src.sendSuccess(() -> Component.literal("\u00a7a\u2714 See-through " + (on ? "\u00a7aenabled" : "\u00a77disabled") + "\u00a7a for '\u00a7e" + id + "\u00a7a'."), true);
        return 1;
    }
    private static int cmdViewRange(CommandSourceStack src, String id, float range) {
        HologramData data = HologramManager.getInstance().getHologram(id);
        if (data == null) { src.sendFailure(Component.literal("\u00a7cHologram '\u00a7e" + id + "\u00a7c' not found.")); return 0; }
        data.viewRange = range;
        HologramManager.getInstance().registerHologram(data);
        respawn(src, data);
        // Approximate display distance: vanilla base is ~64 blocks, multiplied by viewRange
        int approxBlocks = Math.round(range * 64);
        src.sendSuccess(() -> Component.literal("\u00a7a\u2714 View range for '\u00a7e" + id + "\u00a7a' set to \u00a7e" + range + "\u00a7a (approx \u00a7e" + approxBlocks + "\u00a7a blocks)."), true);
        return 1;
    }
    private static int cmdLineWidth(CommandSourceStack src, String id, int pixels) {
        HologramData data = HologramManager.getInstance().getHologram(id);
        if (data == null) { src.sendFailure(Component.literal("\u00a7cHologram '\u00a7e" + id + "\u00a7c' not found.")); return 0; }
        data.lineWidth = pixels;
        HologramManager.getInstance().registerHologram(data);
        respawn(src, data);
        src.sendSuccess(() -> Component.literal("\u00a7a\u2714 Line width for '\u00a7e" + id + "\u00a7a' set to \u00a7e" + pixels + "\u00a7a pixels."), true);
        return 1;
    }
    /** /hologram clearlines <id> – remove all lines from a hologram. */
    private static int cmdClearLines(CommandSourceStack src, String id) {
        HologramData data = HologramManager.getInstance().getHologram(id);
        if (data == null) { src.sendFailure(Component.literal("\u00a7cHologram '\u00a7e" + id + "\u00a7c' not found.")); return 0; }
        int count = data.lines.size();
        data.lines.clear();
        HologramManager.getInstance().registerHologram(data);
        respawn(src, data);
        src.sendSuccess(() -> Component.literal("\u00a7a\u2714 Cleared \u00a7e" + count + "\u00a7a line(s) from '\u00a7e" + id + "\u00a7a'."), true);
        return 1;
    }
    /** /hologram moveline <id> <from> <to> – reorder a line within the hologram. */
    private static int cmdMoveLine(CommandSourceStack src, String id, int userFrom, int userTo) {
        HologramData data = HologramManager.getInstance().getHologram(id);
        if (data == null) { src.sendFailure(Component.literal("\u00a7cHologram '\u00a7e" + id + "\u00a7c' not found.")); return 0; }
        int size = data.lines.size();
        int from = userFrom - 1;
        if (from < 0 || from >= size) {
            src.sendFailure(Component.literal("\u00a7c'from' line number \u00a7e" + userFrom + "\u00a7c out of range (1\u2013" + size + ")."));
            return 0;
        }
        int clampedTo = Math.max(0, Math.min(size - 1, userTo - 1));
        if (from == clampedTo) {
            src.sendSuccess(() -> Component.literal("\u00a77Line is already at position \u00a7e" + userFrom + "\u00a77."), false);
            return 1;
        }
        HologramLine line = data.lines.remove(from);
        data.lines.add(clampedTo, line);
        HologramManager.getInstance().registerHologram(data);
        respawn(src, data);
        int newPos = clampedTo + 1;
        src.sendSuccess(() -> Component.literal("\u00a7a\u2714 Line moved from \u00a7e" + userFrom + "\u00a7a to \u00a7e" + newPos + "\u00a7a in '\u00a7e" + id + "\u00a7a'."), true);
        return 1;
    }
    private static int cmdOpacity(CommandSourceStack src, String id, int opacity) {
        HologramData data = HologramManager.getInstance().getHologram(id);
        if (data == null) { src.sendFailure(Component.literal("§cHologram '§e" + id + "§c' not found.")); return 0; }
        data.textOpacity = Math.max(0, Math.min(255, opacity));
        HologramManager.getInstance().registerHologram(data);
        respawn(src, data);
        src.sendSuccess(() -> Component.literal("§a✓ Hologram '§e" + id + "§a' opacity set to §e" + data.textOpacity + "/255§a."), true);
        return 1;
    }
    /**
     * /hologram background &lt;id&gt; &lt;transparent|#RRGGBB|#AARRGGBB&gt;
     * Examples: transparent, #000000, #40000000
     */
    private static int cmdBackground(CommandSourceStack src, String id, String colorStr) {
        HologramData data = HologramManager.getInstance().getHologram(id);
        if (data == null) { src.sendFailure(Component.literal("§cHologram '§e" + id + "§c' not found.")); return 0; }
        int argb;
        try {
            if (colorStr.equalsIgnoreCase("transparent") || colorStr.equals("0")) {
                argb = 0x00000000;
            } else {
                String hex = colorStr.startsWith("#") ? colorStr.substring(1) : colorStr;
                if (hex.length() == 6) hex = "FF" + hex;   // default to fully opaque when no alpha given
                else if (hex.length() == 8) { /* full AARRGGBB as-is */ }
                else { src.sendFailure(Component.literal("§cInvalid colour. Use §ftransparent§c, §f#RRGGBB§c, or §f#AARRGGBB§c.")); return 0; }
                argb = (int) Long.parseLong(hex, 16);
            }
        } catch (NumberFormatException ex) {
            src.sendFailure(Component.literal("§cInvalid colour value '§e" + colorStr + "§c'. Use §ftransparent§c, §f#RRGGBB§c, or §f#AARRGGBB§c."));
            return 0;
        }
        data.backgroundColorArgb = argb;
        HologramManager.getInstance().registerHologram(data);
        respawn(src, data);
        final String hex = String.format("0x%08X", argb);
        src.sendSuccess(() -> Component.literal("§a✓ Hologram '§e" + id + "§a' background set to §e" + hex + "§a."), true);
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
            // Shop holograms need NBT_SHOP_KEY re-applied after a full respawn — the generic
            // spawnAllForWorld() path has no concept of shops (see retagAllShopHolograms doc).
            com.zerog.neoessentials.hologram.integration.ShopHologramManager.retagAllShopHolograms();
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
                return com.zerog.neoessentials.util.PermissionLevelCompat.hasPermission(player, 4) ||
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
    /** /hologram create <id>  – create at the executing player's position (eye-level). */
    private static int cmdCreateHere(CommandSourceStack src, String id) {
        if (!(src.getEntity() instanceof ServerPlayer player)) {
            src.sendFailure(Component.literal("\u00a7cThis command can only be run by a player."));
            return 0;
        }
        if (HologramManager.getInstance().exists(id)) {
            src.sendFailure(Component.literal("\u00a7cHologram '\u00a7e" + id + "\u00a7c' already exists."));
            return 0;
        }
        HologramData data = new HologramData();
        data.id = id.toLowerCase();
        data.x = player.getX();
        data.y = player.getY() + 1.5;
        data.z = player.getZ();
        data.world = HologramRenderer.dimensionKey(com.zerog.neoessentials.util.LevelCompat.of(player));
        data.refreshInterval = 5;
        HologramManager.getInstance().registerHologram(data);
        HologramRenderer.spawn(data, com.zerog.neoessentials.util.LevelCompat.of(player));
        double nx = data.x, ny = data.y, nz = data.z;
        src.sendSuccess(() -> Component.literal("\u00a7a\u2714 Hologram '\u00a7e" + id + "\u00a7a' created at your position \u00a77(" + fmt(nx) + ", " + fmt(ny) + ", " + fmt(nz) + ")\u00a7a.\n\u00a77Use \u00a7f/hologram addline " + id + " <text> \u00a77to add lines."), true);
        return 1;
    }
    /** /hologram rename <id> <newid> – rename a hologram in-place. */
    private static int cmdRename(CommandSourceStack src, String id, String newId) {
        HologramData data = HologramManager.getInstance().getHologram(id);
        if (data == null) { src.sendFailure(Component.literal("\u00a7cHologram '\u00a7e" + id + "\u00a7c' not found.")); return 0; }
        String newIdLower = newId.toLowerCase();
        if (HologramManager.getInstance().exists(newIdLower)) {
            src.sendFailure(Component.literal("\u00a7cA hologram named '\u00a7e" + newIdLower + "\u00a7c' already exists."));
            return 0;
        }
        ServerLevel level = getLevel(src, data.world);
        if (level != null) HologramRenderer.despawn(data, level);
        HologramManager.getInstance().removeHologram(id);
        data.id = newIdLower;
        HologramManager.getInstance().registerHologram(data);
        if (level != null) HologramRenderer.spawn(data, level);
        src.sendSuccess(() -> Component.literal("\u00a7a\u2714 Hologram '\u00a7e" + id + "\u00a7a' renamed to '\u00a7e" + newIdLower + "\u00a7a'."), true);
        return 1;
    }
    /** /hologram align <id> – snap X/Z to the nearest block centre (+0.5). */
    private static int cmdAlign(CommandSourceStack src, String id) {
        HologramData data = HologramManager.getInstance().getHologram(id);
        if (data == null) { src.sendFailure(Component.literal("\u00a7cHologram '\u00a7e" + id + "\u00a7c' not found.")); return 0; }
        ServerLevel level = getLevel(src, data.world);
        if (level != null) HologramRenderer.despawn(data, level);
        data.x = Math.floor(data.x) + 0.5;
        data.z = Math.floor(data.z) + 0.5;
        HologramManager.getInstance().registerHologram(data);
        if (level != null) HologramRenderer.spawn(data, level);
        double nx = data.x, ny = data.y, nz = data.z;
        src.sendSuccess(() -> Component.literal("\u00a7a\u2714 Hologram '\u00a7e" + id + "\u00a7a' aligned to block centre \u00a77(" + fmt(nx) + ", " + fmt(ny) + ", " + fmt(nz) + ")\u00a7a."), true);
        return 1;
    }
    /** /hologram tp <id> – teleport the executing player to the hologram. */
    private static int cmdTp(CommandSourceStack src, String id) {
        if (!(src.getEntity() instanceof ServerPlayer player)) {
            src.sendFailure(Component.literal("\u00a7cThis command can only be run by a player."));
            return 0;
        }
        HologramData data = HologramManager.getInstance().getHologram(id);
        if (data == null) { src.sendFailure(Component.literal("\u00a7cHologram '\u00a7e" + id + "\u00a7c' not found.")); return 0; }
        ServerLevel targetLevel = getLevel(src, data.world);
        if (targetLevel == null) {
            src.sendFailure(Component.literal("\u00a7cCould not find world '\u00a7e" + data.world + "\u00a7c'."));
            return 0;
        }
        player.teleportTo(
                targetLevel,
                data.x,
                data.y,
                data.z,
                java.util.Set.of(),
                player.getYRot(),
                player.getXRot(),
                true
            );
        src.sendSuccess(() -> Component.literal("\u00a7a\u2714 Teleported to hologram '\u00a7e" + id + "\u00a7a' at \u00a77(" + fmt(data.x) + ", " + fmt(data.y) + ", " + fmt(data.z) + ")\u00a7a."), true);
        return 1;
    }
    /** /hologram textalign <id> <center|left|right> */
    private static int cmdTextAlign(CommandSourceStack src, String id, String align) {
        HologramData data = HologramManager.getInstance().getHologram(id);
        if (data == null) { src.sendFailure(Component.literal("\u00a7cHologram '\u00a7e" + id + "\u00a7c' not found.")); return 0; }
        int alignVal = switch (align.toLowerCase()) {
            case "center", "centre" -> 0;
            case "left"             -> 1;
            case "right"            -> 2;
            default -> -1;
        };
        if (alignVal < 0) {
            src.sendFailure(Component.literal("\u00a7cInvalid alignment. Use: \u00a7fcenter\u00a7c, \u00a7fleft\u00a7c, or \u00a7fright\u00a7c."));
            return 0;
        }
        data.textAlign = alignVal;
        HologramManager.getInstance().registerHologram(data);
        respawn(src, data);
        String alignName = new String[]{"CENTER", "LEFT", "RIGHT"}[alignVal];
        src.sendSuccess(() -> Component.literal("\u00a7a\u2714 Hologram '\u00a7e" + id + "\u00a7a' text alignment set to \u00a7e" + alignName + "\u00a7a."), true);
        return 1;
    }
}
