package com.zerog.neoessentials.shop.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.zerog.neoessentials.api.permissions.PermissionAPI;
import com.zerog.neoessentials.economy.managers.EconomyManager;
import com.zerog.neoessentials.hologram.integration.ShopHologramManager;
import com.zerog.neoessentials.shop.ShopManager;
import com.zerog.neoessentials.shop.csv.ShopCsvImporter;
import com.zerog.neoessentials.shop.csv.ShopCsvSerializer;
import com.zerog.neoessentials.shop.handlers.ShopSignHandler;
import com.zerog.neoessentials.shop.model.ShopData;
import com.zerog.neoessentials.util.ResourceUtil;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/**
 * /chestshop (alias /cshop, /shop) — admin and player shop management commands.
 */
public class ShopCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        var node = Commands.literal("chestshop")
            .then(Commands.literal("list")
                .executes(ctx -> executeList(ctx.getSource(), null))
                .then(Commands.argument("player", StringArgumentType.word())
                    .executes(ctx -> executeList(ctx.getSource(),
                        StringArgumentType.getString(ctx, "player")))))
            .then(Commands.literal("info")
                .executes(ctx -> executeInfo(ctx.getSource())))
            .then(Commands.literal("convert")
                .executes(ctx -> executeConvert(ctx.getSource())))
            .then(Commands.literal("remove")
                .then(Commands.argument("x", com.mojang.brigadier.arguments.IntegerArgumentType.integer())
                    .then(Commands.argument("y", com.mojang.brigadier.arguments.IntegerArgumentType.integer())
                        .then(Commands.argument("z", com.mojang.brigadier.arguments.IntegerArgumentType.integer())
                            .executes(ctx -> executeRemove(ctx.getSource(),
                                com.mojang.brigadier.arguments.IntegerArgumentType.getInteger(ctx, "x"),
                                com.mojang.brigadier.arguments.IntegerArgumentType.getInteger(ctx, "y"),
                                com.mojang.brigadier.arguments.IntegerArgumentType.getInteger(ctx, "z")))))))
            .then(Commands.literal("setprice")
                .then(Commands.argument("type", StringArgumentType.word())   // buy|sell|both
                    .then(Commands.argument("price", DoubleArgumentType.doubleArg(0))
                        .executes(ctx -> executeSetPrice(ctx.getSource(),
                            StringArgumentType.getString(ctx, "type"),
                            DoubleArgumentType.getDouble(ctx, "price"))))))
            .then(Commands.literal("stats")
                .executes(ctx -> executeStats(ctx.getSource())))
            .then(Commands.literal("limit")
                .executes(ctx -> executeLimit(ctx.getSource())))
            .then(Commands.literal("export")
                .executes(ctx -> executeExport(ctx.getSource())))
            .then(Commands.literal("import")
                .executes(ctx -> executeImport(ctx.getSource(), false))
                .then(Commands.literal("create")
                    .executes(ctx -> executeImport(ctx.getSource(), true))))
            .then(Commands.literal("reload")
                .requires(src -> src.hasPermission(3) ||
                    (src.getEntity() != null &&
                     PermissionAPI.hasPermission(src.getEntity().getUUID(), "neoessentials.shop.admin.reload")))
                .executes(ctx -> executeReload(ctx.getSource())))
            .then(Commands.literal("pricing")
                .executes(ctx -> executePricingStatus(ctx.getSource())))
            .then(Commands.literal("hologram")
                .then(Commands.literal("enable")
                    .executes(ctx -> executeHologramEnable(ctx.getSource())))
                .then(Commands.literal("disable")
                    .executes(ctx -> executeHologramDisable(ctx.getSource())))
                // Internal: called by the clickable chat message with exact sign coordinates
                .then(Commands.literal("enablepos")
                    .then(Commands.argument("x", IntegerArgumentType.integer())
                        .then(Commands.argument("y", IntegerArgumentType.integer())
                            .then(Commands.argument("z", IntegerArgumentType.integer())
                                .executes(ctx -> executeHologramEnablePos(ctx.getSource(),
                                    IntegerArgumentType.getInteger(ctx, "x"),
                                    IntegerArgumentType.getInteger(ctx, "y"),
                                    IntegerArgumentType.getInteger(ctx, "z")))))))
                // Move hologram: offsets relative to sign block, clamped to 9×9×9 (±4.5)
                .then(Commands.literal("move")
                    .then(Commands.argument("x", DoubleArgumentType.doubleArg(-4.5, 4.5))
                        .then(Commands.argument("y", DoubleArgumentType.doubleArg(-4.5, 4.5))
                            .then(Commands.argument("z", DoubleArgumentType.doubleArg(-4.5, 4.5))
                                .executes(ctx -> executeHologramMove(ctx.getSource(),
                                    DoubleArgumentType.getDouble(ctx, "x"),
                                    DoubleArgumentType.getDouble(ctx, "y"),
                                    DoubleArgumentType.getDouble(ctx, "z"))))))))
            .executes(ctx -> executeHelp(ctx.getSource()));

        dispatcher.register(node);

        // Aliases
        dispatcher.register(Commands.literal("cshop").redirect(dispatcher.getRoot().getChild("chestshop")));
    }

    // ── /chestshop list [player] ──────────────────────────────────────────────

    private static int executeList(CommandSourceStack src, String targetName) {
        try {
            UUID uuid;
            String displayName;

            if (targetName == null) {
                ServerPlayer self = src.getPlayerOrException();
                uuid = self.getUUID();
                displayName = self.getName().getString();
            } else {
                boolean canListOthers = src.hasPermission(3) ||
                    (src.getEntity() != null &&
                     PermissionAPI.hasPermission(src.getEntity().getUUID(), "neoessentials.shop.list.others"));
                if (!canListOthers) {
                    src.sendFailure(Component.literal("§cYou don't have permission to list others' shops."));
                    return 0;
                }
                // Resolve UUID by online player name
                var server = src.getServer();
                ServerPlayer target = server.getPlayerList().getPlayerByName(targetName);
                if (target == null) {
                    src.sendFailure(Component.literal("§cPlayer not found: " + targetName));
                    return 0;
                }
                uuid = target.getUUID();
                displayName = targetName;
            }

            List<ShopData> shops = ShopManager.getInstance().getShopsByOwner(uuid);
            src.sendSuccess(() -> Component.literal("§6§l=== Shops owned by " + displayName +
                " (" + shops.size() + ") ==="), false);
            if (shops.isEmpty()) {
                src.sendSuccess(() -> Component.literal("§7No shops found."), false);
            } else {
                String currency = EconomyManager.getInstance().getCurrencySymbol();
                for (ShopData s : shops) {
                    src.sendSuccess(() -> Component.literal(String.format(
                        "§e%s §f@ §7(%d,%d,%d) §e| §f%dx %s §e| Buy:§f%s §e| Sell:§f%s",
                        s.signDimension.replace("minecraft:", ""),
                        s.signX, s.signY, s.signZ,
                        s.quantity,
                        s.itemId.replace("minecraft:", ""),
                        s.buyPrice  != null ? currency + s.buyPrice.toPlainString()  : "§7—",
                        s.sellPrice != null ? currency + s.sellPrice.toPlainString() : "§7—"
                    )), false);
                }
            }
            return shops.size();
        } catch (Exception e) {
            src.sendFailure(Component.literal("§cError: " + e.getMessage()));
            return 0;
        }
    }

    // ── /chestshop info ───────────────────────────────────────────────────────

    @SuppressWarnings("resource") // ServerLevel is not AutoCloseable; IntelliJ false positive
    private static int executeInfo(CommandSourceStack src) {
        try {
            ServerPlayer player = src.getPlayerOrException();
            HitResult hit = player.pick(5.0, 0.0f, false);
            if (hit.getType() != HitResult.Type.BLOCK) {
                src.sendFailure(Component.literal("§cLook at a shop sign."));
                return 0;
            }
            BlockPos pos = ((BlockHitResult) hit).getBlockPos();
            ServerLevel level = player.serverLevel();
            String dimension = level.dimension().location().toString();

            ShopData shop = ShopManager.getInstance().getShopBySign(dimension, pos);
            if (shop == null) {
                src.sendFailure(Component.literal("§cNo shop at that sign."));
                return 0;
            }

            String currency = EconomyManager.getInstance().getCurrencySymbol();
            src.sendSuccess(() -> Component.literal("§6§l--- Shop Info ---"), false);
            src.sendSuccess(() -> Component.literal("§eOwner: §f" + shop.ownerName +
                (shop.isAdminShop() ? " §2[Admin]" : "")), false);
            src.sendSuccess(() -> Component.literal("§eItem:  §f" + shop.quantity + "x " +
                shop.itemId.replace("minecraft:", "")), false);
            if (shop.buyPrice  != null) src.sendSuccess(() -> Component.literal(
                "§eBuy:   §f" + currency + shop.buyPrice.toPlainString()), false);
            if (shop.sellPrice != null) src.sendSuccess(() -> Component.literal(
                "§eSell:  §f" + currency + shop.sellPrice.toPlainString()), false);
            src.sendSuccess(() -> Component.literal("§eSign:  §7" +
                shop.signX + ", " + shop.signY + ", " + shop.signZ), false);
            return 1;
        } catch (Exception e) {
            src.sendFailure(Component.literal("§cError: " + e.getMessage()));
            return 0;
        }
    }

    // ── /chestshop convert ────────────────────────────────────────────────────
    /** Register the sign the player is looking at as a shop (for pre-existing signs). */
    private static int executeConvert(CommandSourceStack src) {
        try {
            ServerPlayer player = src.getPlayerOrException();
            if (!PermissionAPI.hasPermission(player.getUUID(), "neoessentials.shop.create")) {
                src.sendFailure(Component.literal("§cNo permission."));
                return 0;
            }
            HitResult hit = player.pick(5.0, 0.0f, false);
            if (hit.getType() != HitResult.Type.BLOCK) {
                src.sendFailure(Component.literal("§cLook at a sign."));
                return 0;
            }
            BlockPos pos = ((BlockHitResult) hit).getBlockPos();
            ServerLevel level = player.serverLevel();
            BlockEntity be = level.getBlockEntity(pos);
            if (!(be instanceof SignBlockEntity sign)) {
                src.sendFailure(Component.literal("§cNot a sign."));
                return 0;
            }
            String dimension = level.dimension().location().toString();
            String[] lines = ShopSignHandler.readSignLines(sign);
            ShopSignHandler.tryRegisterShop(player, lines, pos, dimension, level);
            return 1;
        } catch (Exception e) {
            src.sendFailure(Component.literal("§cError: " + e.getMessage()));
            return 0;
        }
    }

    // ── /chestshop remove <x> <y> <z> ────────────────────────────────────────

    @SuppressWarnings("resource") // ServerLevel is not AutoCloseable; IntelliJ false positive
    private static int executeRemove(CommandSourceStack src, int x, int y, int z) {
        boolean isAdmin = src.hasPermission(3) ||
            (src.getEntity() != null &&
             PermissionAPI.hasPermission(src.getEntity().getUUID(), "neoessentials.shop.admin.remove"));
        if (!isAdmin) {
            src.sendFailure(Component.literal("§cNo permission."));
            return 0;
        }
        try {
            ServerPlayer player = src.getPlayerOrException();
            String dimension = player.serverLevel().dimension().location().toString();
            BlockPos pos = new BlockPos(x, y, z);
            ShopData removed = ShopManager.getInstance().removeShop(dimension, pos);
            if (removed == null) {
                src.sendFailure(Component.literal("§cNo shop found at " + x + ", " + y + ", " + z));
                return 0;
            }
            src.sendSuccess(() -> Component.literal("§aRemoved shop owned by §f" +
                removed.ownerName + " §aat §7" + x + ", " + y + ", " + z), true);
            return 1;
        } catch (Exception e) {
            src.sendFailure(Component.literal("§cError: " + e.getMessage()));
            return 0;
        }
    }

    // ── /chestshop reload ─────────────────────────────────────────────────────

    private static int executeReload(CommandSourceStack src) {
        ShopManager.getInstance().reload();
        com.zerog.neoessentials.shop.pricing.PricingEngine.getInstance().loadConfig();
        src.sendSuccess(() -> Component.literal("§aChestShop data reloaded. §f" +
            ShopManager.getInstance().getShopCount() + " §ashop(s) loaded. Pricing engine updated."), true);
        return 1;
    }

    // ── /chestshop setprice <buy|sell|both> <price> ──────────────────────────

    private static int executeSetPrice(CommandSourceStack src, String type, double price) {
        try {
            ServerPlayer player = src.getPlayerOrException();
            boolean canSetPrice = src.hasPermission(3) ||
                    PermissionAPI.hasPermission(player.getUUID(), "neoessentials.shop.setprice") ||
                    PermissionAPI.hasPermission(player.getUUID(), "neoessentials.shop.admin.setprice");
            if (!canSetPrice) {
                src.sendFailure(Component.literal("§cNo permission."));
                return 0;
            }
            HitResult hit = player.pick(5.0, 0.0f, false);
            if (hit.getType() != HitResult.Type.BLOCK) {
                src.sendFailure(Component.literal("§cLook at a shop sign."));
                return 0;
            }
            BlockPos pos = ((BlockHitResult) hit).getBlockPos();
            ServerLevel level = player.serverLevel();
            String dimension = level.dimension().location().toString();
            ShopData shop = ShopManager.getInstance().getShopBySign(dimension, pos);
            if (shop == null) {
                src.sendFailure(Component.literal("§cNo shop at that sign."));
                return 0;
            }
            // Owner or admin can set price
            boolean isAdmin = src.hasPermission(3) ||
                    PermissionAPI.hasPermission(player.getUUID(), "neoessentials.shop.admin.setprice");
            if (!isAdmin && (shop.ownerUUID == null || !shop.ownerUUID.equals(player.getUUID()))) {
                src.sendFailure(Component.literal("§cYou can only set prices on your own shops."));
                return 0;
            }
            BigDecimal bd = BigDecimal.valueOf(price);
            switch (type.toLowerCase()) {
                case "buy"  -> shop.buyPrice  = bd;
                case "sell" -> shop.sellPrice = bd;
                case "both" -> { shop.buyPrice = bd; shop.sellPrice = bd; }
                default -> {
                    src.sendFailure(Component.literal("§cType must be: buy | sell | both"));
                    return 0;
                }
            }
            ShopManager.getInstance().registerShop(shop);
            com.zerog.neoessentials.shop.handlers.ShopSignHandler.writeSignLines(level, pos,
                    com.zerog.neoessentials.shop.ShopParser.formatSignLines(shop));
            src.sendSuccess(() -> Component.literal("§aPrice updated — " + type + ": " +
                    EconomyManager.getInstance().getCurrencySymbol() + bd.toPlainString()), false);
            return 1;
        } catch (Exception e) {
            src.sendFailure(Component.literal("§cError: " + e.getMessage()));
            return 0;
        }
    }

    // ── /chestshop stats ──────────────────────────────────────────────────────

    private static int executeStats(CommandSourceStack src) {
        try {
            ServerPlayer player = src.getPlayerOrException();
            List<ShopData> myShops = ShopManager.getInstance().getShopsByOwner(player.getUUID());
            long totalSales = myShops.stream().mapToLong(s -> s.totalSalesCount).sum();
            int adminCount  = (int) myShops.stream().filter(ShopData::isAdminShop).count();
            int playerCount = myShops.size() - adminCount;
            src.sendSuccess(() -> Component.literal("§6§l--- Shop Stats ---"), false);
            src.sendSuccess(() -> Component.literal("§eTotal shops:  §f" + myShops.size() + " §7(player: " + playerCount + ", admin: " + adminCount + ")"), false);
            src.sendSuccess(() -> Component.literal("§eTotal sales:  §f" + totalSales), false);
            if (!myShops.isEmpty()) {
                myShops.stream().max(Comparator.comparingLong(s -> s.totalSalesCount))
                        .ifPresent(top -> src.sendSuccess(() -> Component.literal("§eTop seller:   §f" +
                                top.itemId.replace("minecraft:", "") +
                                " §7(" + top.totalSalesCount + " sales)"), false));
            }
            return 1;
        } catch (Exception e) {
            src.sendFailure(Component.literal("§cError: " + e.getMessage()));
            return 0;
        }
    }

    // ── /chestshop limit ──────────────────────────────────────────────────────

    private static int executeLimit(CommandSourceStack src) {
        try {
            ServerPlayer player = src.getPlayerOrException();
            int used  = ShopManager.getInstance().getShopsByOwner(player.getUUID()).size();
            int max   = getMaxShopsPerPlayer();
            src.sendSuccess(() -> Component.literal("§eShops used: §f" + used + " §7/ §f" +
                    (max < 0 ? "unlimited" : String.valueOf(max))), false);
            return 1;
        } catch (Exception e) {
            src.sendFailure(Component.literal("§cError: " + e.getMessage()));
            return 0;
        }
    }

    // ── /chestshop export ─────────────────────────────────────────────────────

    private static int executeExport(CommandSourceStack src) {
        boolean isAdmin = src.hasPermission(3) ||
                (src.getEntity() != null &&
                 PermissionAPI.hasPermission(src.getEntity().getUUID(), "neoessentials.shop.admin.csv.export"));
        if (!isAdmin) { src.sendFailure(Component.literal("§cNo permission.")); return 0; }
        try {
            String csv = ShopCsvSerializer.export(ShopManager.getInstance().getAllShops());
            Path file  = getCsvPath();
            Files.createDirectories(file.getParent());
            Files.writeString(file, csv);
            src.sendSuccess(() -> Component.literal("§aExported §f" +
                    ShopManager.getInstance().getShopCount() + " §ashop(s) to §7" + file.getFileName()), true);
            return 1;
        } catch (Exception e) {
            src.sendFailure(Component.literal("§cExport failed: " + e.getMessage()));
            return 0;
        }
    }

    // ── /chestshop import [create] ────────────────────────────────────────────

    private static int executeImport(CommandSourceStack src, boolean createNew) {
        boolean isAdmin = src.hasPermission(3) ||
                (src.getEntity() != null &&
                 PermissionAPI.hasPermission(src.getEntity().getUUID(), "neoessentials.shop.admin.csv.import"));
        if (!isAdmin) { src.sendFailure(Component.literal("§cNo permission.")); return 0; }
        try {
            Path file = getCsvPath();
            if (!Files.exists(file)) {
                src.sendFailure(Component.literal("§cCSV file not found: " + file + ". Run /chestshop export first."));
                return 0;
            }
            String csv = Files.readString(file);
            var rows   = ShopCsvSerializer.importRows(csv);
            var result = ShopCsvImporter.apply(rows, createNew);
            src.sendSuccess(() -> Component.literal("§aCSV import complete: §f" + result.details()), true);
            return result.updated() + result.created();
        } catch (Exception e) {
            src.sendFailure(Component.literal("§cImport failed: " + e.getMessage()));
            return 0;
        }
    }

    // ── /chestshop hologram enable ────────────────────────────────────────────

    /** Enable hologram on the shop sign the player is currently looking at. */
    private static int executeHologramEnable(CommandSourceStack src) {
        try {
            ServerPlayer player = src.getPlayerOrException();
            ShopData shop = getShopFromLookAt(player);
            if (shop == null) { src.sendFailure(Component.literal("§cLook at a shop sign.")); return 0; }
            if (!isShopOwner(player, shop)) {
                src.sendFailure(Component.literal("§cOnly the shop owner can enable its hologram."));
                return 0;
            }
            if (shop.hologramEnabled) {
                src.sendSuccess(() -> Component.literal("§eHologram is already enabled for this shop."), false);
                return 1;
            }
            ShopHologramManager.enableShopHologram(shop);
            src.sendSuccess(() -> Component.literal(
                "§aHologram enabled! Right/left-click it to buy/sell. Move it with §e/chestshop hologram move <x> <y> <z>§a."), false);
            return 1;
        } catch (Exception e) {
            src.sendFailure(Component.literal("§cError: " + e.getMessage()));
            return 0;
        }
    }

    /** Enable hologram for the shop at the given sign coordinates (used by clickable chat message). */
    private static int executeHologramEnablePos(CommandSourceStack src, int x, int y, int z) {
        try {
            ServerPlayer player = src.getPlayerOrException();
            String dimension = player.serverLevel().dimension().location().toString();
            ShopData shop = ShopManager.getInstance().getShopBySign(dimension, new BlockPos(x, y, z));
            if (shop == null) { src.sendFailure(Component.literal("§cNo shop found at that position.")); return 0; }
            if (!isShopOwner(player, shop)) {
                src.sendFailure(Component.literal("§cOnly the shop owner can enable its hologram."));
                return 0;
            }
            if (shop.hologramEnabled) {
                src.sendSuccess(() -> Component.literal("§eHologram is already enabled for this shop."), false);
                return 1;
            }
            ShopHologramManager.enableShopHologram(shop);
            src.sendSuccess(() -> Component.literal(
                "§aHologram enabled! Right/left-click it to buy/sell. Move it with §e/chestshop hologram move <x> <y> <z>§a."), false);
            return 1;
        } catch (Exception e) {
            src.sendFailure(Component.literal("§cError: " + e.getMessage()));
            return 0;
        }
    }

    // ── /chestshop hologram disable ───────────────────────────────────────────

    private static int executeHologramDisable(CommandSourceStack src) {
        try {
            ServerPlayer player = src.getPlayerOrException();
            ShopData shop = getShopFromLookAt(player);
            if (shop == null) { src.sendFailure(Component.literal("§cLook at a shop sign.")); return 0; }
            if (!isShopOwner(player, shop)) {
                src.sendFailure(Component.literal("§cOnly the shop owner can disable its hologram."));
                return 0;
            }
            if (!shop.hologramEnabled) {
                src.sendSuccess(() -> Component.literal("§eHologram is already disabled for this shop."), false);
                return 1;
            }
            ShopHologramManager.disableShopHologram(shop);
            src.sendSuccess(() -> Component.literal("§aHologram removed."), false);
            return 1;
        } catch (Exception e) {
            src.sendFailure(Component.literal("§cError: " + e.getMessage()));
            return 0;
        }
    }

    // ── /chestshop hologram move <x> <y> <z> ─────────────────────────────────

    /**
     * Move the hologram to a new position offset (in blocks) relative to the sign block.
     * Clamped to ±4.5 blocks per axis (9×9×9 cube around the sign).
     * Player must be looking at their shop sign.
     */
    private static int executeHologramMove(CommandSourceStack src, double ox, double oy, double oz) {
        try {
            ServerPlayer player = src.getPlayerOrException();
            ShopData shop = getShopFromLookAt(player);
            if (shop == null) { src.sendFailure(Component.literal("§cLook at a shop sign.")); return 0; }
            if (!isShopOwner(player, shop)) {
                src.sendFailure(Component.literal("§cOnly the shop owner can move the hologram."));
                return 0;
            }
            if (!shop.hologramEnabled) {
                src.sendFailure(Component.literal(
                    "§cHologram is not enabled. Enable it first with §e/chestshop hologram enable§c."));
                return 0;
            }
            boolean moved = ShopHologramManager.moveShopHologram(shop, ox, oy, oz);
            if (!moved) {
                src.sendFailure(Component.literal("§cCould not move hologram."));
                return 0;
            }
            double cx = shop.hologramOffsetX, cy = shop.hologramOffsetY, cz = shop.hologramOffsetZ;
            src.sendSuccess(() -> Component.literal(String.format(
                "§aHologram moved to offset §f%.2f, %.2f, %.2f §a(relative to sign).", cx, cy, cz)), false);
            return 1;
        } catch (Exception e) {
            src.sendFailure(Component.literal("§cError: " + e.getMessage()));
            return 0;
        }
    }

    // ── Shared helpers ────────────────────────────────────────────────────────

    @SuppressWarnings("resource")
    private static ShopData getShopFromLookAt(ServerPlayer player) {
        HitResult hit = player.pick(5.0, 0.0f, false);
        if (hit.getType() != HitResult.Type.BLOCK) return null;
        BlockPos pos = ((BlockHitResult) hit).getBlockPos();
        ServerLevel level = player.serverLevel();
        String dimension = level.dimension().location().toString();
        return ShopManager.getInstance().getShopBySign(dimension, pos);
    }

    private static boolean isShopOwnerOrAdmin(ServerPlayer player, ShopData shop) {
        if (player.hasPermissions(3)) return true;
        if (PermissionAPI.hasPermission(player.getUUID(), "neoessentials.shop.admin.remove")) return true;
        return shop.ownerUUID != null && shop.ownerUUID.equals(player.getUUID());
    }

    /**
     * Returns {@code true} only if {@code player} is the recorded owner of {@code shop}.
     * Admin flags are intentionally NOT checked — hologram management is owner-exclusive
     * so that server staff cannot enable or reposition holograms on behalf of players.
     * (Admins can still use {@code /hologram remove <id>} for moderation if needed.)
     */
    private static boolean isShopOwner(ServerPlayer player, ShopData shop) {
        return shop.ownerUUID != null && shop.ownerUUID.equals(player.getUUID());
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private static Path getCsvPath() {
        try {
            var cfg = com.zerog.neoessentials.config.ConfigManager.getInstance()
                    .getConfig(com.zerog.neoessentials.config.ConfigManager.MAIN_CONFIG);
            if (cfg != null && cfg.has("shop")) {
                var shopCfg = cfg.getAsJsonObject("shop");
                if (shopCfg.has("csvImportPath")) {
                    return java.nio.file.Paths.get(shopCfg.get("csvImportPath").getAsString());
                }
            }
        } catch (Exception ignored) {}
        return ResourceUtil.getConfigPath("shop_prices.csv");
    }

    private static int getMaxShopsPerPlayer() {
        try {
            var cfg = com.zerog.neoessentials.config.ConfigManager.getInstance()
                    .getConfig(com.zerog.neoessentials.config.ConfigManager.MAIN_CONFIG);
            if (cfg != null && cfg.has("shop")) {
                var shopCfg = cfg.getAsJsonObject("shop");
                if (shopCfg.has("maxShopsPerPlayer")) return shopCfg.get("maxShopsPerPlayer").getAsInt();
            }
        } catch (Exception ignored) {}
        return 10;
    }

    // ── /chestshop pricing ───────────────────────────────────────────────────

    private static int executePricingStatus(CommandSourceStack src) {
        var engine = com.zerog.neoessentials.shop.pricing.PricingEngine.getInstance();
        src.sendSuccess(() -> Component.literal("§6§l--- Shop Pricing Engine ---"), false);
        src.sendSuccess(() -> Component.literal("§eEnabled:  §f" + engine.isEnabled()), false);
        src.sendSuccess(() -> Component.literal("§eRules:    §f" + engine.getRuleCount()), false);
        if (!engine.isEnabled()) {
            src.sendSuccess(() -> Component.literal(
                "§7Dynamic pricing is OFF. To enable, set §eshop.pricing.enabled=true §7in config.json " +
                "and run §e/chestshop reload§7."), false);
        }
        return 1;
    }

    // ── /chestshop (help) ─────────────────────────────────────────────────────

    private static int executeHelp(CommandSourceStack src) {
        src.sendSuccess(() -> Component.literal("§6§l=== ChestShop Commands ==="), false);
        src.sendSuccess(() -> Component.literal("§e/chestshop list §7- List your shops"), false);
        src.sendSuccess(() -> Component.literal("§e/chestshop info §7- Info on looked-at shop"), false);
        src.sendSuccess(() -> Component.literal("§e/chestshop setprice <buy|sell|both> <price> §7- Set price on looked-at shop"), false);
        src.sendSuccess(() -> Component.literal("§e/chestshop stats §7- Your shop statistics"), false);
        src.sendSuccess(() -> Component.literal("§e/chestshop limit §7- Show your shop limit"), false);
        src.sendSuccess(() -> Component.literal("§e/chestshop pricing §7- Show dynamic pricing engine status"), false);
        src.sendSuccess(() -> Component.literal("§e/chestshop hologram enable §7- Enable hologram on looked-at shop"), false);
        src.sendSuccess(() -> Component.literal("§e/chestshop hologram disable §7- Remove hologram from looked-at shop"), false);
        src.sendSuccess(() -> Component.literal("§e/chestshop hologram move <x> <y> <z> §7- Move hologram (offset from sign, max ±4.5 blocks)"), false);
        src.sendSuccess(() -> Component.literal("§e/chestshop export §7- Admin: export shops to CSV"), false);
        src.sendSuccess(() -> Component.literal("§e/chestshop import [create] §7- Admin: import from CSV"), false);
        src.sendSuccess(() -> Component.literal("§e/chestshop convert §7- Register looked-at sign as shop"), false);
        src.sendSuccess(() -> Component.literal("§e/chestshop remove <x> <y> <z> §7- Admin: remove shop"), false);
        src.sendSuccess(() -> Component.literal("§e/chestshop reload §7- Admin: reload shop data"), false);
        src.sendSuccess(() -> Component.literal("§7Signs: [Name] / [Qty] / [B buy:S sell] / [item or ?]"), false);
        return 1;
    }
}

