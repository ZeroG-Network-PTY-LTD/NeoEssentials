package com.zerog.neoessentials.auctionhouse;

import com.google.gson.JsonElement;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

/**
 * Core singleton for the NeoEssentials Auction House feature.
 *
 * <p>Manages the in-memory listing caches and handles the per-second tick
 * that decrements listing timers and moves expired items to the expired table.
 *
 * <p>Lifecycle:
 * <ol>
 *   <li>{@code initialize(server)} — called from {@code ServerStartingEvent}</li>
 *   <li>{@code setServer(server)} — called from {@code ServerStartedEvent} (for registry ops)</li>
 *   <li>{@code shutdown()} — called from {@code ServerStoppingEvent}</li>
 * </ol>
 */
public final class AuctionHouseManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(AuctionHouseManager.class);

    private static AuctionHouseManager instance;

    /** All active listings across all players. Thread-safe for GUI reads. */
    private final List<AuctionItem> activeListings   = new CopyOnWriteArrayList<>();
    /** All uncollected expired items across all players. */
    private final List<AuctionItem> expiredItems     = new CopyOnWriteArrayList<>();

    private MinecraftServer server;

    /** Tick counter — triggers a one-second update every 20 game ticks. */
    private int tickCounter = 0;

    // ── Singleton ─────────────────────────────────────────────────────────────

    private AuctionHouseManager() {}

    public static AuctionHouseManager getInstance() {
        if (instance == null) instance = new AuctionHouseManager();
        return instance;
    }

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    /** Opens the database, loads persisted data, and registers the tick event. */
    public void initialize(MinecraftServer srv) {
        this.server = srv;

        AuctionConfig.load();
        AuctionDB db = AuctionDB.getInstance();
        db.initialize();

        activeListings.clear();
        activeListings.addAll(db.loadAllActive());

        expiredItems.clear();
        expiredItems.addAll(db.loadAllExpired());

        LOGGER.info("[AuctionHouse] Loaded {} active listing(s) and {} expired item(s).",
                activeListings.size(), expiredItems.size());

        // Register server-tick listener
        NeoForge.EVENT_BUS.register(this);
    }

    /** Called after the server fully starts so registry access is available for component serialisation. */
    public void setServer(MinecraftServer srv) {
        this.server = srv;
        AuctionComponentSerializer.setServer(srv);
    }

    /** Closes the DB connection and unregisters the tick. */
    public void shutdown() {
        try {
            NeoForge.EVENT_BUS.unregister(this);
        } catch (Exception ignored) {}
        AuctionDB.getInstance().shutdown();
        LOGGER.info("[AuctionHouse] Shutdown complete.");
    }

    // ── Server tick ───────────────────────────────────────────────────────────

    @SubscribeEvent
    public void onServerTick(ServerTickEvent.Post event) {
        tickCounter++;
        if (tickCounter < 20) return; // once per real second
        tickCounter = 0;

        List<AuctionItem> toExpire = new ArrayList<>();
        for (AuctionItem item : activeListings) {
            long remaining = item.getSecondsLeft() - 1;
            item.setSecondsLeft(remaining);
            AuctionDB.getInstance().updateTime(item.getId(), remaining);

            if (remaining <= 0) {
                toExpire.add(item);
            }
        }

        for (AuctionItem item : toExpire) {
            expireItemInternal(item);
        }
    }

    // ── Active listing management ─────────────────────────────────────────────

    /**
     * List an item from a player's inventory on the Auction House.
     *
     * @return the assigned database ID, or {@code -1} on failure.
     */
    public int sellItem(ServerPlayer seller, ItemStack stack, double price) {
        AuctionConfig cfg = AuctionConfig.get();

        // Check per-player listing cap
        if (countActiveForPlayer(seller.getStringUUID()) >= cfg.getMaxItemsPerPlayer()) {
            return -2; // sentinel: at limit
        }

        String itemKey = BuiltInRegistries.ITEM
                .getKey(stack.getItem()).toString();
        String nbtJson = ""; // components
        try {
            if (!stack.getComponentsPatch().isEmpty()) {
                JsonElement json = AuctionComponentSerializer.serialize(stack.getComponents());
                nbtJson = json.toString();
            }
        } catch (Exception e) {
            LOGGER.warn("[AuctionHouse] Could not serialize components for {}: {}", itemKey, e.getMessage());
        }

        long duration = cfg.getAuctionSecondsDuration();
        int id = AuctionDB.getInstance().addItem(
                seller.getStringUUID(), seller.getName().getString(),
                nbtJson, itemKey, stack.getCount(), price, duration);

        if (id > 0) {
            AuctionItem ai = new AuctionItem(id, seller.getStringUUID(),
                    seller.getName().getString(), nbtJson, itemKey,
                    stack.getCount(), price, duration);
            activeListings.add(ai);
        }
        return id;
    }

    /**
     * Cancel an active listing, returning the item to the seller.
     * The seller does NOT get a refund (listing was free).
     *
     * @return {@code true} if the listing was found and cancelled.
     */
    public boolean cancelListing(AuctionItem item, ServerPlayer seller) {
        if (!item.getUuid().equals(seller.getStringUUID())) return false;
        // Remove-and-check FIRST, before handing back the item: a seller with two GUI sessions
        // open on the same listing (e.g. browsed from both the main AH and "My Listings") could
        // otherwise click Cancel twice and receive two copies of the item back, since neither
        // click re-validated the listing was still active before unconditionally returning it.
        if (!activeListings.remove(item)) return false;
        AuctionDB.getInstance().removeActive(item.getId());
        // Return item to seller
        giveOrDrop(seller, item.getItemStack().copy());
        return true;
    }

    /**
     * Execute the purchase of an item: deduct from buyer, pay seller, move item.
     *
     * @return {@code true} if the purchase succeeded.
     */
    public boolean buyItem(AuctionItem item, ServerPlayer buyer) {
        // Remove-and-check FIRST, before any money/item changes hands: two buyers who both
        // opened GUIAuctionItem for the same listing (e.g. one from the main AH browse screen,
        // one from a search/refresh that hadn't caught up yet) previously could both complete
        // a purchase against the same stale AuctionItem reference, since nothing re-validated
        // the listing was still active — duplicating the item and paying the seller twice.
        if (!activeListings.remove(item)) return false;

        // Economy check — use EconomyAPI
        java.math.BigDecimal price = java.math.BigDecimal.valueOf(item.getPrice());
        java.math.BigDecimal balance = com.zerog.neoessentials.api.EconomyAPI.getBalance(buyer.getUUID());
        if (balance.compareTo(price) < 0) {
            // Not yet committed to a purchase — restore the listing so it isn't silently lost.
            activeListings.add(item);
            return false;
        }

        boolean withdrawn = com.zerog.neoessentials.api.EconomyAPI.withdraw(buyer.getUUID(), price);
        if (!withdrawn) {
            activeListings.add(item);
            return false;
        }

        AuctionDB.getInstance().removeActive(item.getId());

        // Pay seller
        try {
            java.util.UUID sellerUuid = java.util.UUID.fromString(item.getUuid());
            com.zerog.neoessentials.api.EconomyAPI.deposit(sellerUuid, price);
        } catch (Exception e) {
            LOGGER.warn("[AuctionHouse] Could not credit seller {}: {}", item.getUuid(), e.getMessage());
        }

        // Give item to buyer
        giveOrDrop(buyer, item.getItemStack().copy());

        return true;
    }

    // ── Expired item management ───────────────────────────────────────────────

    /**
     * Collect an expired item back into the player's inventory (or drop it).
     *
     * @return {@code true} if the item was in the expired list.
     */
    public boolean collectExpired(AuctionItem item, ServerPlayer player) {
        if (!expiredItems.remove(item)) return false;
        AuctionDB.getInstance().removeExpired(item.getId());
        giveOrDrop(player, item.getItemStack().copy());
        return true;
    }

    // ── Read helpers ──────────────────────────────────────────────────────────

    /** All active listings (all players). Returns an unmodifiable snapshot. */
    public List<AuctionItem> getAllActive() {
        return List.copyOf(activeListings);
    }

    /** Active listings belonging to a specific player (UUID string). */
    public List<AuctionItem> getActiveForPlayer(String uuid) {
        return activeListings.stream()
                .filter(ai -> ai.getUuid().equals(uuid))
                .collect(Collectors.toList());
    }

    /** Expired items belonging to a specific player (UUID string). */
    public List<AuctionItem> getExpiredForPlayer(String uuid) {
        return expiredItems.stream()
                .filter(ai -> ai.getUuid().equals(uuid))
                .collect(Collectors.toList());
    }

    /** Count how many active listings a player has. */
    public int countActiveForPlayer(String uuid) {
        return (int) activeListings.stream().filter(ai -> ai.getUuid().equals(uuid)).count();
    }

    /** Get an active listing by its database ID, or {@code null}. */
    public AuctionItem getActiveById(int id) {
        return activeListings.stream()
                .filter(ai -> ai.getId() == id)
                .findFirst().orElse(null);
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    /** Move an active listing into the expired list (timer ran out). */
    private void expireItemInternal(AuctionItem item) {
        activeListings.remove(item);
        expiredItems.add(item);
        AuctionDB.getInstance().expireItem(item);
    }

    /**
     * Give an ItemStack to a player; if inventory is full, drop at their feet.
     */
    private static void giveOrDrop(ServerPlayer player, ItemStack stack) {
        if (!player.getInventory().add(stack)) {
            player.drop(stack, false);
        }
    }
}

