package com.zerog.neoessentials.crates;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.zerog.neoessentials.config.ConfigManager;
import com.zerog.neoessentials.logging.LogCategory;
import com.zerog.neoessentials.logging.NeoLog;
import com.zerog.neoessentials.shop.ShopTransaction;
import com.zerog.neoessentials.storage.DataStore;
import com.zerog.neoessentials.storage.StorageManager;
import com.zerog.neoessentials.util.MessageUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/** Registry of crate definitions (from {@code crates.json}) and physical block placements,
 *  plus the actual open/reward-grant orchestration. */
public class CrateManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(CrateManager.class);
    private static final String BLOCKS_COLLECTION = "crate_blocks";
    private static final String HISTORY_COLLECTION = "crate_history";
    private static final String KEY_NBT_TAG = "neoessentials_crate_key";

    private static class Holder {
        static final CrateManager INSTANCE = new CrateManager();
    }
    public static CrateManager getInstance() { return Holder.INSTANCE; }

    private final Map<String, CrateDefinition> crates = new LinkedHashMap<>();

    private CrateManager() {}

    public void load() {
        crates.clear();
        try {
            JsonObject root = ConfigManager.getInstance().getConfig(ConfigManager.CRATES_CONFIG);
            if (!root.has("crates")) {
                NeoLog.info(LOGGER, LogCategory.CRATES, "No crates configured.");
                return;
            }
            JsonObject cratesJson = root.getAsJsonObject("crates");
            for (String id : cratesJson.keySet()) {
                try {
                    crates.put(id.toLowerCase(), CrateDefinition.fromJson(id, cratesJson.getAsJsonObject(id)));
                } catch (Exception e) {
                    NeoLog.error(LOGGER, LogCategory.CRATES, "Failed to load crate '" + id + "' — skipping", e);
                }
            }
            NeoLog.info(LOGGER, LogCategory.CRATES, "CrateManager: loaded {} crate(s)", crates.size());
        } catch (Exception e) {
            NeoLog.error(LOGGER, LogCategory.CRATES, "Failed to load crates.json", e);
        }
    }

    public CrateDefinition getCrate(String id) {
        return id == null ? null : crates.get(id.toLowerCase());
    }

    public Map<String, CrateDefinition> getAllCrates() {
        return crates;
    }

    /** Whether the crate has at least one reward with a positive weight — i.e. whether opening
     *  it could actually resolve to something. Callers should check this before spending a key
     *  so an empty crate gets a distinct "no rewards configured" message instead of eating a
     *  key and reporting the misleading generic "no keys" error. */
    public boolean hasAnyReward(CrateDefinition crate) {
        return crate.rewards.stream().anyMatch(r -> r.weight > 0);
    }

    // ── Physical block placement ────────────────────────────────────────────

    private static String posKey(ServerLevel level, BlockPos pos) {
        return level.dimension().identifier() + "@" + pos.getX() + "," + pos.getY() + "," + pos.getZ();
    }

    public void setBlock(ServerLevel level, BlockPos pos, String crateId) {
        JsonObject record = new JsonObject();
        record.addProperty("crateId", crateId.toLowerCase());
        StorageManager.getInstance().getStore().put(BLOCKS_COLLECTION, posKey(level, pos), record);
    }

    public boolean removeBlock(ServerLevel level, BlockPos pos) {
        return StorageManager.getInstance().getStore().delete(BLOCKS_COLLECTION, posKey(level, pos));
    }

    /** The crate a physical block position represents, or {@code null} if it isn't one. */
    public CrateDefinition getCrateAt(ServerLevel level, BlockPos pos) {
        JsonObject record = StorageManager.getInstance().getStore().get(BLOCKS_COLLECTION, posKey(level, pos));
        if (record == null || !record.has("crateId")) return null;
        return getCrate(record.get("crateId").getAsString());
    }

    // ── Key items ────────────────────────────────────────────────────────────

    /** Builds a real, giveable key ItemStack for a crate — tagged so it can only ever be
     *  redeemed for keys of that specific crate. Unlike the virtual balance, this item is a
     *  self-contained key: holding one is by itself enough to open the crate (see
     *  {@link #tryConsumeKeyAndPick}), so it can be freely given, dropped, or traded between
     *  players like any other item. */
    public ItemStack buildKeyItem(CrateDefinition crate, int count) {
        ItemStack stack = crate.keyItem.copyWithCount(count);
        var tag = new net.minecraft.nbt.CompoundTag();
        tag.putString(KEY_NBT_TAG, crate.id);
        stack.set(DataComponents.CUSTOM_NAME,
            com.zerog.neoessentials.util.ChatComponentUtil.parseColorCodes(
                crate.displayName + " Key", net.minecraft.network.chat.Style.EMPTY.withColor(net.minecraft.ChatFormatting.GOLD)));
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        return stack;
    }

    /** Gives {@code count} physical key items for {@code crate} to the player, split across
     *  stacks that respect the key item's own max stack size, dropping at their feet if their
     *  inventory can't hold it all. This is the only place that mints physical key items. */
    public void giveKeyItems(ServerPlayer player, CrateDefinition crate, int count) {
        int max = Math.max(1, crate.keyItem.getMaxStackSize());
        int remaining = count;
        while (remaining > 0) {
            int chunk = Math.min(max, remaining);
            ItemStack stack = buildKeyItem(crate, chunk);
            if (!player.getInventory().add(stack)) player.drop(stack, false);
            remaining -= chunk;
        }
    }

    /** The crate id a held key item represents, or {@code null} if it isn't a crate key. */
    public String getKeyCrateId(ItemStack stack) {
        if (stack.isEmpty() || !stack.has(DataComponents.CUSTOM_DATA)) return null;
        var tag = stack.get(DataComponents.CUSTOM_DATA).copyTag();
        return tag.contains(KEY_NBT_TAG) ? com.zerog.neoessentials.util.CompoundTagCompat.getString(tag, KEY_NBT_TAG) : null;
    }

    // ── Opening ──────────────────────────────────────────────────────────────

    /** Consumes one key (a valid physical item for this crate if given, else one from the
     *  virtual balance) and returns the resolved reward, or {@code null} if the player has no
     *  key by either means or the crate has no positively-weighted rewards (callers should check
     *  {@link #hasAnyReward} first to tell those two cases apart for the player). The reward is
     *  resolved <b>before</b> any key is consumed, so an empty/all-zero-weight reward pool never
     *  eats a key for nothing.
     *
     * <p>A valid physical key is sufficient on its own — it does not also require virtual
     * balance — so a key item given, dropped, or traded between players works standalone for
     * whoever ends up holding it. Consuming the item itself (shrinking the stack) is what
     * prevents reuse; the virtual balance is a separate, non-transferable source of keys, only
     * checked when no valid physical key is in hand. */
    public CrateReward tryConsumeKeyAndPick(ServerPlayer player, CrateDefinition crate, ItemStack physicalKey) {
        CrateReward picked = WeightedRandomPicker.pick(crate.rewards, r -> r.weight);
        if (picked == null) return null;

        boolean validPhysicalKey = physicalKey != null && !physicalKey.isEmpty()
            && crate.id.equalsIgnoreCase(getKeyCrateId(physicalKey));

        if (validPhysicalKey) {
            physicalKey.shrink(1);
        } else if (!CrateKeyManager.getInstance().removeKeys(player.getUUID(), crate.id, 1)) {
            return null;
        }

        return picked;
    }

    public void grantReward(ServerPlayer player, CrateDefinition crate, CrateReward reward) {
        MinecraftServer server = player.level().getServer();
        if (server == null) return;

        if (!reward.item.isEmpty()) {
            ShopTransaction.giveItems(player, reward.item);
        }
        for (String cmd : reward.commands) {
            String finalCmd = (cmd.startsWith("/") ? cmd.substring(1) : cmd).replace("{player}", player.getName().getString());
            server.execute(() -> server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), finalCmd));
        }
        if (reward.broadcastRare) {
            String template = reward.broadcastMessage != null && !reward.broadcastMessage.isEmpty()
                ? reward.broadcastMessage
                : "&6{player} &7won a rare reward from &6" + crate.displayName + "&7!";
            String message = template.replace("{player}", player.getName().getString());
            var component = com.zerog.neoessentials.chat.RichTextFormatter.processTablistText(message);
            for (ServerPlayer p : server.getPlayerList().getPlayers()) p.sendSystemMessage(component);
        }

        logHistory(player.getUUID(), crate.id, reward.id);
        NeoLog.info(LOGGER, LogCategory.CRATES, "{} opened crate '{}' and won reward '{}'", player.getName().getString(), crate.id, reward.id);
    }

    private void logHistory(UUID playerUuid, String crateId, String rewardId) {
        DataStore store = StorageManager.getInstance().getStore();
        String key = playerUuid + ":" + System.currentTimeMillis();
        JsonObject record = new JsonObject();
        record.addProperty("player", playerUuid.toString());
        record.addProperty("crate", crateId);
        record.addProperty("reward", rewardId);
        record.addProperty("timestamp", System.currentTimeMillis());
        store.put(HISTORY_COLLECTION, key, record);
    }

    // ── Persistence (admin commands) ────────────────────────────────────────

    public void saveCrate(CrateDefinition def) {
        crates.put(def.id.toLowerCase(), def);
        JsonObject root = ConfigManager.getInstance().getConfig(ConfigManager.CRATES_CONFIG);
        JsonObject cratesJson = root.has("crates") ? root.getAsJsonObject("crates") : new JsonObject();
        cratesJson.add(def.id, def.toJson());
        root.add("crates", cratesJson);
        ConfigManager.getInstance().saveConfig(ConfigManager.CRATES_CONFIG, root);
    }

    public boolean deleteCrate(String id) {
        if (getCrate(id) == null) return false;
        crates.remove(id.toLowerCase());
        JsonObject root = ConfigManager.getInstance().getConfig(ConfigManager.CRATES_CONFIG);
        if (root.has("crates")) {
            JsonObject cratesJson = root.getAsJsonObject("crates");
            JsonObject rebuilt = new JsonObject();
            for (String key : cratesJson.keySet()) {
                if (!key.equalsIgnoreCase(id)) rebuilt.add(key, cratesJson.get(key));
            }
            root.add("crates", rebuilt);
            ConfigManager.getInstance().saveConfig(ConfigManager.CRATES_CONFIG, root);
        }
        return true;
    }
}
