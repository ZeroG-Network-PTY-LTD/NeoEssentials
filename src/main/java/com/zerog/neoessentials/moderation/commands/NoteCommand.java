package com.zerog.neoessentials.moderation.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.zerog.neoessentials.moderation.NoteEntry;
import com.zerog.neoessentials.moderation.NoteManager;
import com.zerog.neoessentials.util.MessageUtil;
import com.zerog.neoessentials.util.PermissionValidator;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.server.level.ServerPlayer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.UUID;

/**
 * Staff notes commands:
 *   /note <player> <text>            — Add a note to a player's record
 *   /notes <player>                  — View a player's notes
 *   /removenote <player> <noteId>    — Remove a single note by its ID
 */
public class NoteCommand {

    private static final Logger LOGGER = LoggerFactory.getLogger(NoteCommand.class);
    private static final int NOTES_PER_PAGE = 5;

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {

        // /note <player> <text>
        dispatcher.register(Commands.literal("note")
            .requires(src -> PermissionValidator.validatePermission(src, "neoessentials.moderation.note").hasPermission())
            .then(Commands.argument("player", StringArgumentType.word())
                .suggests((ctx, b) -> SharedSuggestionProvider.suggest(
                    ctx.getSource().getServer().getPlayerNames(), b))
                .then(Commands.argument("text", StringArgumentType.greedyString())
                    .executes(ctx -> executeNote(ctx,
                        StringArgumentType.getString(ctx, "player"),
                        StringArgumentType.getString(ctx, "text")))
                )
            )
        );

        // /notes <player>
        dispatcher.register(Commands.literal("notes")
            .requires(src -> PermissionValidator.validatePermission(src, "neoessentials.moderation.notes").hasPermission())
            .then(Commands.argument("player", StringArgumentType.word())
                .suggests((ctx, b) -> SharedSuggestionProvider.suggest(
                    ctx.getSource().getServer().getPlayerNames(), b))
                .executes(ctx -> executeNotes(ctx, StringArgumentType.getString(ctx, "player")))
            )
        );

        // /removenote <player> <noteId>
        dispatcher.register(Commands.literal("removenote")
            .requires(src -> PermissionValidator.validatePermission(src, "neoessentials.moderation.note").hasPermission())
            .then(Commands.argument("player", StringArgumentType.word())
                .then(Commands.argument("noteId", StringArgumentType.word())
                    .executes(ctx -> executeRemoveNote(ctx,
                        StringArgumentType.getString(ctx, "player"),
                        StringArgumentType.getString(ctx, "noteId")))
                )
            )
        );
    }

    // ── /note ────────────────────────────────────────────────────────────────

    private static int executeNote(CommandContext<CommandSourceStack> ctx, String playerName, String text) {
        CommandSourceStack source = ctx.getSource();
        String authorName = getCommandSender(source);
        UUID authorId = getCommandSenderUUID(source);

        UUID targetId = resolvePlayerUUID(ctx, playerName);
        if (targetId == null) {
            source.sendFailure(MessageUtil.error("neoessentials.moderation.player_not_found", playerName));
            return 0;
        }

        NoteEntry entry = NoteManager.getInstance().addNote(targetId, playerName, authorId, authorName, text);

        final String fShortId = entry.getId().substring(0, 8);
        source.sendSuccess(() -> MessageUtil.component("commands.neoessentials.note.added",
            playerName, text, fShortId), true);

        LOGGER.info("[Note] {} added a note on {}: {} (ID: {})", authorName, playerName, text, entry.getId().substring(0, 8));
        return 1;
    }

    // ── /notes ───────────────────────────────────────────────────────────────

    private static int executeNotes(CommandContext<CommandSourceStack> ctx, String playerName) {
        CommandSourceStack source = ctx.getSource();

        UUID targetId = resolvePlayerUUID(ctx, playerName);
        if (targetId == null) {
            source.sendFailure(MessageUtil.error("neoessentials.moderation.player_not_found", playerName));
            return 0;
        }

        List<NoteEntry> notes = NoteManager.getInstance().getNotes(targetId);

        if (notes.isEmpty()) {
            source.sendSuccess(() -> MessageUtil.component("commands.neoessentials.note.none_found", playerName), false);
            return 1;
        }

        final int fNoteCount = notes.size();
        source.sendSuccess(() -> MessageUtil.component("commands.neoessentials.note.list_header", playerName, fNoteCount), false);

        int display = Math.min(notes.size(), NOTES_PER_PAGE);
        for (int i = 0; i < display; i++) {
            NoteEntry n = notes.get(i);
            String shortId = n.getId().substring(0, 8);
            source.sendSuccess(() -> MessageUtil.component("commands.neoessentials.note.list_entry",
                shortId, n.getFormattedTime(), n.getAuthorName(), n.getText()), false);
        }
        if (notes.size() > NOTES_PER_PAGE) {
            final int more = notes.size() - NOTES_PER_PAGE;
            source.sendSuccess(() -> MessageUtil.component("commands.neoessentials.note.list_more", more), false);
        }
        return 1;
    }

    // ── /removenote ──────────────────────────────────────────────────────────

    private static int executeRemoveNote(CommandContext<CommandSourceStack> ctx, String playerName, String noteId) {
        CommandSourceStack source = ctx.getSource();

        UUID targetId = resolvePlayerUUID(ctx, playerName);
        if (targetId == null) {
            source.sendFailure(MessageUtil.error("neoessentials.moderation.player_not_found", playerName));
            return 0;
        }

        // The user may provide only the 8-char shortId prefix; resolve to full ID
        List<NoteEntry> notes = NoteManager.getInstance().getNotes(targetId);
        String fullId = notes.stream()
            .filter(n -> n.getId().startsWith(noteId) || n.getId().equals(noteId))
            .map(NoteEntry::getId)
            .findFirst()
            .orElse(null);

        if (fullId == null) {
            source.sendFailure(MessageUtil.component("commands.neoessentials.note.remove_id_not_found", noteId, playerName));
            return 0;
        }

        boolean removed = NoteManager.getInstance().removeNote(targetId, fullId);
        if (removed) {
            String sender = getCommandSender(source);
            LOGGER.info("[Note] {} removed note {} from {}", sender, noteId, playerName);
            source.sendSuccess(() -> MessageUtil.component("commands.neoessentials.note.removed", noteId, playerName), false);
        } else {
            source.sendFailure(MessageUtil.component("commands.neoessentials.note.remove_failed", playerName));
        }
        return removed ? 1 : 0;
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private static String getCommandSender(CommandSourceStack source) {
        if (source.getEntity() instanceof ServerPlayer p) return p.getName().getString();
        return "Console";
    }

    private static UUID getCommandSenderUUID(CommandSourceStack source) {
        if (source.getEntity() instanceof ServerPlayer p) return p.getUUID();
        return null;
    }

    /**
     * Resolves a player UUID by name. Checks online players first, then existing note
     * records for offline players (mirrors {@code WarnCommand}'s resolution strategy).
     */
    private static UUID resolvePlayerUUID(CommandContext<CommandSourceStack> ctx, String playerName) {
        ServerPlayer online = ctx.getSource().getServer().getPlayerList().getPlayerByName(playerName);
        if (online != null) return online.getUUID();
        return NoteManager.getInstance().findUUIDByName(playerName);
    }
}
