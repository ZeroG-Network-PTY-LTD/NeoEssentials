package com.zerog.neoessentials.teleportation.DirectTeleport;

import com.google.gson.JsonObject;
import com.zerog.neoessentials.config.ConfigManager;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.List;
import java.util.Map;

/**
 * GUI icon for a biome id, used by the RTP biome-select GUI. No per-item icon concept exists
 * elsewhere in this codebase, so this is a small cosmetic default table (common vanilla biomes
 * only — not exhaustive) plus an admin-configurable override
 * ({@code teleportation.randomTeleportSettings.biomeIcons}). Wooded biomes use that tree's own
 * sapling/propagule/fungus (the "growable" item, not the log) since that's the more immediately
 * recognizable icon; biomes with no tree of their own use a block unique to that biome instead.
 * Anything neither maps (every modded biome, by default) falls back to a generic icon.
 */
public final class BiomeIconRegistry {
    private BiomeIconRegistry() {}

    private static final Item FALLBACK = Items.GRASS_BLOCK;

    private static final Map<String, Item> DEFAULT_ICONS = Map.ofEntries(
        Map.entry("minecraft:plains", Items.GRASS_BLOCK),
        Map.entry("minecraft:sunflower_plains", Items.SUNFLOWER),
        Map.entry("minecraft:forest", Items.OAK_SAPLING),
        Map.entry("minecraft:flower_forest", Items.POPPY),
        Map.entry("minecraft:birch_forest", Items.BIRCH_SAPLING),
        Map.entry("minecraft:old_growth_birch_forest", Items.BIRCH_SAPLING),
        Map.entry("minecraft:dark_forest", Items.DARK_OAK_SAPLING),
        Map.entry("minecraft:taiga", Items.SPRUCE_SAPLING),
        Map.entry("minecraft:old_growth_spruce_taiga", Items.SPRUCE_SAPLING),
        Map.entry("minecraft:old_growth_pine_taiga", Items.SPRUCE_SAPLING),
        Map.entry("minecraft:snowy_taiga", Items.SPRUCE_SAPLING),
        Map.entry("minecraft:snowy_plains", Items.SNOW_BLOCK),
        Map.entry("minecraft:ice_spikes", Items.PACKED_ICE),
        Map.entry("minecraft:desert", Items.SAND),
        Map.entry("minecraft:savanna", Items.ACACIA_SAPLING),
        Map.entry("minecraft:savanna_plateau", Items.ACACIA_SAPLING),
        Map.entry("minecraft:windswept_savanna", Items.ACACIA_SAPLING),
        Map.entry("minecraft:jungle", Items.JUNGLE_SAPLING),
        Map.entry("minecraft:sparse_jungle", Items.JUNGLE_SAPLING),
        Map.entry("minecraft:bamboo_jungle", Items.BAMBOO),
        Map.entry("minecraft:swamp", Items.LILY_PAD),
        Map.entry("minecraft:mangrove_swamp", Items.MANGROVE_PROPAGULE),
        Map.entry("minecraft:badlands", Items.RED_SAND),
        Map.entry("minecraft:eroded_badlands", Items.RED_SAND),
        Map.entry("minecraft:wooded_badlands", Items.RED_SAND),
        Map.entry("minecraft:ocean", Items.WATER_BUCKET),
        Map.entry("minecraft:deep_ocean", Items.PRISMARINE),
        Map.entry("minecraft:warm_ocean", Items.TROPICAL_FISH),
        Map.entry("minecraft:lukewarm_ocean", Items.TROPICAL_FISH),
        Map.entry("minecraft:cold_ocean", Items.COD),
        Map.entry("minecraft:frozen_ocean", Items.ICE),
        Map.entry("minecraft:river", Items.WATER_BUCKET),
        Map.entry("minecraft:frozen_river", Items.ICE),
        Map.entry("minecraft:beach", Items.SAND),
        Map.entry("minecraft:snowy_beach", Items.SNOW),
        Map.entry("minecraft:stony_shore", Items.STONE),
        Map.entry("minecraft:windswept_hills", Items.STONE),
        Map.entry("minecraft:windswept_gravelly_hills", Items.GRAVEL),
        Map.entry("minecraft:windswept_forest", Items.SPRUCE_SAPLING),
        Map.entry("minecraft:meadow", Items.PINK_TULIP),
        Map.entry("minecraft:grove", Items.POWDER_SNOW_BUCKET),
        Map.entry("minecraft:snowy_slopes", Items.SNOW_BLOCK),
        Map.entry("minecraft:frozen_peaks", Items.PACKED_ICE),
        Map.entry("minecraft:jagged_peaks", Items.STONE),
        Map.entry("minecraft:stony_peaks", Items.STONE),
        Map.entry("minecraft:cherry_grove", Items.CHERRY_SAPLING),
        Map.entry("minecraft:mushroom_fields", Items.RED_MUSHROOM),
        Map.entry("minecraft:dripstone_caves", Items.POINTED_DRIPSTONE),
        Map.entry("minecraft:lush_caves", Items.MOSS_BLOCK),
        Map.entry("minecraft:deep_dark", Items.SCULK),
        Map.entry("minecraft:nether_wastes", Items.NETHERRACK),
        Map.entry("minecraft:soul_sand_valley", Items.SOUL_SAND),
        Map.entry("minecraft:crimson_forest", Items.CRIMSON_FUNGUS),
        Map.entry("minecraft:warped_forest", Items.WARPED_FUNGUS),
        Map.entry("minecraft:basalt_deltas", Items.BASALT),
        Map.entry("minecraft:the_end", Items.END_STONE),
        Map.entry("minecraft:end_highlands", Items.END_STONE),
        Map.entry("minecraft:end_midlands", Items.END_STONE),
        Map.entry("minecraft:small_end_islands", Items.END_STONE),
        Map.entry("minecraft:end_barrens", Items.END_STONE)
    );

