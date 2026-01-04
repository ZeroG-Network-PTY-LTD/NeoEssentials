package com.zerog.neoessentials.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.util.Arrays;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Utility class to help diagnose thread-related shutdown issues
 */
public class ThreadDiagnostics {
    private static final Logger LOGGER = LoggerFactory.getLogger(ThreadDiagnostics.class);

    /**
     * Log all currently running threads
     * Useful for identifying threads that are preventing shutdown
     */
    @SuppressWarnings("unused") // Public API method for manual diagnostics
    public static void logAllThreads() {
        ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();
        ThreadInfo[] threadInfos = threadMXBean.dumpAllThreads(false, false);

        LOGGER.info("════════════════════════════════════════════════════════════════");
        LOGGER.info("Thread Diagnostics - Total Threads: {}", threadInfos.length);
        LOGGER.info("════════════════════════════════════════════════════════════════");

        for (ThreadInfo threadInfo : threadInfos) {
            if (threadInfo != null) {
                Thread.State state = threadInfo.getThreadState();
                String name = threadInfo.getThreadName();
                boolean isDaemon = threadInfo.isDaemon();

                // Highlight non-daemon threads as they prevent JVM shutdown
                String prefix = isDaemon ? "  [DAEMON]" : "  [USER]  ";
                LOGGER.info("{} {} - State: {}", prefix, name, state);
            }
        }

        LOGGER.info("════════════════════════════════════════════════════════════════");
    }

    /**
     * Log only NeoEssentials-related threads
     */
    public static void logNeoEssentialsThreads() {
        ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();
        ThreadInfo[] threadInfos = threadMXBean.dumpAllThreads(false, false);

        Set<ThreadInfo> neoThreads = Arrays.stream(threadInfos)
            .filter(Objects::nonNull)
            .filter(info -> isNeoEssentialsThread(info.getThreadName()))
            .collect(Collectors.toSet());

        if (neoThreads.isEmpty()) {
            LOGGER.info("No NeoEssentials threads detected");
            return;
        }

        LOGGER.info("════════════════════════════════════════════════════════════════");
        LOGGER.info("NeoEssentials Threads - Count: {}", neoThreads.size());
        LOGGER.info("════════════════════════════════════════════════════════════════");

        for (ThreadInfo threadInfo : neoThreads) {
            Thread.State state = threadInfo.getThreadState();
            String name = threadInfo.getThreadName();
            boolean isDaemon = threadInfo.isDaemon();

            String prefix = isDaemon ? "  [DAEMON]" : "  [USER]  ";
            LOGGER.warn("{} {} - State: {} (NEEDS SHUTDOWN!)", prefix, name, state);
        }

        LOGGER.info("════════════════════════════════════════════════════════════════");
    }

    /**
     * Check if a thread name suggests it belongs to NeoEssentials
     */
    private static boolean isNeoEssentialsThread(String threadName) {
        String lowerName = threadName.toLowerCase();
        return lowerName.contains("neoessentials")
            || lowerName.contains("afk")
            || lowerName.contains("economy")
            || lowerName.contains("dashboard")
            || lowerName.contains("transaction")
            || lowerName.contains("paytoggle")
            || lowerName.contains("ban")
            || lowerName.contains("teleport");
    }

    /**
     * Log threads that are NOT daemon threads (these prevent JVM shutdown)
     */
    public static void logNonDaemonThreads() {
        ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();
        ThreadInfo[] threadInfos = threadMXBean.dumpAllThreads(false, false);

        Set<ThreadInfo> nonDaemonThreads = Arrays.stream(threadInfos)
            .filter(Objects::nonNull)
            .filter(info -> !info.isDaemon())
            .filter(info -> !isSystemThread(info.getThreadName()))
            .collect(Collectors.toSet());

        if (nonDaemonThreads.isEmpty()) {
            LOGGER.info("No non-daemon user threads detected (good for shutdown)");
            return;
        }

        LOGGER.warn("════════════════════════════════════════════════════════════════");
        LOGGER.warn("NON-DAEMON THREADS - Count: {} (BLOCKING SHUTDOWN!)", nonDaemonThreads.size());
        LOGGER.warn("════════════════════════════════════════════════════════════════");

        for (ThreadInfo threadInfo : nonDaemonThreads) {
            Thread.State state = threadInfo.getThreadState();
            String name = threadInfo.getThreadName();

            LOGGER.warn("  [BLOCKING] {} - State: {}", name, state);
        }

        LOGGER.warn("════════════════════════════════════════════════════════════════");
    }

    /**
     * Check if a thread is a system thread (JVM internals)
     */
    private static boolean isSystemThread(String threadName) {
        String lowerName = threadName.toLowerCase();
        return lowerName.contains("reference handler")
            || lowerName.contains("finalizer")
            || lowerName.contains("signal dispatcher")
            || lowerName.contains("attach listener")
            || lowerName.contains("common-cleaner")
            || lowerName.contains("main");
    }
}

