package com.zerog.neoessentials.shop.csv;

import com.zerog.neoessentials.shop.ShopManager;
import com.zerog.neoessentials.shop.model.ShopData;
import com.zerog.neoessentials.shop.model.ShopType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.zerog.neoessentials.logging.LogCategory;
import com.zerog.neoessentials.logging.NeoLog;

import java.util.List;

/**
 * Applies a list of {@link ShopCsvSerializer.CsvRow} rows to the live {@link ShopManager}.
 *
 * <p><b>Behaviour:</b>
 * <ul>
 *   <li>Rows with {@code shop_type=SIGN_ADMIN} (or blank) update prices on the first matching
 *       admin shop by item ID, or create a new virtual admin-shop record if none exists.</li>
 *   <li>Player shops are never auto-created from CSV; only prices on existing shops are updated.</li>
 * </ul>
 */
public class ShopCsvImporter {

    private static final Logger LOGGER = LoggerFactory.getLogger(ShopCsvImporter.class);

    private ShopCsvImporter() {}

    public record ImportSummary(int updated, int created, int skipped, String details) {}

    /**
     * Apply the given CSV rows to the live shop data.
     *
     * @param rows    rows returned by {@link ShopCsvSerializer#importRows(String)}
     * @param createNew when true, admin-shop rows with no matching existing shop create a new
     *                  in-memory shop entry (position will be 0,0,0 — useful for price sheets)
     * @return summary of what happened
     */
    public static ImportSummary apply(List<ShopCsvSerializer.CsvRow> rows, boolean createNew) {
        int updated = 0, created = 0, skipped = 0;
        StringBuilder details = new StringBuilder();

        for (ShopCsvSerializer.CsvRow row : rows) {
            boolean isAdmin = row.shopType() == null
                    || row.shopType().isBlank()
                    || row.shopType().equalsIgnoreCase("SIGN_ADMIN");

            if (!isAdmin) {
                // Player shops: only update prices if a matching shop exists
                boolean found = false;
                for (ShopData s : ShopManager.getInstance().getAllShops()) {
                    if (row.itemId().equalsIgnoreCase(s.itemId) && !s.isAdminShop()) {
                        if (row.buyPrice()  != null) s.buyPrice  = row.buyPrice();
                        if (row.sellPrice() != null) s.sellPrice = row.sellPrice();
                        ShopManager.getInstance().registerShop(s);
                        updated++; found = true;
                        break;
                    }
                }
                if (!found) { skipped++; NeoLog.debug(LOGGER, LogCategory.GENERAL, "[CSV] Skipped player-shop row for {} (no match)", row.itemId()); }
                continue;
            }

            // Admin shop: find first existing admin shop for this item
            ShopData existing = null;
            for (ShopData s : ShopManager.getInstance().getAllShops()) {
                if (row.itemId().equalsIgnoreCase(s.itemId) && s.isAdminShop()) {
                    existing = s; break;
                }
            }

            if (existing != null) {
                if (row.buyPrice()  != null) existing.buyPrice  = row.buyPrice();
                if (row.sellPrice() != null) existing.sellPrice = row.sellPrice();
                ShopManager.getInstance().registerShop(existing);
                updated++;
            } else if (createNew) {
                ShopData neo = new ShopData();
                neo.shopType     = ShopType.SIGN_ADMIN;
                neo.ownerName    = ShopData.ADMIN_SHOP_NAME;
                neo.ownerUUID    = null;
                neo.itemId       = row.itemId();
                neo.quantity     = Math.max(1, row.quantity());
                neo.buyPrice     = row.buyPrice();
                neo.sellPrice    = row.sellPrice();
                neo.signDimension = "minecraft:overworld";
                neo.signX = neo.signY = neo.signZ = 0;
                neo.hasChest = false;
                ShopManager.getInstance().registerShop(neo);
                details.append(" ").append(neo.itemId);
                created++;
            } else {
                skipped++;
            }
        }

        String msg = String.format("Updated=%d Created=%d Skipped=%d%s",
                updated, created, skipped, details.length() > 0 ? " (new:" + details + ")" : "");
        NeoLog.info(LOGGER, LogCategory.GENERAL, "[ChestShop] CSV import: {}", msg);
        return new ImportSummary(updated, created, skipped, msg);
    }
}

