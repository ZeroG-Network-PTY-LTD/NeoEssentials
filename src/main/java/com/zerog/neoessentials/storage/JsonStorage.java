package com.zerog.neoessentials.storage;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class JsonStorage {
    private static final Gson gson = new Gson();
    private static final Path MAIL_PATH = Path.of("config/mail_data.json");
    private static final Path SOCIALSPY_PATH = Path.of("config/socialspy_data.json");

    public static Map<UUID, List<MailEntry>> loadMail() {
        if (!Files.exists(MAIL_PATH)) return new ConcurrentHashMap<>();
        try {
            String json = Files.readString(MAIL_PATH);
            Type type = new TypeToken<Map<String, List<MailEntry>>>(){}.getType();
            Map<String, List<MailEntry>> raw = gson.fromJson(json, type);
            Map<UUID, List<MailEntry>> result = new ConcurrentHashMap<>();
            if (raw != null) {
                for (var entry : raw.entrySet()) {
                    result.put(UUID.fromString(entry.getKey()), entry.getValue());
                }
            }
            return result;
        } catch (IOException e) {
            return new ConcurrentHashMap<>();
        }
    }

    public static void saveMail(Map<UUID, List<MailEntry>> mailData) {
        Map<String, List<MailEntry>> raw = mailData.entrySet().stream()
            .collect(Collectors.toMap(e -> e.getKey().toString(), Map.Entry::getValue));
        try {
            Files.createDirectories(MAIL_PATH.getParent());
            Files.writeString(MAIL_PATH, gson.toJson(raw));
        } catch (IOException ignored) {}
    }

    public static Map<UUID, Boolean> loadSocialSpy() {
        if (!Files.exists(SOCIALSPY_PATH)) return new ConcurrentHashMap<>();
        try {
            String json = Files.readString(SOCIALSPY_PATH);
            Type type = new TypeToken<Map<String, Boolean>>(){}.getType();
            Map<String, Boolean> raw = gson.fromJson(json, type);
            Map<UUID, Boolean> result = new ConcurrentHashMap<>();
            if (raw != null) {
                for (var entry : raw.entrySet()) {
                    result.put(UUID.fromString(entry.getKey()), entry.getValue());
                }
            }
            return result;
        } catch (IOException e) {
            return new ConcurrentHashMap<>();
        }
    }

    public static void saveSocialSpy(Map<UUID, Boolean> spyData) {
        Map<String, Boolean> raw = spyData.entrySet().stream()
            .collect(Collectors.toMap(e -> e.getKey().toString(), Map.Entry::getValue));
        try {
            Files.createDirectories(SOCIALSPY_PATH.getParent());
            Files.writeString(SOCIALSPY_PATH, gson.toJson(raw));
        } catch (IOException ignored) {}
    }

    public static class MailEntry {
        private String from;
        private String message;
        private long timestamp;
        public MailEntry(String from, String message, long timestamp) {
            this.from = from;
            this.message = message;
            this.timestamp = timestamp;
        }
        public String from() { return from; }
        public String message() { return message; }
        public long timestamp() { return timestamp; }
    }
}
