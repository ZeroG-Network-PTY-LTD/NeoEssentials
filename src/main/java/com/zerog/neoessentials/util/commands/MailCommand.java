package com.zerog.neoessentials.util.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.zerog.neoessentials.chat.IgnoreManager;
import com.zerog.neoessentials.chat.MuteManager;
import com.zerog.neoessentials.config.ConfigManager;
import com.zerog.neoessentials.util.CommandSourceHelper;
import com.zerog.neoessentials.util.MessageUtil;
import com.zerog.neoessentials.util.PermissionValidator;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.zerog.neoessentials.logging.LogCategory;
import com.zerog.neoessentials.logging.NeoLog;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * /mail command — player-to-player mail system.
 *
 * Ported from EssentialsX's Commandmail / MailServiceImpl:
 *  - read [page]
 *  - send <player> <message>
 *  - sendtemp <player> <duration> <message>   (timed / expiring mail)
 *  - sendall <message>                         (admin broadcast to all)
 *  - sendtempall <duration> <message>          (admin timed broadcast)
 *  - clear [index]                             (clear own mail or by index)
 *  - clear <player> [index]                    (admin: clear another's mail)
 *  - clearall                                  (admin: wipe every player's mailbox)
 *  - delete <id>                               (delete single message by short ID)
 *
 * Extras vs Essentials:
 *  - Mute check (muted players cannot send)
 *  - Ignore check (silently drops mail from ignored senders)
 *  - Per-minute rate limit
 *  - Console can send via /mail send <player> <message>
 */
public class MailCommand {
    private static final Logger LOGGER = LoggerFactory.getLogger(MailCommand.class);

    // ── Storage ──────────────────────────────────────────────────────────────
    private static final Map<UUID, List<MailMessage>> MAIL_BOX = new ConcurrentHashMap<>();
    private static final Path MAIL_DATA_FILE =
        Paths.get("config", "neoessentials", "mail_data.json");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    private static final DateTimeFormatter TIME_FORMAT =
        DateTimeFormatter.ofPattern("MM/dd/yyyy HH:mm");

    // ── Rate limiting (matches Essentials: configurable mails-per-minute) ───
    private static final AtomicInteger mailsThisMinute = new AtomicInteger(0);
    private static final AtomicLong   rateLimitWindowStart = new AtomicLong(0L);
    private static final int DEFAULT_MAILS_PER_MINUTE = 10;

    // ── Config constants ─────────────────────────────────────────────────────
    private static final int MAX_MESSAGE_LENGTH = 1000;   // matches Essentials
    private static final int MAX_MAILBOX_SIZE   = 100;
    private static final int ITEMS_PER_PAGE     = 9;      // matches Essentials

    // ────────────────────────────────────────────────────────────────────────
    // Mail message data model
    // ────────────────────────────────────────────────────────────────────────
    private static class MailMessage {
        String id;
        String senderName;   // display name / "Console"
        String senderUuid;   // null for console/legacy
        String message;
        long   timeSent;     // epoch millis, 0 = legacy
        long   timeExpire;   // epoch millis, 0 = never
        boolean read;
        boolean legacy;      // true for pre-uuid messages loaded from old files

        /** Normal constructor (player or console send). */
        MailMessage(String senderName, String senderUuid, String message, long timeExpire) {
            this.id          = UUID.randomUUID().toString().substring(0, 8);
            this.senderName  = senderName;
            this.senderUuid  = senderUuid;
            this.message     = message;
            this.timeSent    = System.currentTimeMillis();
            this.timeExpire  = timeExpire;
            this.read        = false;
            this.legacy      = false;
        }

        /** Legacy constructor for loading old format from disk. */
        MailMessage(String senderName, String message) {
            this(senderName, null, message, 0L);
            this.legacy = true;
        }

        boolean isExpired() {
            return timeExpire > 0 && System.currentTimeMillis() > timeExpire;
        }

        String formattedTime() {
            if (timeSent == 0) return "unknown";
            return LocalDateTime.ofInstant(Instant.ofEpochMilli(timeSent), ZoneId.systemDefault())
                .format(TIME_FORMAT);
        }

        String formattedExpiry() {
            if (timeExpire == 0) return "never";
            return LocalDateTime.ofInstant(Instant.ofEpochMilli(timeExpire), ZoneId.systemDefault())
                .format(TIME_FORMAT);
        }
    }

    // ────────────────────────────────────────────────────────────────────────
    // Registration
    // ────────────────────────────────────────────────────────────────────────
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        if (!ConfigManager.getInstance().isCommandEnabled("mail")) return;
        loadMailData();

        dispatcher.register(Commands.literal("mail")
            // /mail  (no args) — show status
            .executes(ctx -> {
                ServerPlayer player = CommandSourceHelper.requirePlayer(
                    ctx.getSource(), "commands.neoessentials.mail.player_only");
                if (player == null) return 0;
                return checkPerm(ctx.getSource(), "neoessentials.mail") ?
                    showMailStatus(player) : 0;
            })

            // /mail read [page]
            .then(Commands.literal("read")
                .executes(ctx -> {
                    ServerPlayer p = CommandSourceHelper.requirePlayer(
                        ctx.getSource(), "commands.neoessentials.mail.player_only");
                    if (p == null) return 0;
                    return checkPerm(ctx.getSource(), "neoessentials.mail") ?
                        readMail(p, 1) : 0;
                })
                .then(Commands.argument("page", IntegerArgumentType.integer(1))
                    .executes(ctx -> {
                        ServerPlayer p = CommandSourceHelper.requirePlayer(
                            ctx.getSource(), "commands.neoessentials.mail.player_only");
                        if (p == null) return 0;
                        return checkPerm(ctx.getSource(), "neoessentials.mail") ?
                            readMail(p, IntegerArgumentType.getInteger(ctx, "page")) : 0;
                    })
                )
            )

            // /mail send <player> <message>
            .then(Commands.literal("send")
                .then(Commands.argument("player", StringArgumentType.word())
                    .then(Commands.argument("message", StringArgumentType.greedyString())
                        .executes(ctx -> {
                            if (!checkPerm(ctx.getSource(), "neoessentials.mail.send")) return 0;
                            String targetName = StringArgumentType.getString(ctx, "player");
                            String message    = StringArgumentType.getString(ctx, "message");
                            ServerPlayer sender = ctx.getSource().getPlayer(); // null = console
                            return sendMail(ctx.getSource(), sender, targetName, message, 0L);
                        })
                    )
                )
            )

            // /mail sendtemp <player> <duration> <message>
            .then(Commands.literal("sendtemp")
                .then(Commands.argument("player", StringArgumentType.word())
                    .then(Commands.argument("duration", StringArgumentType.word())
                        .then(Commands.argument("message", StringArgumentType.greedyString())
                            .executes(ctx -> {
                                if (!checkPerm(ctx.getSource(), "neoessentials.mail.sendtemp")) return 0;
                                String targetName = StringArgumentType.getString(ctx, "player");
                                String duration   = StringArgumentType.getString(ctx, "duration");
                                String message    = StringArgumentType.getString(ctx, "message");
                                ServerPlayer sender = ctx.getSource().getPlayer();
                                long expireAt = parseDuration(duration);
                                if (expireAt < 0) {
                                    ctx.getSource().sendFailure(MessageUtil.error(
                                        "commands.neoessentials.mail.invalid_duration", duration));
                                    return 0;
                                }
                                return sendMail(ctx.getSource(), sender, targetName, message,
                                    System.currentTimeMillis() + expireAt);
                            })
                        )
                    )
                )
            )

            // /mail sendall <message>  (admin)
            .then(Commands.literal("sendall")
                .then(Commands.argument("message", StringArgumentType.greedyString())
                    .executes(ctx -> {
                        if (!checkPerm(ctx.getSource(), "neoessentials.mail.sendall")) return 0;
                        String message = StringArgumentType.getString(ctx, "message");
                        String senderName = ctx.getSource().getPlayer() != null
                            ? ctx.getSource().getPlayer().getName().getString() : "Console";
                        String senderUuid = ctx.getSource().getPlayer() != null
                            ? ctx.getSource().getPlayer().getUUID().toString() : null;
                        return sendMailAll(ctx.getSource(), senderName, senderUuid, message, 0L);
                    })
                )
            )

            // /mail sendtempall <duration> <message>  (admin)
            .then(Commands.literal("sendtempall")
                .then(Commands.argument("duration", StringArgumentType.word())
                    .then(Commands.argument("message", StringArgumentType.greedyString())
                        .executes(ctx -> {
                            if (!checkPerm(ctx.getSource(), "neoessentials.mail.sendtempall")) return 0;
                            String duration = StringArgumentType.getString(ctx, "duration");
                            String message  = StringArgumentType.getString(ctx, "message");
                            long expireAt = parseDuration(duration);
                            if (expireAt < 0) {
                                ctx.getSource().sendFailure(MessageUtil.error(
                                    "commands.neoessentials.mail.invalid_duration", duration));
                                return 0;
                            }
                            String senderName = ctx.getSource().getPlayer() != null
                                ? ctx.getSource().getPlayer().getName().getString() : "Console";
                            String senderUuid = ctx.getSource().getPlayer() != null
                                ? ctx.getSource().getPlayer().getUUID().toString() : null;
                            return sendMailAll(ctx.getSource(), senderName, senderUuid, message,
                                System.currentTimeMillis() + expireAt);
                        })
                    )
                )
            )

            // /mail delete <id>
            .then(Commands.literal("delete")
                .then(Commands.argument("id", StringArgumentType.word())
                    .executes(ctx -> {
                        ServerPlayer p = CommandSourceHelper.requirePlayer(
                            ctx.getSource(), "commands.neoessentials.mail.player_only");
                        if (p == null) return 0;
                        return checkPerm(ctx.getSource(), "neoessentials.mail") ?
                            deleteMail(p, StringArgumentType.getString(ctx, "id")) : 0;
                    })
                )
            )

            // /mail clear [index | player [index]]
            .then(Commands.literal("clear")
                // /mail clear  — wipe own mailbox
                .executes(ctx -> {
                    ServerPlayer p = CommandSourceHelper.requirePlayer(
                        ctx.getSource(), "commands.neoessentials.mail.player_only");
                    if (p == null) return 0;
                    return checkPerm(ctx.getSource(), "neoessentials.mail.clear") ?
                        clearMail(ctx.getSource(), p, -1) : 0;
                })
                // /mail clear <index-or-player>
                .then(Commands.argument("indexOrPlayer", StringArgumentType.word())
                    .executes(ctx -> {
                        String arg = StringArgumentType.getString(ctx, "indexOrPlayer");
                        if (isPositiveInt(arg)) {
                            // /mail clear <index>
                            ServerPlayer p = CommandSourceHelper.requirePlayer(
                                ctx.getSource(), "commands.neoessentials.mail.player_only");
                            if (p == null) return 0;
                            return checkPerm(ctx.getSource(), "neoessentials.mail.clear") ?
                                clearMail(ctx.getSource(), p, Integer.parseInt(arg)) : 0;
                        } else {
                            // /mail clear <player>  (admin)
                            if (!checkPerm(ctx.getSource(), "neoessentials.mail.clear.others")) return 0;
                            ServerPlayer target = ctx.getSource().getServer()
                                .getPlayerList().getPlayerByName(arg);
                            if (target == null) {
                                ctx.getSource().sendFailure(MessageUtil.error(
                                    "commands.neoessentials.mail.player_not_found", arg));
                                return 0;
                            }
                            return clearMail(ctx.getSource(), target, -1);
                        }
                    })
                    // /mail clear <player> <index>  (admin)
                    .then(Commands.argument("index", IntegerArgumentType.integer(1))
                        .executes(ctx -> {
                            if (!checkPerm(ctx.getSource(), "neoessentials.mail.clear.others")) return 0;
                            String playerName = StringArgumentType.getString(ctx, "indexOrPlayer");
                            int index = IntegerArgumentType.getInteger(ctx, "index");
                            ServerPlayer target = ctx.getSource().getServer()
                                .getPlayerList().getPlayerByName(playerName);
                            if (target == null) {
                                ctx.getSource().sendFailure(MessageUtil.error(
                                    "commands.neoessentials.mail.player_not_found", playerName));
                                return 0;
                            }
                            return clearMail(ctx.getSource(), target, index);
                        })
                    )
                )
            )

            // /mail clearall  (admin — wipe ALL players' mailboxes)
            .then(Commands.literal("clearall")
                .executes(ctx -> {
                    if (!checkPerm(ctx.getSource(), "neoessentials.mail.clearall")) return 0;
                    return clearAll(ctx.getSource());
                })
            )
        );
    }

    // ────────────────────────────────────────────────────────────────────────
    // Handlers
    // ────────────────────────────────────────────────────────────────────────

    private static int showMailStatus(ServerPlayer player) {
        List<MailMessage> msgs = MAIL_BOX.get(player.getUUID());
        if (msgs == null || msgs.isEmpty()) {
            player.sendSystemMessage(MessageUtil.info("commands.neoessentials.mail.no_mail"));
            return 1;
        }
        long unread = msgs.stream().filter(m -> !m.read && !m.isExpired()).count();
        player.sendSystemMessage(MessageUtil.info("commands.neoessentials.mail.status",
            msgs.size(), unread));
        player.sendSystemMessage(MessageUtil.component("commands.neoessentials.mail.status_hint"));
        return 1;
    }

    private static int readMail(ServerPlayer player, int page) {
        List<MailMessage> msgs = MAIL_BOX.computeIfAbsent(player.getUUID(), k -> new ArrayList<>());

        // Remove expired messages (Essentials: iterator.remove() on expired)
        boolean removed = msgs.removeIf(MailMessage::isExpired);

        if (msgs.isEmpty()) {
            if (removed) saveMailData();
            player.sendSystemMessage(MessageUtil.info("commands.neoessentials.mail.no_mail"));
            return 1;
        }

        int totalPages = (int) Math.ceil((double) msgs.size() / (double) ITEMS_PER_PAGE);
        if (page > totalPages) {
            player.sendSystemMessage(MessageUtil.error(
                "commands.neoessentials.mail.invalid_page", page, totalPages));
            return 0;
        }

        int start = (page - 1) * ITEMS_PER_PAGE;
        int end   = Math.min(start + ITEMS_PER_PAGE, msgs.size());

        player.sendSystemMessage(MessageUtil.component("commands.neoessentials.mail.header", page, totalPages));

        for (int i = start; i < end; i++) {
            MailMessage mail = msgs.get(i);
            int displayIndex = i + 1;

            // Capture read state BEFORE marking as read (Essentials: iterator.set() to replace with read=true)
            boolean wasUnread = !mail.read;
            mail.read = true;

            String unreadMarker = wasUnread ? MessageUtil.localize("commands.neoessentials.mail.unread_marker") : "";
            String expireInfo   = mail.timeExpire > 0
                ? MessageUtil.localize("commands.neoessentials.mail.expire_info", mail.formattedExpiry()) : "";

            MutableComponent line = (MutableComponent) MessageUtil.component("commands.neoessentials.mail.entry",
                displayIndex, unreadMarker, mail.senderName, mail.message, expireInfo);

            // Hover: full details; click: suggest delete
            MutableComponent hover = ((MutableComponent) MessageUtil.component("commands.neoessentials.mail.hover_sent", mail.formattedTime())).append("\n")
                .append(MessageUtil.component("commands.neoessentials.mail.hover_id", mail.id)).append("\n")
                .append(MessageUtil.component("commands.neoessentials.mail.hover_from", mail.senderName)).append("\n")
                .append(MessageUtil.component("commands.neoessentials.mail.hover_click_delete"));

            line = line.withStyle(s -> s
                .withHoverEvent(com.zerog.neoessentials.util.HoverEventCompat.create(HoverEvent.Action.SHOW_TEXT, hover))
                .withClickEvent(com.zerog.neoessentials.util.ClickEventCompat.create(ClickEvent.Action.SUGGEST_COMMAND,
                    "/mail delete " + mail.id))
            );
            player.sendSystemMessage(line);
        }

        // Pagination footer
        if (totalPages > 1) {
            MutableComponent footer = Component.literal("§7");
            if (page > 1) {
                footer.append(((MutableComponent) MessageUtil.component("commands.neoessentials.mail.prev_button"))
                    .withStyle(s -> s.withClickEvent(com.zerog.neoessentials.util.ClickEventCompat.create(
                        ClickEvent.Action.RUN_COMMAND, "/mail read " + (page - 1)))));
            }
            footer.append(MessageUtil.component("commands.neoessentials.mail.page_footer", page, totalPages));
            if (page < totalPages) {
                footer.append(((MutableComponent) MessageUtil.component("commands.neoessentials.mail.next_button"))
                    .withStyle(s -> s.withClickEvent(com.zerog.neoessentials.util.ClickEventCompat.create(
                        ClickEvent.Action.RUN_COMMAND, "/mail read " + (page + 1)))));
            }
            player.sendSystemMessage(footer);
        }

        player.sendSystemMessage(MessageUtil.component("commands.neoessentials.mail.clear_hint"));

        if (removed) saveMailData();
        else saveMailData(); // always persist read flags
        return 1;
    }

    /**
     * Core send logic used by both player and console paths.
     * sender == null means console.
     */
    private static int sendMail(CommandSourceStack source, ServerPlayer sender,
                                String targetName, String message, long expireAt) {
        // Mute check (Essentials: user.isMuted() → throw voiceSilenced)
        if (sender != null && MuteManager.isMuted(sender)) {
            source.sendFailure(MessageUtil.error("commands.neoessentials.mail.muted"));
            return 0;
        }

        // Length check (Essentials: 1000 chars)
        if (message.length() > MAX_MESSAGE_LENGTH) {
            source.sendFailure(MessageUtil.error("commands.neoessentials.mail.message_too_long",
                MAX_MESSAGE_LENGTH));
            return 0;
        }

        // Rate limit (Essentials: mailsPerMinute)
        long now = System.currentTimeMillis();
        if (now - rateLimitWindowStart.get() > 60_000L) {
            rateLimitWindowStart.set(now);
            mailsThisMinute.set(0);
        }
        int limit = getMailsPerMinute();
        if (mailsThisMinute.incrementAndGet() > limit) {
            source.sendFailure(MessageUtil.error(
                "commands.neoessentials.mail.rate_limit", limit));
            return 0;
        }

        // Resolve target UUID
        UUID targetUUID = getPlayerUUID(source.getServer(), targetName);
        if (targetUUID == null) {
            source.sendFailure(MessageUtil.error(
                "commands.neoessentials.mail.player_not_found", targetName));
            return 0;
        }

        // Self-mail check
        if (sender != null && targetUUID.equals(sender.getUUID())) {
            source.sendFailure(MessageUtil.error("commands.neoessentials.mail.cannot_mail_self"));
            return 0;
        }

        // Ignore check (Essentials: !u.isIgnoredPlayer(user))
        // Only check when both sender and target are online
        if (sender != null) {
            ServerPlayer onlineTarget = source.getServer().getPlayerList().getPlayer(targetUUID);
            if (onlineTarget != null && IgnoreManager.isIgnoring(onlineTarget, sender)) {
                // Silently drop (Essentials behaviour — sender gets success, mail is not delivered)
                source.sendSuccess(() -> MessageUtil.success(
                    "commands.neoessentials.mail.sent", targetName), false);
                return 1;
            }
        }

        // Build and store message
        String senderName = sender != null ? sender.getName().getString() : "Console";
        String senderUuid = sender != null ? sender.getUUID().toString() : null;
        MailMessage mail  = new MailMessage(senderName, senderUuid, message, expireAt);

        List<MailMessage> box = MAIL_BOX.computeIfAbsent(targetUUID, k -> new ArrayList<>());
        box.add(0, mail); // newest first (Essentials: add(0, message))

        // Mailbox cap
        while (box.size() > MAX_MAILBOX_SIZE) box.remove(box.size() - 1);

        saveMailData();

        source.sendSuccess(() -> MessageUtil.success(
            "commands.neoessentials.mail.sent", targetName), false);

        // Notify target if online
        ServerPlayer onlineTarget = source.getServer().getPlayerList().getPlayer(targetUUID);
        if (onlineTarget != null) {
            onlineTarget.sendSystemMessage(MessageUtil.info(
                "commands.neoessentials.mail.received", senderName));
        }

        return 1;
    }

    /** /mail sendall / sendtempall — broadcast to every player's mailbox. Runs async. */
    private static int sendMailAll(CommandSourceStack source, String senderName,
                                   String senderUuid, String message, long expireAt) {
        if (message.length() > MAX_MESSAGE_LENGTH) {
            source.sendFailure(MessageUtil.error("commands.neoessentials.mail.message_too_long",
                MAX_MESSAGE_LENGTH));
            return 0;
        }

        // Run asynchronously — could touch many files
        Thread t = new Thread(() -> {
            // Online players first
            source.getServer().getPlayerList().getPlayers().forEach(p -> {
                MailMessage mail = new MailMessage(senderName, senderUuid, message, expireAt);
                MAIL_BOX.computeIfAbsent(p.getUUID(), k -> new ArrayList<>()).add(0, mail);
            });
            // Also load and update offline player mailboxes from disk
            saveMailData();
            NeoLog.info(LOGGER, LogCategory.GENERAL, "sendall from {} completed: {} players", senderName,
                source.getServer().getPlayerList().getPlayerCount());
        }, "NeoEssentials-MailSendAll");
        t.setDaemon(true);
        t.start();

        source.sendSuccess(() -> MessageUtil.success("commands.neoessentials.mail.sent_all"), false);
        return 1;
    }

    private static int deleteMail(ServerPlayer player, String id) {
        List<MailMessage> msgs = MAIL_BOX.get(player.getUUID());
        if (msgs == null || msgs.isEmpty()) {
            player.sendSystemMessage(MessageUtil.error("commands.neoessentials.mail.no_mail"));
            return 0;
        }
        boolean removed = msgs.removeIf(m -> m.id.equals(id));
        if (!removed) {
            player.sendSystemMessage(MessageUtil.error(
                "commands.neoessentials.mail.invalid_id", id));
            return 0;
        }
        if (msgs.isEmpty()) MAIL_BOX.remove(player.getUUID());
        saveMailData();
        player.sendSystemMessage(MessageUtil.success("commands.neoessentials.mail.deleted", id));
        return 1;
    }

    /**
     * /mail clear [index]
     * index == -1 → clear all; index >= 1 → remove that specific message (1-based).
     */
    private static int clearMail(CommandSourceStack source, ServerPlayer target, int index) {
        List<MailMessage> msgs = MAIL_BOX.get(target.getUUID());
        if (msgs == null || msgs.isEmpty()) {
            source.sendFailure(MessageUtil.error("commands.neoessentials.mail.no_mail"));
            return 0;
        }

        if (index > 0) {
            // Essentials: remove(toRemove - 1)
            if (index > msgs.size()) {
                source.sendFailure(MessageUtil.error(
                    "commands.neoessentials.mail.invalid_index", msgs.size()));
                return 0;
            }
            msgs.remove(index - 1);
            if (msgs.isEmpty()) MAIL_BOX.remove(target.getUUID());
            saveMailData();
            source.sendSuccess(() -> MessageUtil.success(
                "commands.neoessentials.mail.deleted_index", index), false);
        } else {
            int count = msgs.size();
            MAIL_BOX.remove(target.getUUID());
            saveMailData();
            source.sendSuccess(() -> MessageUtil.success(
                "commands.neoessentials.mail.cleared", count), false);
        }
        return 1;
    }

    /** /mail clearall — wipes every online player's mailbox and persists. */
    private static int clearAll(CommandSourceStack source) {
        int count = MAIL_BOX.size();
        MAIL_BOX.clear();
        saveMailData();
        source.sendSuccess(() -> MessageUtil.success(
            "commands.neoessentials.mail.cleared_all", count), false);
        NeoLog.info(LOGGER, LogCategory.GENERAL, "[Mail] clearall executed by {}", source.getTextName());
        return 1;
    }

    // ────────────────────────────────────────────────────────────────────────
    // Public API (used by login notification and other systems)
    // ────────────────────────────────────────────────────────────────────────

    /** Notify a player of unread mail on login. */
    public static void notifyOnLogin(ServerPlayer player) {
        List<MailMessage> msgs = MAIL_BOX.get(player.getUUID());
        if (msgs == null) return;
        long unread = msgs.stream().filter(m -> !m.read && !m.isExpired()).count();
        if (unread > 0) {
            player.sendSystemMessage(MessageUtil.info(
                "commands.neoessentials.mail.login_notification", unread));
        }
    }

    public static boolean hasUnreadMail(UUID playerId) {
        List<MailMessage> msgs = MAIL_BOX.get(playerId);
        return msgs != null && msgs.stream().anyMatch(m -> !m.read && !m.isExpired());
    }

    public static int getUnreadMailCount(UUID playerId) {
        List<MailMessage> msgs = MAIL_BOX.get(playerId);
        return msgs == null ? 0 : (int) msgs.stream().filter(m -> !m.read && !m.isExpired()).count();
    }

    // ────────────────────────────────────────────────────────────────────────
    // Helpers
    // ────────────────────────────────────────────────────────────────────────

    /** Quick permission check — sends failure message and returns false if denied. */
    private static boolean checkPerm(CommandSourceStack source, String node) {
        PermissionValidator.PermissionResult r =
            PermissionValidator.validatePermission(source, node);
        if (!r.hasPermission()) {
            source.sendFailure(MessageUtil.error(r.getErrorMessage()));
            return false;
        }
        return true;
    }

    private static boolean isPositiveInt(String s) {
        try { return Integer.parseInt(s) > 0; } catch (NumberFormatException e) { return false; }
    }

    /**
     * Parse a human duration string into milliseconds.
     * Supports: 30s, 5m, 2h, 1d, 1w
     * Returns -1 if unparseable.
     */
    public static long parseDuration(String input) {
        if (input == null || input.isBlank()) return -1;
        try {
            String s = input.trim().toLowerCase();
            long multiplier = 1000L;
            if      (s.endsWith("w")) { multiplier = 7 * 24 * 3600 * 1000L; s = s.substring(0, s.length()-1); }
            else if (s.endsWith("d")) { multiplier = 24 * 3600 * 1000L;      s = s.substring(0, s.length()-1); }
            else if (s.endsWith("h")) { multiplier = 3600 * 1000L;            s = s.substring(0, s.length()-1); }
            else if (s.endsWith("m")) { multiplier = 60 * 1000L;              s = s.substring(0, s.length()-1); }
            else if (s.endsWith("s")) { s = s.substring(0, s.length()-1); }
            return Long.parseLong(s) * multiplier;
        } catch (Exception e) { return -1; }
    }

    private static int getMailsPerMinute() {
        try {
            var cfg = ConfigManager.getInstance().getConfig("config.json");
            if (cfg.has("mail") && cfg.getAsJsonObject("mail").has("mailsPerMinute"))
                return cfg.getAsJsonObject("mail").get("mailsPerMinute").getAsInt();
        } catch (Exception ignored) {}
        return DEFAULT_MAILS_PER_MINUTE;
    }

    private static UUID getPlayerUUID(net.minecraft.server.MinecraftServer server, String name) {
        ServerPlayer online = server.getPlayerList().getPlayerByName(name);
        if (online != null) return online.getUUID();
        try {
            net.minecraft.server.players.NameAndId p = server.services().nameToIdCache().get(name).orElse(null);
            if (p != null) return p.id();
        } catch (Exception ignored) {}
        return null;
    }

    // ────────────────────────────────────────────────────────────────────────
    // Persistence
    // ────────────────────────────────────────────────────────────────────────

    public static void loadMailData() {
        try {
            if (!Files.exists(MAIL_DATA_FILE)) {
                Files.createDirectories(MAIL_DATA_FILE.getParent());
                return;
            }
            String json = Files.readString(MAIL_DATA_FILE);
            JsonObject data = JsonParser.parseString(json).getAsJsonObject();

            for (Map.Entry<String, JsonElement> entry : data.entrySet()) {
                try {
                    UUID uuid = UUID.fromString(entry.getKey());
                    JsonArray arr = entry.getValue().getAsJsonArray();
                    List<MailMessage> msgs = new ArrayList<>();
                    for (JsonElement el : arr) {
                        JsonObject o = el.getAsJsonObject();
                        MailMessage m;
                        if (o.has("timeSent")) {
                            // New format
                            m = new MailMessage(
                                o.has("senderName") ? o.get("senderName").getAsString() : "unknown",
                                o.has("senderUuid") && !o.get("senderUuid").isJsonNull()
                                    ? o.get("senderUuid").getAsString() : null,
                                o.get("message").getAsString(),
                                o.has("timeExpire") ? o.get("timeExpire").getAsLong() : 0L
                            );
                            m.id       = o.has("id") ? o.get("id").getAsString() : m.id;
                            m.timeSent = o.get("timeSent").getAsLong();
                            m.read     = o.has("read") && o.get("read").getAsBoolean();
                            m.legacy   = o.has("legacy") && o.get("legacy").getAsBoolean();
                        } else {
                            // Old format (sender + message + timestamp string + read + id)
                            m = new MailMessage(
                                o.has("sender") ? o.get("sender").getAsString() : "unknown",
                                o.get("message").getAsString()
                            );
                            m.id   = o.has("id") ? o.get("id").getAsString() : m.id;
                            m.read = o.has("read") && o.get("read").getAsBoolean();
                        }
                        msgs.add(m);
                    }
                    if (!msgs.isEmpty()) MAIL_BOX.put(uuid, msgs);
                } catch (Exception e) {
                    LOGGER.warn("Skipped invalid mail entry for key '{}': {}", entry.getKey(), e.getMessage());
                }
            }
            NeoLog.debug(LOGGER, LogCategory.GENERAL, "Loaded mail data for {} players", MAIL_BOX.size());
        } catch (Exception e) {
            LOGGER.error("Failed to load mail data: {}", e.getMessage(), e);
        }
    }

    private static synchronized void saveMailData() {
        try {
            JsonObject data = new JsonObject();
            for (Map.Entry<UUID, List<MailMessage>> entry : MAIL_BOX.entrySet()) {
                JsonArray arr = new JsonArray();
                for (MailMessage m : entry.getValue()) {
                    JsonObject o = new JsonObject();
                    o.addProperty("id",         m.id);
                    o.addProperty("senderName", m.senderName);
                    if (m.senderUuid != null) o.addProperty("senderUuid", m.senderUuid);
                    o.addProperty("message",    m.message);
                    o.addProperty("timeSent",   m.timeSent);
                    o.addProperty("timeExpire", m.timeExpire);
                    o.addProperty("read",       m.read);
                    o.addProperty("legacy",     m.legacy);
                    arr.add(o);
                }
                data.add(entry.getKey().toString(), arr);
            }
            Files.createDirectories(MAIL_DATA_FILE.getParent());
            Files.writeString(MAIL_DATA_FILE, GSON.toJson(data));
        } catch (Exception e) {
            LOGGER.error("Failed to save mail data: {}", e.getMessage(), e);
        }
    }
}

