package com.zerog.neoessentials.economy;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class EconomyTransactionLogger {
    private static final String LOG_FILE = "logs/neoessentials/transactions.log";
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public static void log(String type, String sender, String receiver, String amount, String reason) {
        String timestamp = LocalDateTime.now().format(FORMATTER);
        String entry = String.format("[%s] %s | %s -> %s | %s | %s\n", timestamp, type, sender, receiver, amount, reason);
        try {
            File logFile = new File(LOG_FILE);
            File parent = logFile.getParentFile();
            if (parent != null && !parent.exists()) parent.mkdirs();
            try (FileWriter writer = new FileWriter(logFile, true)) {
                writer.write(entry);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}