package com.zerog.neoessentials.util;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Shared, bounded scheduled executor for short, one-off background tasks that need to run a
 * little while after a per-player event (e.g. a resource-pack send or a permission sync a
 * second after join) without blocking the caller.
 * <p>
 * Exists because several {@code onPlayerJoin} handlers used to each spawn a brand-new raw
 * {@link Thread} per player join (some with a {@code Thread.sleep()} inside to implement the
 * delay, tying the thread up for the whole wait). On a server with frequent joins/reconnects
 * — many players, an unstable connection, or a crash-looping client — that's unbounded thread
 * creation with no cap, which can exhaust the OS's native thread limit and crash the server
 * with {@code OutOfMemoryError: unable to create native thread}. A small fixed pool bounds the
 * worst case to a handful of threads no matter how many joins happen at once; extra work just
 * queues briefly instead of spawning more threads.
 */
public final class DelayedTaskExecutor {
    private static final ScheduledExecutorService EXECUTOR = Executors.newScheduledThreadPool(2, r -> {
        Thread t = new Thread(r, "NeoEssentials-DelayedTask");
        t.setDaemon(true);
        return t;
    });

    private DelayedTaskExecutor() {
    }

    /** Runs {@code task} after {@code delayMs} milliseconds on the shared pool. */
    public static void schedule(Runnable task, long delayMs) {
        EXECUTOR.schedule(task, delayMs, TimeUnit.MILLISECONDS);
    }
}
