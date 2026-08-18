package com.zerog.neoessentials.util;

import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Central queue for one-time, admin-facing startup notices — config splitting available,
 * legacy data files no longer being read, a manager failing to initialize, etc.
 *
 * <p>Before this existed, each subsystem that wanted to tell an admin something on their first
 * join tracked its own "have I shown this yet" flag and sent its own independently-formatted,
 * independently-timed chat block (see {@code ConfigSplitter}'s old {@code shouldNotifyAdmins}
 * and {@code SupportLinks}' old {@code shouldAlertJoiningAdmin} — both now route through here
 * instead). This collects every notice raised during boot and delivers them all together, in one
 * consistent format, to the first admin who joins.
 */
public final class AdminNotices {
    private AdminNotices() {}

    private record Notice(Component title, List<Component> body) {}

    private static final ConcurrentLinkedQueue<Notice> PENDING = new ConcurrentLinkedQueue<>();
    private static final AtomicBoolean CONSUMED = new AtomicBoolean(false);

    /** Queue a notice built from localization keys — title first, then one line per body key. */
    public static void queue(String titleKey, String... bodyKeys) {
        List<Component> body = new ArrayList<>();
        for (String key : bodyKeys) body.add(MessageUtil.component(key));
        PENDING.add(new Notice(MessageUtil.component(titleKey), body));
    }

    /** Queue a notice built from already-constructed components (e.g. clickable links). */
    public static void queue(Component title, Component... body) {
        PENDING.add(new Notice(title, List.of(body)));
    }

    public static boolean hasPending() {
        return !PENDING.isEmpty();
    }

    /**
     * True at most once per server session, and only when there's something to show. Callers
     * MUST have already verified the joining player is an admin before calling this — it
     * consumes the one-shot "show it now" opportunity regardless of who's asking, so checking
     * permission afterward would silently burn it on a non-admin.
     */
    public static boolean consumeIfPending() {
        return hasPending() && CONSUMED.compareAndSet(false, true);
    }

    /**
     * Sends every queued notice to {@code player} after a short delay — long enough that the
     * message doesn't get lost while the client is still finishing its own join sequence — then
     * clears the queue. The delay runs on a background thread; sleeping on the server thread
     * would freeze the whole server for the duration.
     */
    public static void scheduleSendTo(ServerPlayer player) {
        MinecraftServer server = player.level().getServer();
        if (server == null) return;
        Thread notifyThread = new Thread(() -> {
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
            server.execute(() -> sendAllTo(player));
        }, "NeoEssentials-AdminNotify");
        notifyThread.setDaemon(true);
        notifyThread.start();
    }

    private static void sendAllTo(ServerPlayer player) {
        Notice notice;
        boolean first = true;
        while ((notice = PENDING.poll()) != null) {
            if (!first) player.sendSystemMessage(Component.literal(""));
            first = false;
            player.sendSystemMessage(MessageUtil.component("commands.neoessentials.admin_notice.border"));
            player.sendSystemMessage(notice.title());
            player.sendSystemMessage(MessageUtil.component("commands.neoessentials.admin_notice.border"));
            player.sendSystemMessage(Component.literal(""));
            for (Component line : notice.body()) {
                player.sendSystemMessage(line);
            }
        }
    }
}
