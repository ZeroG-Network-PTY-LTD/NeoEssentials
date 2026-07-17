package com.zerog.neoessentials.shop.dashboard;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.zerog.neoessentials.shop.ShopManager;
import com.zerog.neoessentials.shop.csv.ShopCsvImporter;
import com.zerog.neoessentials.shop.csv.ShopCsvSerializer;
import com.zerog.neoessentials.shop.entity.ShopEntityData;
import com.zerog.neoessentials.shop.entity.ShopEntityManager;
import com.zerog.neoessentials.shop.model.ShopData;

import java.io.IOException;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.Collection;

/**
 * REST endpoint for the web dashboard — mounted at {@code /api/shops/}.
 *
 * <pre>
 *   GET  /api/shops/list           — paginated list of all sign shops
 *   GET  /api/shops/stats          — aggregate statistics
 *   GET  /api/shops/npc            — NPC shop list
 *   GET  /api/shops/csv/export     — download shops as CSV
 *   POST /api/shops/csv/import     — upload CSV text in request body
 *   PUT  /api/shops/price          — update a sign shop's prices
 * </pre>
 */
public class ShopEndpoint implements HttpHandler {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String path   = exchange.getRequestURI().getPath();
        String method = exchange.getRequestMethod();

        // Importing/updating shop prices requires ADMIN, matching every other config-writing
        // endpoint group. Reads (list/stats/npc/csv export) stay open to any authenticated caller.
        if (!"GET".equalsIgnoreCase(method) && !Boolean.TRUE.equals(exchange.getAttribute("auth-admin"))) {
            respond(exchange, 403, "{\"success\":false,\"error\":\"Admin access required\"}");
            return;
        }

