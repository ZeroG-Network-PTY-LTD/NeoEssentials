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
import com.zerog.neoessentials.util.MessageUtil;
import com.zerog.neoessentials.util.ResourceUtil;
import com.zerog.neoessentials.logging.LogCategory;
import com.zerog.neoessentials.logging.NeoLog;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
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
    private static final org.slf4j.Logger LOGGER = org.slf4j.LoggerFactory.getLogger(ShopCommand.class);

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        if (!com.zerog.neoessentials.config.ConfigManager.isShopModuleEnabled()) {
            return;
        }

        boolean chestshopEnabled = com.zerog.neoessentials.config.ConfigManager.getInstance().isCommandEnabled("chestshop");
        boolean cshopEnabled = com.zerog.neoessentials.config.ConfigManager.getInstance().isCommandEnabled("cshop");

        if (!chestshopEnabled && !cshopEnabled) {
            return;
        }

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
                .executes(ctx -> executeRemoveLookAt(ctx.getSource()))
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

        if (chestshopEnabled) {
            dispatcher.register(node);
        }

        // Aliases
        if (cshopEnabled && chestshopEnabled) {
            dispatcher.register(Commands.literal("cshop").redirect(dispatcher.getRoot().getChild("chestshop")));
        }
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
                    src.sendFailure(MessageUtil.component("commands.neoessentials.chestshop.no_permission_list_others"));
                    return 0;
                }
                // Resolve UUID by online player name
                var server = src.getServer();
                ServerPlayer target = server.getPlayerList().getPlayerByName(targetName);
                if (target == null) {
                    src.sendFailure(MessageUtil.component("commands.neoessentials.chestshop.player_not_found", targetName));
                    return 0;
                }
                uuid = target.getUUID();
                displayName = targetName;
            }

            List<ShopData> shops = ShopManager.getInstance().getShopsByOwner(uuid);
            String finalDisplayName = displayName;
            src.sendSuccess(() -> MessageUtil.component("commands.neoessentials.chestshop.list_header",
                finalDisplayName, shops.size()), false);
            if (shops.isEmpty()) {
                src.sendSuccess(() -> MessageUtil.component("commands.neoessentials.chestshop.list_empty"), false);
            } else {
                String currency = EconomyManager.getInstance().getCurrencySymbol();
                for (ShopData s : shops) {
                    src.sendSuccess(() -> MessageUtil.component("commands.neoessentials.chestshop.list_entry",
                        s.signDimension.replace("minecraft:", ""),
                        s.signX, s.signY, s.signZ,
                        s.quantity,
                        s.itemId.replace("minecraft:", ""),
                        s.buyPrice  != null ? currency + s.buyPrice.toPlainString()  : "§7—",
                        s.sellPrice != null ? currency + s.sellPrice.toPlainString() : "§7—"
                    ), false);
                }
            }
            return shops.size();
        } catch (Exception e) {
            src.sendFailure(MessageUtil.component("commands.neoessentials.chestshop.error", e.getMessage()));
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
                src.sendFailure(MessageUtil.component("commands.neoessentials.chestshop.look_at_sign"));
                return 0;
            }
            BlockPos pos = ((BlockHitResult) hit).getBlockPos();
            ServerLevel level = com.zerog.neoessentials.util.LevelCompat.of(player);
            String dimension = level.dimension().location().toString();

            ShopData shop = ShopManager.getInstance().getShopBySign(dimension, pos);
            if (shop == null) {
                src.sendFailure(MessageUtil.component("commands.neoessentials.chestshop.no_shop_at_sign"));
                return 0;
            }

            String currency = EconomyManager.getInstance().getCurrencySymbol();
            src.sendSuccess(() -> MessageUtil.component("commands.neoessentials.chestshop.info_header"), false);
            src.sendSuccess(() -> MessageUtil.component("commands.neoessentials.chestshop.info_owner",
                shop.ownerName, shop.isAdminShop() ? " §2[Admin]" : ""), false);
            src.sendSuccess(() -> MessageUtil.component("commands.neoessentials.chestshop.info_item",
                shop.quantity, shop.itemId.replace("minecraft:", "")), false);
            if (shop.buyPrice  != null) src.sendSuccess(() -> MessageUtil.component(
                "commands.neoessentials.chestshop.info_buy", currency, shop.buyPrice.toPlainString()), false);
            if (shop.sellPrice != null) src.sendSuccess(() -> MessageUtil.component(
                "commands.neoessentials.chestshop.info_sell", currency, shop.sellPrice.toPlainString()), false);
            src.sendSuccess(() -> MessageUtil.component("commands.neoessentials.chestshop.info_sign",
                shop.signX, shop.signY, shop.signZ), false);
            return 1;
        } catch (Exception e) {
            src.sendFailure(MessageUtil.component("commands.neoessentials.chestshop.error", e.getMessage()));
            return 0;
        }
    }

    // ── /chestshop convert ────────────────────────────────────────────────────
    /** Register the sign the player is looking at as a shop (for pre-existing signs). */
    private static int executeConvert(CommandSourceStack src) {
        try {
            ServerPlayer player = src.getPlayerOrException();
            if (!PermissionAPI.hasPermission(player.getUUID(), "neoessentials.shop.create")) {
                src.sendFailure(MessageUtil.component("commands.neoessentials.chestshop.no_permission"));
                return 0;
            }
            HitResult hit = player.pick(5.0, 0.0f, false);
            if (hit.getType() != HitResult.Type.BLOCK) {
                src.sendFailure(MessageUtil.component("commands.neoessentials.chestshop.look_at_generic_sign"));
                return 0;
            }
            BlockPos pos = ((BlockHitResult) hit).getBlockPos();
            ServerLevel level = com.zerog.neoessentials.util.LevelCompat.of(player);
            BlockEntity be = level.getBlockEntity(pos);
            if (!(be instanceof SignBlockEntity sign)) {
                src.sendFailure(MessageUtil.component("commands.neoessentials.chestshop.not_a_sign"));
                return 0;
            }
            String dimension = level.dimension().location().toString();
            String[] lines = ShopSignHandler.readSignLines(sign);
            ShopSignHandler.tryRegisterShop(player, lines, pos, dimension, level);
            return 1;
        } catch (Exception e) {
            src.sendFailure(MessageUtil.component("commands.neoessentials.chestshop.error", e.getMessage()));
            return 0;
        }
    }

    // ── /chestshop remove <x> <y> <z> ────────────────────────────────────────

    /**
     * {@code /chestshop remove} with no coordinates — removes whichever shop the player is
     * currently looking at (sign or linked chest), if they own it or hold
     * {@code neoessentials.shop.admin.remove}/OP 3. The coordinate-based {@link #executeRemove}
     * variant below remains admin-only (it doesn't require looking at the shop at all, so an
     * owner check there wouldn't be meaningful).
     */
    @SuppressWarnings("resource")
    private static int executeRemoveLookAt(CommandSourceStack src) {
        ServerPlayer player;
        try {
            player = src.getPlayerOrException();
        } catch (Exception e) {
            src.sendFailure(MessageUtil.component("commands.neoessentials.chestshop.remove_player_only"));
            return 0;
        }

        ShopData shop = getShopFromLookAt(player);
        String dimension = com.zerog.neoessentials.util.LevelCompat.of(player).dimension().location().toString();
        if (shop == null) {
            // Fall back to the linked chest, in case the player is looking at that instead of the sign
            HitResult hit = player.pick(5.0, 0.0f, false);
            if (hit.getType() == HitResult.Type.BLOCK) {
                shop = ShopManager.getInstance().getShopByChest(dimension, ((BlockHitResult) hit).getBlockPos());
            }
        }
        if (shop == null) {
            src.sendFailure(MessageUtil.component("commands.neoessentials.chestshop.remove_look_required"));
            return 0;
        }
        if (!isShopOwnerOrAdmin(player, shop)) {
            src.sendFailure(MessageUtil.component("commands.neoessentials.chestshop.remove_no_permission"));
            return 0;
        }

        BlockPos signPos = shop.getSignPos();
        ShopData removed = ShopManager.getInstance().removeShop(dimension, signPos);
        if (removed == null) {
            src.sendFailure(MessageUtil.component("commands.neoessentials.chestshop.remove_failed"));
            return 0;
        }
        src.sendSuccess(() -> MessageUtil.component("commands.neoessentials.chestshop.remove_success",
            removed.ownerName, signPos.getX(), signPos.getY(), signPos.getZ()), true);
        return 1;
    }

    @SuppressWarnings("resource") // ServerLevel is not AutoCloseable; IntelliJ false positive
    private static int executeRemove(CommandSourceStack src, int x, int y, int z) {
        boolean isAdmin = src.hasPermission(3) ||
            (src.getEntity() != null &&
             PermissionAPI.hasPermission(src.getEntity().getUUID(), "neoessentials.shop.admin.remove"));
        if (!isAdmin) {
            src.sendFailure(MessageUtil.component("commands.neoessentials.chestshop.no_permission"));
            return 0;
        }
        try {
            ServerPlayer player = src.getPlayerOrException();
            String dimension = com.zerog.neoessentials.util.LevelCompat.of(player).dimension().location().toString();
            BlockPos pos = new BlockPos(x, y, z);
            ShopData removed = ShopManager.getInstance().removeShop(dimension, pos);
            if (removed == null) {
                src.sendFailure(MessageUtil.component("commands.neoessentials.chestshop.remove_not_found_at", x, y, z));
                return 0;
            }
            src.sendSuccess(() -> MessageUtil.component("commands.neoessentials.chestshop.remove_success",
                removed.ownerName, x, y, z), true);
            return 1;
        } catch (Exception e) {
            src.sendFailure(MessageUtil.component("commands.neoessentials.chestshop.error", e.getMessage()));
            return 0;
        }
    }

    // ── /chestshop reload ─────────────────────────────────────────────────────

    private static int executeReload(CommandSourceStack src) {
        ShopManager.getInstance().reload();
        com.zerog.neoessentials.shop.pricing.PricingEngine.getInstance().loadConfig();
        src.sendSuccess(() -> MessageUtil.component("commands.neoessentials.chestshop.reload_success",
            ShopManager.getInstance().getShopCount()), true);
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
                src.sendFailure(MessageUtil.component("commands.neoessentials.chestshop.no_permission"));
                return 0;
            }
            HitResult hit = player.pick(5.0, 0.0f, false);
            if (hit.getType() != HitResult.Type.BLOCK) {
                src.sendFailure(MessageUtil.component("commands.neoessentials.chestshop.look_at_sign"));
                return 0;
            }
            BlockPos pos = ((BlockHitResult) hit).getBlockPos();
            ServerLevel level = com.zerog.neoessentials.util.LevelCompat.of(player);
            String dimension = level.dimension().location().toString();
            ShopData shop = ShopManager.getInstance().getShopBySign(dimension, pos);
            if (shop == null) {
                src.sendFailure(MessageUtil.component("commands.neoessentials.chestshop.no_shop_at_sign"));
                return 0;
            }
            // Owner or admin can set price
            boolean isAdmin = src.hasPermission(3) ||
                    PermissionAPI.hasPermission(player.getUUID(), "neoessentials.shop.admin.setprice");
            if (!isAdmin && (shop.ownerUUID == null || !shop.ownerUUID.equals(player.getUUID()))) {
                src.sendFailure(MessageUtil.component("commands.neoessentials.chestshop.setprice_not_owner"));
                return 0;
            }
            BigDecimal bd = BigDecimal.valueOf(price);
            switch (type.toLowerCase()) {
                case "buy"  -> shop.buyPrice  = bd;
                case "sell" -> shop.sellPrice = bd;
                case "both" -> { shop.buyPrice = bd; shop.sellPrice = bd; }
                default -> {
                    src.sendFailure(MessageUtil.component("commands.neoessentials.chestshop.setprice_invalid_type"));
                    return 0;
                }
            }
            ShopManager.getInstance().registerShop(shop);
            com.zerog.neoessentials.shop.handlers.ShopSignHandler.writeSignLines(level, pos,
                    com.zerog.neoessentials.shop.ShopParser.formatSignLines(shop));
            src.sendSuccess(() -> MessageUtil.component("commands.neoessentials.chestshop.setprice_success",
                    type, EconomyManager.getInstance().getCurrencySymbol(), bd.toPlainString()), false);
            return 1;
        } catch (Exception e) {
            src.sendFailure(MessageUtil.component("commands.neoessentials.chestshop.error", e.getMessage()));
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
            src.sendSuccess(() -> MessageUtil.component("commands.neoessentials.chestshop.stats_header"), false);
            src.sendSuccess(() -> MessageUtil.component("commands.neoessentials.chestshop.stats_total",
                    myShops.size(), playerCount, adminCount), false);
            src.sendSuccess(() -> MessageUtil.component("commands.neoessentials.chestshop.stats_sales", totalSales), false);
            if (!myShops.isEmpty()) {
                myShops.stream().max(Comparator.comparingLong(s -> s.totalSalesCount))
                        .ifPresent(top -> src.sendSuccess(() -> MessageUtil.component(
                                "commands.neoessentials.chestshop.stats_top_seller",
                                top.itemId.replace("minecraft:", ""), top.totalSalesCount), false));
            }
            return 1;
        } catch (Exception e) {
            src.sendFailure(MessageUtil.component("commands.neoessentials.chestshop.error", e.getMessage()));
            return 0;
        }
    }

    // ── /chestshop limit ──────────────────────────────────────────────────────

    private static int executeLimit(CommandSourceStack src) {
        try {
            ServerPlayer player = src.getPlayerOrException();
            int used  = ShopManager.getInstance().getShopsByOwner(player.getUUID()).size();
            int max   = getMaxShopsPerPlayer();
            src.sendSuccess(() -> MessageUtil.component("commands.neoessentials.chestshop.limit_status",
                    used, max < 0 ? "unlimited" : String.valueOf(max)), false);
            return 1;
        } catch (Exception e) {
            src.sendFailure(MessageUtil.component("commands.neoessentials.chestshop.error", e.getMessage()));
            return 0;
        }
    }

    // ── /chestshop export ─────────────────────────────────────────────────────

    private static int executeExport(CommandSourceStack src) {
        boolean isAdmin = src.hasPermission(3) ||
                (src.getEntity() != null &&
                 PermissionAPI.hasPermission(src.getEntity().getUUID(), "neoessentials.shop.admin.csv.export"));
        if (!isAdmin) { src.sendFailure(MessageUtil.component("commands.neoessentials.chestshop.no_permission")); return 0; }
        try {
            String csv = ShopCsvSerializer.export(ShopManager.getInstance().getAllShops());
            Path file  = getCsvPath();
            Files.createDirectories(file.getParent());
            Files.writeString(file, csv);
            src.sendSuccess(() -> MessageUtil.component("commands.neoessentials.chestshop.export_success",
                    ShopManager.getInstance().getShopCount(), file.getFileName()), true);
            return 1;
        } catch (Exception e) {
            src.sendFailure(MessageUtil.component("commands.neoessentials.chestshop.export_failed", e.getMessage()));
            return 0;
        }
    }

    // ── /chestshop import [create] ────────────────────────────────────────────

    private static int executeImport(CommandSourceStack src, boolean createNew) {
        boolean isAdmin = src.hasPermission(3) ||
                (src.getEntity() != null &&
                 PermissionAPI.hasPermission(src.getEntity().getUUID(), "neoessentials.shop.admin.csv.import"));
        if (!isAdmin) { src.sendFailure(MessageUtil.component("commands.neoessentials.chestshop.no_permission")); return 0; }
        try {
            Path file = getCsvPath();
            if (!Files.exists(file)) {
                src.sendFailure(MessageUtil.component("commands.neoessentials.chestshop.import_file_not_found", file));
                return 0;
            }
            String csv = Files.readString(file);
            var rows   = ShopCsvSerializer.importRows(csv);
            var result = ShopCsvImporter.apply(rows, createNew);
            src.sendSuccess(() -> MessageUtil.component("commands.neoessentials.chestshop.import_success", result.details()), true);
            return result.updated() + result.created();
        } catch (Exception e) {
            src.sendFailure(MessageUtil.component("commands.neoessentials.chestshop.import_failed", e.getMessage()));
            return 0;
        }
    }

    // ── /chestshop hologram enable ────────────────────────────────────────────

    /** Enable hologram on the shop sign the player is currently looking at. */
    private static int executeHologramEnable(CommandSourceStack src) {
        try {
            ServerPlayer player = src.getPlayerOrException();
            ShopData shop = getShopFromLookAt(player);
            if (shop == null) { src.sendFailure(MessageUtil.component("commands.neoessentials.chestshop.look_at_sign")); return 0; }
            if (!isShopOwner(player, shop)) {
                src.sendFailure(MessageUtil.component("commands.neoessentials.chestshop.hologram_owner_only_enable"));
                return 0;
            }
            if (shop.hologramEnabled) {
                src.sendSuccess(() -> MessageUtil.component("commands.neoessentials.chestshop.hologram_already_enabled"), false);
                return 1;
            }
            ShopHologramManager.enableShopHologram(shop);
            src.sendSuccess(() -> MessageUtil.component("commands.neoessentials.chestshop.hologram_enabled"), false);
            return 1;
        } catch (Exception e) {
            src.sendFailure(MessageUtil.component("commands.neoessentials.chestshop.error", e.getMessage()));
            return 0;
        }
    }

    /** Enable hologram for the shop at the given sign coordinates (used by clickable chat message). */
    private static int executeHologramEnablePos(CommandSourceStack src, int x, int y, int z) {
        try {
            ServerPlayer player = src.getPlayerOrException();
            String dimension = com.zerog.neoessentials.util.LevelCompat.of(player).dimension().location().toString();
            ShopData shop = ShopManager.getInstance().getShopBySign(dimension, new BlockPos(x, y, z));
            if (shop == null) { src.sendFailure(MessageUtil.component("commands.neoessentials.chestshop.hologram_no_shop_at_pos")); return 0; }
            if (!isShopOwner(player, shop)) {
                src.sendFailure(MessageUtil.component("commands.neoessentials.chestshop.hologram_owner_only_enable"));
                return 0;
            }
            if (shop.hologramEnabled) {
                src.sendSuccess(() -> MessageUtil.component("commands.neoessentials.chestshop.hologram_already_enabled"), false);
                return 1;
            }
            ShopHologramManager.enableShopHologram(shop);
            src.sendSuccess(() -> MessageUtil.component("commands.neoessentials.chestshop.hologram_enabled"), false);
            return 1;
        } catch (Exception e) {
            src.sendFailure(MessageUtil.component("commands.neoessentials.chestshop.error", e.getMessage()));
            return 0;
        }
    }

    // ── /chestshop hologram disable ───────────────────────────────────────────

    private static int executeHologramDisable(CommandSourceStack src) {
        try {
            ServerPlayer player = src.getPlayerOrException();
            ShopData shop = getShopFromLookAt(player);
            if (shop == null) { src.sendFailure(MessageUtil.component("commands.neoessentials.chestshop.look_at_sign")); return 0; }
            if (!isShopOwner(player, shop)) {
                src.sendFailure(MessageUtil.component("commands.neoessentials.chestshop.hologram_owner_only_disable"));
                return 0;
            }
            if (!shop.hologramEnabled) {
                src.sendSuccess(() -> MessageUtil.component("commands.neoessentials.chestshop.hologram_already_disabled"), false);
                return 1;
            }
            ShopHologramManager.disableShopHologram(shop);
            src.sendSuccess(() -> MessageUtil.component("commands.neoessentials.chestshop.hologram_removed"), false);
            return 1;
        } catch (Exception e) {
            src.sendFailure(MessageUtil.component("commands.neoessentials.chestshop.error", e.getMessage()));
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
            if (shop == null) { src.sendFailure(MessageUtil.component("commands.neoessentials.chestshop.look_at_sign")); return 0; }
            if (!isShopOwner(player, shop)) {
                src.sendFailure(MessageUtil.component("commands.neoessentials.chestshop.hologram_owner_only_move"));
                return 0;
            }
            if (!shop.hologramEnabled) {
                src.sendFailure(MessageUtil.component("commands.neoessentials.chestshop.hologram_not_enabled"));
                return 0;
            }
            boolean moved = ShopHologramManager.moveShopHologram(shop, ox, oy, oz);
            if (!moved) {
                src.sendFailure(MessageUtil.component("commands.neoessentials.chestshop.hologram_move_failed"));
                return 0;
            }
            double cx = shop.hologramOffsetX, cy = shop.hologramOffsetY, cz = shop.hologramOffsetZ;
            src.sendSuccess(() -> MessageUtil.component("commands.neoessentials.chestshop.hologram_moved",
                String.format("%.2f", cx), String.format("%.2f", cy), String.format("%.2f", cz)), false);
            return 1;
        } catch (Exception e) {
            src.sendFailure(MessageUtil.component("commands.neoessentials.chestshop.error", e.getMessage()));
            return 0;
        }
    }

    // ── Shared helpers ────────────────────────────────────────────────────────

    @SuppressWarnings("resource")
    private static ShopData getShopFromLookAt(ServerPlayer player) {
        HitResult hit = player.pick(5.0, 0.0f, false);
        if (hit.getType() != HitResult.Type.BLOCK) return null;
        BlockPos pos = ((BlockHitResult) hit).getBlockPos();
        ServerLevel level = com.zerog.neoessentials.util.LevelCompat.of(player);
        String dimension = level.dimension().location().toString();
        return ShopManager.getInstance().getShopBySign(dimension, pos);
    }

    private static boolean isShopOwnerOrAdmin(ServerPlayer player, ShopData shop) {
        if (player.hasPermissions(3)) return true;
        if (PermissionAPI.hasPermission(player.getUUID(), "neoessentials.shop.admin.remove")) return true;
        return shop.ownerUUID != null && shop.ownerUUID.equals(player.getUUID());
    }

    /**
     * Returns {@code true} if {@code player} may manage (enable/disable/move) {@code shop}'s
     * hologram. For player shops this is owner-exclusive — general admin flags are NOT
     * checked, so server staff cannot enable or reposition holograms on behalf of players
     * (admins can still use {@code /hologram remove <id>} for moderation if needed).
     *
     * <p>Admin shops have no {@code ownerUUID} at all (it's {@code null} by design), so an
     * owner-only check would reject every player unconditionally — including whoever has
     * permission to create/manage admin shops in the first place. That meant an admin shop's
     * hologram could be offered (the creation-time opt-in prompt doesn't check shop type) but
     * never actually enabled: every click of the prompt failed with "owner only". Admin shops
     * are authorized via the same {@code neoessentials.shop.create.admin} permission used
     * elsewhere for admin-shop item assignment (see {@link com.zerog.neoessentials.shop.handlers.ShopInteractHandler}).
     */
    private static boolean isShopOwner(ServerPlayer player, ShopData shop) {
        if (shop.isAdminShop()) {
            return PermissionAPI.hasPermission(player.getUUID(), "neoessentials.shop.create.admin");
        }
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
        } catch (Exception e) {
            NeoLog.debug(LOGGER, LogCategory.GENERAL, "Failed to read shop.csvImportPath config — using default", e);
        }
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
        } catch (Exception e) {
            NeoLog.debug(LOGGER, LogCategory.GENERAL, "Failed to read shop.maxShopsPerPlayer config — using default", e);
        }
        return 10;
    }

    // ── /chestshop pricing ───────────────────────────────────────────────────

    private static int executePricingStatus(CommandSourceStack src) {
        var engine = com.zerog.neoessentials.shop.pricing.PricingEngine.getInstance();
        src.sendSuccess(() -> MessageUtil.component("commands.neoessentials.chestshop.pricing_header"), false);
        src.sendSuccess(() -> MessageUtil.component("commands.neoessentials.chestshop.pricing_enabled", engine.isEnabled()), false);
        src.sendSuccess(() -> MessageUtil.component("commands.neoessentials.chestshop.pricing_rules", engine.getRuleCount()), false);
        if (!engine.isEnabled()) {
            src.sendSuccess(() -> MessageUtil.component("commands.neoessentials.chestshop.pricing_off_hint"), false);
        }
        return 1;
    }

    // ── /chestshop (help) ─────────────────────────────────────────────────────

    private static int executeHelp(CommandSourceStack src) {
        src.sendSuccess(() -> MessageUtil.component("commands.neoessentials.chestshop.help_header"), false);
        src.sendSuccess(() -> MessageUtil.component("commands.neoessentials.chestshop.help_list"), false);
        src.sendSuccess(() -> MessageUtil.component("commands.neoessentials.chestshop.help_info"), false);
        src.sendSuccess(() -> MessageUtil.component("commands.neoessentials.chestshop.help_setprice"), false);
        src.sendSuccess(() -> MessageUtil.component("commands.neoessentials.chestshop.help_stats"), false);
        src.sendSuccess(() -> MessageUtil.component("commands.neoessentials.chestshop.help_limit"), false);
        src.sendSuccess(() -> MessageUtil.component("commands.neoessentials.chestshop.help_pricing"), false);
        src.sendSuccess(() -> MessageUtil.component("commands.neoessentials.chestshop.help_hologram_enable"), false);
        src.sendSuccess(() -> MessageUtil.component("commands.neoessentials.chestshop.help_hologram_disable"), false);
        src.sendSuccess(() -> MessageUtil.component("commands.neoessentials.chestshop.help_hologram_move"), false);
        src.sendSuccess(() -> MessageUtil.component("commands.neoessentials.chestshop.help_export"), false);
        src.sendSuccess(() -> MessageUtil.component("commands.neoessentials.chestshop.help_import"), false);
        src.sendSuccess(() -> MessageUtil.component("commands.neoessentials.chestshop.help_convert"), false);
        src.sendSuccess(() -> MessageUtil.component("commands.neoessentials.chestshop.help_remove"), false);
        src.sendSuccess(() -> MessageUtil.component("commands.neoessentials.chestshop.help_remove_coords"), false);
        src.sendSuccess(() -> MessageUtil.component("commands.neoessentials.chestshop.help_reload"), false);
        src.sendSuccess(() -> MessageUtil.component("commands.neoessentials.chestshop.help_signs_hint"), false);
        return 1;
    }
}