    /**
     * One {@code randomTeleportSettings.biomeMenuItems} entry.
     * @param slot    fixed GUI slot (0 = first box, top-left) to pin this biome to on page 1 of
     *                the RTP GUI — {@code null} if this entry is icon-only (auto-positioned
     *                like any other biome, just with a custom icon).
     * @param biomeId full biome resource location, e.g. {@code "minecraft:taiga"}
     * @param item    icon item id override — {@code null} keeps the built-in default (sapling/
     *                dedicated block/fallback) for this biome.
     */
    public record MenuItemConfig(Integer slot, String biomeId, String item) {}

    public static ItemStack iconFor(String biomeId) {
        String override = itemOverrideFor(biomeId);
        if (override != null) {
            try {
                return new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.parse(override)));
            } catch (Exception ignored) {
                // fall through to the default table below
            }
        }
        return new ItemStack(DEFAULT_ICONS.getOrDefault(biomeId, FALLBACK));
    }

    private static String itemOverrideFor(String biomeId) {
        for (MenuItemConfig entry : getConfiguredMenuItems()) {
            if (entry.item() != null && biomeId.equals(entry.biomeId())) return entry.item();
        }
        return null;
    }

    /** Parses {@code teleportation.randomTeleportSettings.biomeMenuItems} — an array of
     *  {@code { "slot": N, "biome": "...", "item": "..." }} objects. {@code slot} and
     *  {@code item} are both optional per entry. Malformed entries (missing {@code biome}) are
     *  skipped, logged at debug — one bad entry can't break the GUI. */
    public static List<MenuItemConfig> getConfiguredMenuItems() {
        List<MenuItemConfig> result = new java.util.ArrayList<>();
        try {
            JsonObject config = ConfigManager.getInstance().getConfig(ConfigManager.MAIN_CONFIG);
            if (!config.has("teleportation")) return result;
            JsonObject tp = config.getAsJsonObject("teleportation");
            if (!tp.has("randomTeleportSettings")) return result;
            JsonObject rtp = tp.getAsJsonObject("randomTeleportSettings");
            if (!rtp.has("biomeMenuItems") || !rtp.get("biomeMenuItems").isJsonArray()) return result;

            for (com.google.gson.JsonElement el : rtp.getAsJsonArray("biomeMenuItems")) {
                if (!el.isJsonObject()) continue;
                JsonObject obj = el.getAsJsonObject();
                if (!obj.has("biome") || obj.get("biome").isJsonNull()) continue;
                String biomeId = obj.get("biome").getAsString();
                Integer slot = obj.has("slot") && !obj.get("slot").isJsonNull() ? obj.get("slot").getAsInt() : null;
                String item = obj.has("item") && !obj.get("item").isJsonNull() ? obj.get("item").getAsString() : null;
                result.add(new MenuItemConfig(slot, biomeId, item));
            }
        } catch (Exception ignored) {
            // config missing/malformed — no menu items configured, GUI falls back to fully auto-listed
        }
        return result;
    }
}