        try {
            if (path.endsWith("/list"))        { handleList(exchange); }
            else if (path.endsWith("/stats"))  { handleStats(exchange); }
            else if (path.endsWith("/npc"))    { handleNpc(exchange); }
            else if (path.contains("/csv/export") && "GET".equalsIgnoreCase(method))  { handleCsvExport(exchange); }
            else if (path.contains("/csv/import") && "POST".equalsIgnoreCase(method)) { handleCsvImport(exchange); }
            else if (path.endsWith("/price")   && "PUT".equalsIgnoreCase(method))      { handleSetPrice(exchange); }
            else { respond(exchange, 404, "{\"error\":\"Not found\"}"); }
        } catch (Exception e) {
            respond(exchange, 500, "{\"error\":\"" + e.getMessage() + "\"}");
        }
    }

    // ── GET /api/shops/list ───────────────────────────────────────────────────

    private void handleList(HttpExchange exchange) throws IOException {
        String query  = exchange.getRequestURI().getQuery();
        int    page   = 1, size = 50;
        if (query != null) {
            for (String p : query.split("&")) {
                if (p.startsWith("page="))  try { page = Integer.parseInt(p.substring(5)); } catch (Exception ignored) {}
                if (p.startsWith("size="))  try { size = Integer.parseInt(p.substring(5)); } catch (Exception ignored) {}
            }
        }

        Collection<ShopData> all = ShopManager.getInstance().getAllShops();
        JsonArray arr = new JsonArray();
        int skip = (page - 1) * size, count = 0;
        for (ShopData s : all) {
            if (skip-- > 0) continue;
            if (count++ >= size) break;
            arr.add(shopToJson(s));
        }

        JsonObject resp = new JsonObject();
        resp.addProperty("page",  page);
        resp.addProperty("size",  size);
        resp.addProperty("total", all.size());
        resp.add("shops", arr);
        respond(exchange, 200, GSON.toJson(resp));
    }

    // ── GET /api/shops/stats ──────────────────────────────────────────────────

    private void handleStats(HttpExchange exchange) throws IOException {
        Collection<ShopData> all = ShopManager.getInstance().getAllShops();
        long totalTransactions = all.stream().mapToLong(s -> s.totalSalesCount).sum();
        long adminShops  = all.stream().filter(ShopData::isAdminShop).count();
        long playerShops = all.size() - adminShops;

        JsonObject resp = new JsonObject();
        resp.addProperty("totalShops",        all.size());
        resp.addProperty("adminShops",         adminShops);
        resp.addProperty("playerShops",        playerShops);
        resp.addProperty("totalTransactions",  totalTransactions);
        resp.addProperty("npcShops",           ShopEntityManager.getInstance().getShopCount());

        // Top 5 by sales
        JsonArray top = new JsonArray();
        all.stream()
           .filter(s -> s.totalSalesCount > 0)
           .sorted((a, b) -> Long.compare(b.totalSalesCount, a.totalSalesCount))
           .limit(5)
           .forEach(s -> {
               JsonObject o = new JsonObject();
               o.addProperty("item",  s.itemId);
               o.addProperty("sales", s.totalSalesCount);
               o.addProperty("owner", s.ownerName);
               top.add(o);
           });
        resp.add("topSellers", top);
        respond(exchange, 200, GSON.toJson(resp));
    }

    // ── GET /api/shops/npc ────────────────────────────────────────────────────

    private void handleNpc(HttpExchange exchange) throws IOException {
        JsonArray arr = new JsonArray();
        for (ShopEntityData d : ShopEntityManager.getInstance().getAll()) {
            JsonObject o = new JsonObject();
            o.addProperty("shopId",    d.shopId != null ? d.shopId.toString() : null);
            o.addProperty("shopName",  d.shopName);
            o.addProperty("ownerUUID", d.ownerUUID != null ? d.ownerUUID.toString() : null);
            o.addProperty("dimension", d.dimension);
            o.addProperty("x", d.spawnX);
            o.addProperty("y", d.spawnY);
            o.addProperty("z", d.spawnZ);
            o.addProperty("listings",  d.listings.size());
            arr.add(o);
        }
        JsonObject resp = new JsonObject();
        resp.addProperty("total", ShopEntityManager.getInstance().getShopCount());
        resp.add("shops", arr);
        respond(exchange, 200, GSON.toJson(resp));
    }

    // ── GET /api/shops/csv/export ─────────────────────────────────────────────

    private void handleCsvExport(HttpExchange exchange) throws IOException {
        String csv = ShopCsvSerializer.export(ShopManager.getInstance().getAllShops());
        byte[] bytes = csv.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "text/csv; charset=UTF-8");
        exchange.getResponseHeaders().add("Content-Disposition", "attachment; filename=\"shop_prices.csv\"");
        exchange.sendResponseHeaders(200, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) { os.write(bytes); }
    }

    // ── POST /api/shops/csv/import ────────────────────────────────────────────

    private void handleCsvImport(HttpExchange exchange) throws IOException {
        String body       = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
        var    rows       = ShopCsvSerializer.importRows(body);
        var    result     = ShopCsvImporter.apply(rows, false);
        JsonObject resp   = new JsonObject();
        resp.addProperty("updated", result.updated());
        resp.addProperty("created", result.created());
        resp.addProperty("skipped", result.skipped());
        resp.addProperty("details", result.details());
        respond(exchange, 200, GSON.toJson(resp));
    }

    // ── PUT /api/shops/price ──────────────────────────────────────────────────

    private void handleSetPrice(HttpExchange exchange) throws IOException {
        String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
        JsonObject req = GSON.fromJson(body, JsonObject.class);

        String signKey = req.has("signKey") ? req.get("signKey").getAsString() : null;
        if (signKey == null) { respond(exchange, 400, "{\"error\":\"signKey required\"}"); return; }

        ShopData shop = ShopManager.getInstance().getAllShops().stream()
                .filter(s -> s.toKey().equals(signKey)).findFirst().orElse(null);
        if (shop == null) { respond(exchange, 404, "{\"error\":\"Shop not found\"}"); return; }

        if (req.has("buyPrice") && !req.get("buyPrice").isJsonNull()) {
            shop.buyPrice  = new BigDecimal(req.get("buyPrice").getAsString());
        }
        if (req.has("sellPrice") && !req.get("sellPrice").isJsonNull()) {
            shop.sellPrice = new BigDecimal(req.get("sellPrice").getAsString());
        }
        ShopManager.getInstance().registerShop(shop);

        JsonObject resp = new JsonObject();
        resp.addProperty("success", true);
        resp.addProperty("signKey", signKey);
        respond(exchange, 200, GSON.toJson(resp));
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private static JsonObject shopToJson(ShopData s) {
        JsonObject o = new JsonObject();
        o.addProperty("signKey",    s.toKey());
        o.addProperty("ownerName",  s.ownerName);
        o.addProperty("ownerUUID",  s.ownerUUID != null ? s.ownerUUID.toString() : null);
        o.addProperty("itemId",     s.itemId);
        o.addProperty("quantity",   s.quantity);
        o.addProperty("buyPrice",   s.buyPrice  != null ? s.buyPrice.toPlainString()  : null);
        o.addProperty("sellPrice",  s.sellPrice != null ? s.sellPrice.toPlainString() : null);
        o.addProperty("isAdmin",    s.isAdminShop());
        o.addProperty("shopType",   s.resolvedShopType().name());
        o.addProperty("totalSales", s.totalSalesCount);
        return o;
    }

    private static void respond(HttpExchange exchange, int code, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "application/json; charset=UTF-8");
        exchange.sendResponseHeaders(code, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) { os.write(bytes); }
    }
}

