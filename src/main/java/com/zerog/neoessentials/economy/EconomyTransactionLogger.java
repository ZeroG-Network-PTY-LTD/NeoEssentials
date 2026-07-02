package com.zerog.neoessentials.economy;
import com.zerog.neoessentials.util.DebugUtil;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class EconomyTransactionLogger {
    private static final String LOG_FILE = "logs/neoessentials/transactions.log";
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    // Single daemon thread so log writes never block the server thread
    private static final ExecutorService LOG_EXECUTOR = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "EconomyLogger");
        t.setDaemon(true);
        return t;
    });

    public static void log(String type, String sender, String receiver, String amount, String reason) {
        if (!com.zerog.neoessentials.config.ConfigManager.isLogTransactionsEnabled()) return;
        String timestamp = LocalDateTime.now().format(FORMATTER);
        String entry = String.format("[%s] %s | %s -> %s | %s | %s\n", timestamp, type, sender, receiver, amount, reason);
        LOG_EXECUTOR.submit(() -> {
            try {
                File logFile = new File(LOG_FILE);
                File parent = logFile.getParentFile();
                if (parent != null && !parent.exists()) parent.mkdirs();
                try (FileWriter writer = new FileWriter(logFile, true)) {
                    writer.write(entry);
                }
            } catch (IOException e) {
                DebugUtil.debugStackTrace(e);
            }
        });
    }
}