package com.zerog.neoessentials.votifier;

import com.google.gson.JsonObject;
import com.zerog.neoessentials.api.PlaceholderExpansion;
import com.zerog.neoessentials.storage.StorageManager;
import net.minecraft.server.level.ServerPlayer;

import javax.annotation.Nullable;
import java.util.Set;

/** {@code {votifier_total}} / {@code {votifier_voteparty_progress}} / {@code {votifier_voteparty_required}}. */
public class VotifierPlaceholderExpansion extends PlaceholderExpansion {
    @Override public String getIdentifier() { return "votifier"; }
    @Override public String getVersion() { return "1.0.0"; }
    @Override public String getAuthor() { return "ZeroG Network"; }

    @Override
    public Set<String> getPlaceholders() {
        return Set.of("total", "voteparty_progress", "voteparty_required");
    }

    @Nullable
    @Override
    public String onPlaceholderRequest(@Nullable ServerPlayer player, String identifier, @Nullable String params) {
        return switch (identifier) {
            case "total" -> {
                if (player == null) yield "0";
                JsonObject record = StorageManager.getInstance().getStore()
                    .get("votifier_stats", player.getName().getString().toLowerCase());
                yield String.valueOf(record != null && record.has("total") ? record.get("total").getAsLong() : 0L);
            }
            case "voteparty_progress" -> String.valueOf(VotePartyManager.getInstance().getProgress());
            case "voteparty_required" -> String.valueOf(VotePartyManager.getInstance().getRequired());
            default -> null;
        };
    }
}
