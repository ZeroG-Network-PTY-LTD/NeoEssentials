package com.zerog.neoessentials.shop.csv;

import com.zerog.neoessentials.shop.model.ShopData;
import com.zerog.neoessentials.logging.LogCategory;
import com.zerog.neoessentials.logging.NeoLog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Serializes and deserializes shop data as comma-separated values.
 *
 * <p>CSV format (with header):
 * <pre>
 *   item_id,buy_price,sell_price,quantity,shop_type,owner_name,sign_x,sign_y,sign_z,total_sales
 * </pre>
 *
 * <p>Blank {@code buy_price} / {@code sell_price} fields mean the operation is disabled.
 */
public final class ShopCsvSerializer {
    private static final Logger LOGGER = LoggerFactory.getLogger(ShopCsvSerializer.class);

    private static final String HEADER =
            "item_id,buy_price,sell_price,quantity,shop_type,owner_name,sign_x,sign_y,sign_z,total_sales";

    private ShopCsvSerializer() {}

    // ── Export ────────────────────────────────────────────────────────────────

    /**
     * Export all shops as a CSV string including the header row.
     *
     * @param shops collection of shops to export
     * @return CSV text (UTF-8 safe)
     */
    public static String export(Collection<ShopData> shops) {
        StringBuilder sb = new StringBuilder(HEADER).append('\n');
        for (ShopData s : shops) {
            sb.append(escape(s.itemId)).append(',');
            sb.append(s.buyPrice  != null ? s.buyPrice.toPlainString()  : "").append(',');
            sb.append(s.sellPrice != null ? s.sellPrice.toPlainString() : "").append(',');
            sb.append(s.quantity).append(',');
            sb.append(s.resolvedShopType().name()).append(',');
            sb.append(escape(s.ownerName)).append(',');
            sb.append(s.signX).append(',');
            sb.append(s.signY).append(',');
            sb.append(s.signZ).append(',');
            sb.append(s.totalSalesCount);
            sb.append('\n');
        }
        return sb.toString();
    }

    // ── Import ────────────────────────────────────────────────────────────────

    /**
     * Parse CSV text (with or without header) into a list of row records.
     * Rows with missing mandatory fields (item_id, quantity) are silently skipped.
     *
     * @param csv raw CSV text
     * @return list of parsed rows; never null
     */
    public static List<CsvRow> importRows(String csv) {
        List<CsvRow> rows = new ArrayList<>();
        if (csv == null || csv.isBlank()) return rows;

        String[] lines = csv.split("\\r?\\n");
        for (String line : lines) {
            line = line.trim();
            if (line.isEmpty() || line.startsWith("item_id")) continue; // skip header

            String[] cols = splitCsv(line);
            if (cols.length < 4) continue;

            try {
                String   itemId    = unescape(cols[0]);
                String   buyStr    = cols.length > 1 ? cols[1].trim() : "";
                String   sellStr   = cols.length > 2 ? cols[2].trim() : "";
                int      quantity  = Integer.parseInt(cols[3].trim());
                String   shopType  = cols.length > 4 ? cols[4].trim() : "";
                String   ownerName = cols.length > 5 ? unescape(cols[5]) : "";

                if (itemId.isBlank() || quantity < 1) continue;

                BigDecimal buyPrice  = buyStr.isBlank()  ? null : new BigDecimal(buyStr);
                BigDecimal sellPrice = sellStr.isBlank() ? null : new BigDecimal(sellStr);

                rows.add(new CsvRow(itemId, buyPrice, sellPrice, quantity, shopType, ownerName));
            } catch (NumberFormatException e) {
                NeoLog.debug(LOGGER, LogCategory.GENERAL, "Skipping malformed CSV row: {}", line, e);
            }
        }
        return rows;
    }

    // ── Row record ────────────────────────────────────────────────────────────

    public record CsvRow(
            String     itemId,
            BigDecimal buyPrice,
            BigDecimal sellPrice,
            int        quantity,
            String     shopType,
            String     ownerName
    ) {}

    // ── Helpers ───────────────────────────────────────────────────────────────

    /** Wrap value in quotes if it contains a comma or quote. */
    private static String escape(String value) {
        if (value == null) return "";
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return '"' + value.replace("\"", "\"\"") + '"';
        }
        return value;
    }

    private static String unescape(String value) {
        if (value == null) return "";
        value = value.trim();
        if (value.startsWith("\"") && value.endsWith("\"") && value.length() >= 2) {
            value = value.substring(1, value.length() - 1).replace("\"\"", "\"");
        }
        return value;
    }

    /** Split a CSV line respecting quoted fields containing commas. */
    private static String[] splitCsv(String line) {
        List<String> fields = new ArrayList<>();
        boolean inQuotes = false;
        StringBuilder current = new StringBuilder();
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '"') {
                if (inQuotes && i + 1 < line.length() && line.charAt(i + 1) == '"') {
                    current.append('"'); i++;
                } else {
                    inQuotes = !inQuotes;
                }
            } else if (c == ',' && !inQuotes) {
                fields.add(current.toString());
                current.setLength(0);
            } else {
                current.append(c);
            }
        }
        fields.add(current.toString());
        return fields.toArray(new String[0]);
    }
}

