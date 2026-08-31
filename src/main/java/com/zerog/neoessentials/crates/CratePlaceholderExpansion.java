package com.zerog.neoessentials.crates;

import com.zerog.neoessentials.api.PlaceholderExpansion;
import net.minecraft.server.level.ServerPlayer;

import javax.annotation.Nullable;
import java.util.Set;

/** {@code {crate_keys:<crateId>}} — the viewing player's own key balance for that crate. */
public class CratePlaceholderExpansion extends PlaceholderExpansion {
    @Override public String getIdentifier() { return "crate"; }
    @Override public String getVersion() { return "1.0.0"; }
    @Override public String getAuthor() { return "ZeroG Network"; }

    @Override
    public Set<String> getPlaceholders() {
        return Set.copyOf(CrateManager.getInstance().getAllCrates().keySet());
    }

    @Nullable
    @Override
    public String onPlaceholderRequest(@Nullable ServerPlayer player, String identifier, @Nullable String params) {
        if (player == null || !"keys".equals(identifier)) return null;
        if (params == null || CrateManager.getInstance().getCrate(params) == null) return null;
        return String.valueOf(CrateKeyManager.getInstance().getKeys(player.getUUID(), params));
    }
}
