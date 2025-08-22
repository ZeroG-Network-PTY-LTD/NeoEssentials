package com.zerog.neoessentials.managers;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class IgnoreManager {
    private static IgnoreManager instance;
    private final Set<String> ignoredPairs = new HashSet<>();

    public static IgnoreManager getInstance() {
        if (instance == null) instance = new IgnoreManager();
        return instance;
    }

    // Example: store ignored pairs as "senderUUID:recipientUUID"
    public boolean isIgnored(UUID sender, UUID recipient) {
        return ignoredPairs.contains(sender.toString() + ":" + recipient.toString());
    }

    public void ignore(UUID sender, UUID recipient) {
        ignoredPairs.add(sender.toString() + ":" + recipient.toString());
    }

    public void unignore(UUID sender, UUID recipient) {
        ignoredPairs.remove(sender.toString() + ":" + recipient.toString());
    }
}