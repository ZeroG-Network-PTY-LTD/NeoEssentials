package com.zerog.neoessentials.crates.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.zerog.neoessentials.api.permissions.PermissionAPI;
import com.zerog.neoessentials.config.ConfigManager;
import com.zerog.neoessentials.crates.CrateAnimation;
import com.zerog.neoessentials.crates.CrateDefinition;
import com.zerog.neoessentials.crates.CrateKeyManager;
import com.zerog.neoessentials.crates.CrateManager;
import com.zerog.neoessentials.crates.CrateReward;
import com.zerog.neoessentials.crates.gui.CrateOpeningMenu;
import com.zerog.neoessentials.crates.gui.CratePreviewMenu;
import com.zerog.neoessentials.util.MessageUtil;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.HitResult;

import java.util.UUID;

public class CrateCommands {
    private static final String PERM_OPEN = "neoessentials.crate.open";
    private static final String PERM_PREVIEW = "neoessentials.crate.preview";
    private static final String PERM_ADMIN = "neoessentials.crate.admin";

    private static final SuggestionProvider<CommandSourceStack> CRATE_SUGGESTIONS = (ctx, builder) ->
        SharedSuggestionProvider.suggest(CrateManager.getInstance().getAllCrates().keySet(), builder);

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        if (!ConfigManager.isCratesModuleEnabled()) return;

        dispatcher.register(Commands.literal("crate")
            .requires(src -> {
                var p = src.getPlayer();
                return p == null || PermissionAPI.hasPermission(p.getUUID(), PERM_OPEN);
            })
            .executes(ctx -> listCrates(ctx.getSource()))
            .then(Commands.literal("list").executes(ctx -> listCrates(ctx.getSource())))
            .then(Commands.literal("open")
                .then(Commands.argument("crate", StringArgumentType.word()).suggests(CRATE_SUGGESTIONS)
                    .executes(ctx -> openCrate(ctx.getSource(), StringArgumentType.getString(ctx, "crate")))))
            .then(Commands.literal("preview")
                .requires(src -> {
                    var p = src.getPlayer();
                    return p == null || PermissionAPI.hasPermission(p.getUUID(), PERM_PREVIEW);
                })
                .then(Commands.argument("crate", StringArgumentType.word()).suggests(CRATE_SUGGESTIONS)
                    .executes(ctx -> previewCrate(ctx.getSource(), StringArgumentType.getString(ctx, "crate")))))
            .then(Commands.literal("keys")
                .executes(ctx -> showKeys(ctx.getSource(), null))
                .then(Commands.argument("player", StringArgumentType.word())
                    .suggests((ctx, b) -> SharedSuggestionProvider.suggest(ctx.getSource().getServer().getPlayerNames(), b))
                    .executes(ctx -> showKeys(ctx.getSource(), StringArgumentType.getString(ctx, "player")))))
            .then(Commands.literal("key")
                .requires(adminCheck())
                .then(Commands.literal("give")
                    .then(Commands.argument("player", StringArgumentType.word())
                        .suggests((ctx, b) -> SharedSuggestionProvider.suggest(ctx.getSource().getServer().getPlayerNames(), b))
                        .then(Commands.argument("crate", StringArgumentType.word()).suggests(CRATE_SUGGESTIONS)
                            .then(Commands.argument("amount", IntegerArgumentType.integer(1))
                                .executes(ctx -> giveKeys(ctx.getSource(),
                                    StringArgumentType.getString(ctx, "player"),
                                    StringArgumentType.getString(ctx, "crate"),
                                    IntegerArgumentType.getInteger(ctx, "amount")))))))
                .then(Commands.literal("take")
                    .then(Commands.argument("player", StringArgumentType.word())
                        .suggests((ctx, b) -> SharedSuggestionProvider.suggest(ctx.getSource().getServer().getPlayerNames(), b))
                        .then(Commands.argument("crate", StringArgumentType.word()).suggests(CRATE_SUGGESTIONS)
                            .then(Commands.argument("amount", IntegerArgumentType.integer(1))
                                .executes(ctx -> takeKeys(ctx.getSource(),
                                    StringArgumentType.getString(ctx, "player"),
                                    StringArgumentType.getString(ctx, "crate"),
                                    IntegerArgumentType.getInteger(ctx, "amount"))))))))
            .then(Commands.literal("admin")
                .requires(adminCheck())
                .then(Commands.literal("create")
                    .then(Commands.argument("id", StringArgumentType.word())
                        .then(Commands.argument("displayName", StringArgumentType.greedyString())
                            .executes(ctx -> createCrate(ctx.getSource(),
                                StringArgumentType.getString(ctx, "id"),
                                StringArgumentType.getString(ctx, "displayName"))))))
                .then(Commands.literal("delete")
                    .then(Commands.argument("crate", StringArgumentType.word()).suggests(CRATE_SUGGESTIONS)
                        .executes(ctx -> deleteCrate(ctx.getSource(), StringArgumentType.getString(ctx, "crate")))))
                .then(Commands.literal("addreward")
                    .then(Commands.argument("crate", StringArgumentType.word()).suggests(CRATE_SUGGESTIONS)
                        .then(Commands.argument("weight", DoubleArgumentType.doubleArg(0.01))
                            .executes(ctx -> addReward(ctx.getSource(),
                                StringArgumentType.getString(ctx, "crate"),
                                DoubleArgumentType.getDouble(ctx, "weight"))))))
                .then(Commands.literal("setanimation")
                    .then(Commands.argument("crate", StringArgumentType.word()).suggests(CRATE_SUGGESTIONS)
                        .then(Commands.argument("animation", StringArgumentType.word())
                            .executes(ctx -> setAnimation(ctx.getSource(),
                                StringArgumentType.getString(ctx, "crate"),
                                StringArgumentType.getString(ctx, "animation"))))))
                .then(Commands.literal("setkey")
                    .then(Commands.argument("crate", StringArgumentType.word()).suggests(CRATE_SUGGESTIONS)
                        .executes(ctx -> setKeyItem(ctx.getSource(), StringArgumentType.getString(ctx, "crate")))))
                .then(Commands.literal("setblock")
                    .then(Commands.argument("crate", StringArgumentType.word()).suggests(CRATE_SUGGESTIONS)
                        .executes(ctx -> setBlock(ctx.getSource(), StringArgumentType.getString(ctx, "crate")))))
                .then(Commands.literal("removeblock").executes(ctx -> removeBlock(ctx.getSource())))
                .then(Commands.literal("reload").executes(ctx -> {
                    ConfigManager.getInstance().clearCache();
                    CrateManager.getInstance().load();
                    com.zerog.neoessentials.crates.CrateHologramManager.cleanOrphanedCrateHolograms();
                    ctx.getSource().sendSuccess(() -> MessageUtil.success("commands.neoessentials.crate.reloaded"), false);
                    return 1;
                }))
            )
        );
    }

    private static java.util.function.Predicate<CommandSourceStack> adminCheck() {
        return src -> {
            var p = src.getPlayer();
            return p == null || PermissionAPI.hasPermission(p.getUUID(), PERM_ADMIN);
        };
    }

    private static int listCrates(CommandSourceStack source) {
        var ids = CrateManager.getInstance().getAllCrates().keySet();
        String msg = ids.isEmpty() ? "§7(none)" : "§e" + String.join("§7, §e", ids);
        source.sendSuccess(() -> MessageUtil.component("commands.neoessentials.crate.list", msg), false);
        return 1;
    }

    private static int openCrate(CommandSourceStack source, String crateId) {
        CrateDefinition crate = CrateManager.getInstance().getCrate(crateId);
        if (crate == null) {
            source.sendFailure(MessageUtil.error("commands.neoessentials.crate.not_found", crateId));
            return 0;
        }
        if (!(source.getEntity() instanceof ServerPlayer player)) {
            source.sendFailure(MessageUtil.error("commands.neoessentials.general.player_only"));
            return 0;
        }
        if (!CrateManager.getInstance().hasAnyReward(crate)) {
            source.sendFailure(MessageUtil.error("commands.neoessentials.crate.no_rewards", crate.displayName));
            return 0;
        }
        if (CrateKeyManager.getInstance().getKeys(player.getUUID(), crate.id) <= 0) {
            source.sendFailure(MessageUtil.error("commands.neoessentials.crate.no_keys", crate.displayName));
            return 0;
        }
        CrateReward won = CrateManager.getInstance().tryConsumeKeyAndPick(player, crate, null);
        if (won == null) {
            source.sendFailure(MessageUtil.error("commands.neoessentials.crate.no_keys", crate.displayName));
            return 0;
        }
        CrateOpeningMenu.open(player, crate, won);
        return 1;
    }

    private static int previewCrate(CommandSourceStack source, String crateId) {
        CrateDefinition crate = CrateManager.getInstance().getCrate(crateId);
        if (crate == null) {
            source.sendFailure(MessageUtil.error("commands.neoessentials.crate.not_found", crateId));
            return 0;
        }
        if (!(source.getEntity() instanceof ServerPlayer player)) {
            source.sendFailure(MessageUtil.error("commands.neoessentials.general.player_only"));
            return 0;
        }
        CratePreviewMenu.open(player, crate);
        return 1;
    }

    private static int showKeys(CommandSourceStack source, String playerName) {
        UUID target;
        String displayName;
        if (playerName != null) {
            var online = source.getServer().getPlayerList().getPlayerByName(playerName);
            if (online == null) {
                source.sendFailure(MessageUtil.error("commands.neoessentials.general.player_not_found", playerName));
                return 0;
            }
            target = online.getUUID();
            displayName = playerName;
        } else if (source.getEntity() instanceof ServerPlayer p) {
            target = p.getUUID();
            displayName = p.getName().getString();
        } else {
            source.sendFailure(MessageUtil.error("commands.neoessentials.general.player_only"));
            return 0;
        }

        StringBuilder sb = new StringBuilder();
        for (String id : CrateManager.getInstance().getAllCrates().keySet()) {
            int keys = CrateKeyManager.getInstance().getKeys(target, id);
            if (keys > 0) {
                if (!sb.isEmpty()) sb.append("§7, ");
                sb.append("§e").append(id).append(" §7x§f").append(keys);
            }
        }
        String result = sb.isEmpty() ? "§7(none)" : sb.toString();
        final String fDisplayName = displayName;
        source.sendSuccess(() -> MessageUtil.component("commands.neoessentials.crate.keys_list", fDisplayName, result), false);
        return 1;
    }

    private static int giveKeys(CommandSourceStack source, String playerName, String crateId, int amount) {
        CrateDefinition crate = CrateManager.getInstance().getCrate(crateId);
        if (crate == null) {
            source.sendFailure(MessageUtil.error("commands.neoessentials.crate.not_found", crateId));
            return 0;
        }
        var online = source.getServer().getPlayerList().getPlayerByName(playerName);
        if (online == null) {
            source.sendFailure(MessageUtil.error("commands.neoessentials.general.player_not_found", playerName));
            return 0;
        }
        CrateKeyManager.getInstance().addKeys(online.getUUID(), crate.id, amount);
        source.sendSuccess(() -> MessageUtil.success("commands.neoessentials.crate.keys_given", amount, crate.displayName, playerName), true);
        return 1;
    }

    private static int takeKeys(CommandSourceStack source, String playerName, String crateId, int amount) {
        CrateDefinition crate = CrateManager.getInstance().getCrate(crateId);
        if (crate == null) {
            source.sendFailure(MessageUtil.error("commands.neoessentials.crate.not_found", crateId));
            return 0;
        }
        var online = source.getServer().getPlayerList().getPlayerByName(playerName);
        if (online == null) {
            source.sendFailure(MessageUtil.error("commands.neoessentials.general.player_not_found", playerName));
            return 0;
        }
        CrateKeyManager.getInstance().removeKeys(online.getUUID(), crate.id, amount);
        source.sendSuccess(() -> MessageUtil.success("commands.neoessentials.crate.keys_taken", amount, crate.displayName, playerName), true);
        return 1;
    }

    private static int createCrate(CommandSourceStack source, String id, String displayName) {
        if (CrateManager.getInstance().getCrate(id) != null) {
            source.sendFailure(MessageUtil.error("commands.neoessentials.crate.already_exists", id));
            return 0;
        }
        CrateDefinition def = new CrateDefinition();
        def.id = id;
        def.displayName = displayName;
        CrateManager.getInstance().saveCrate(def);
        source.sendSuccess(() -> MessageUtil.success("commands.neoessentials.crate.created", id), false);
        return 1;
    }

    private static int deleteCrate(CommandSourceStack source, String id) {
        if (!CrateManager.getInstance().deleteCrate(id)) {
            source.sendFailure(MessageUtil.error("commands.neoessentials.crate.not_found", id));
            return 0;
        }
        // Any physical blocks pointing at this now-gone crate lose their hologram too —
        // the block-position mapping itself is left alone (matches how a deleted crate's
        // blocks already just silently stop responding to right-click) but a stale
        // hologram floating over nothing would be confusing.
        com.zerog.neoessentials.crates.CrateHologramManager.cleanOrphanedCrateHolograms();
        source.sendSuccess(() -> MessageUtil.success("commands.neoessentials.crate.deleted", id), false);
        return 1;
    }

    private static int addReward(CommandSourceStack source, String crateId, double weight) {
        CrateDefinition crate = CrateManager.getInstance().getCrate(crateId);
        if (crate == null) {
            source.sendFailure(MessageUtil.error("commands.neoessentials.crate.not_found", crateId));
            return 0;
        }
        if (!(source.getEntity() instanceof ServerPlayer player)) {
            source.sendFailure(MessageUtil.error("commands.neoessentials.general.player_only"));
            return 0;
        }
        ItemStack held = player.getMainHandItem();
        if (held.isEmpty()) {
            source.sendFailure(MessageUtil.error("commands.neoessentials.crate.hold_item"));
            return 0;
        }
        CrateReward reward = new CrateReward();
        reward.id = java.util.UUID.randomUUID().toString().substring(0, 8);
        reward.weight = weight;
        reward.item = held.copy();
        crate.rewards.add(reward);
        CrateManager.getInstance().saveCrate(crate);
        source.sendSuccess(() -> MessageUtil.success("commands.neoessentials.crate.reward_added", crate.displayName), false);
        return 1;
    }

    private static int setAnimation(CommandSourceStack source, String crateId, String animation) {
        CrateDefinition crate = CrateManager.getInstance().getCrate(crateId);
        if (crate == null) {
            source.sendFailure(MessageUtil.error("commands.neoessentials.crate.not_found", crateId));
            return 0;
        }
        crate.animation = CrateAnimation.parse(animation);
        CrateManager.getInstance().saveCrate(crate);
        source.sendSuccess(() -> MessageUtil.success("commands.neoessentials.crate.animation_set", crate.displayName, crate.animation.name().toLowerCase()), false);
        return 1;
    }

    private static int setKeyItem(CommandSourceStack source, String crateId) {
        CrateDefinition crate = CrateManager.getInstance().getCrate(crateId);
        if (crate == null) {
            source.sendFailure(MessageUtil.error("commands.neoessentials.crate.not_found", crateId));
            return 0;
        }
        if (!(source.getEntity() instanceof ServerPlayer player)) {
            source.sendFailure(MessageUtil.error("commands.neoessentials.general.player_only"));
            return 0;
        }
        ItemStack held = player.getMainHandItem();
        if (held.isEmpty()) {
            source.sendFailure(MessageUtil.error("commands.neoessentials.crate.hold_item"));
            return 0;
        }
        crate.keyItem = held.copy();
        CrateManager.getInstance().saveCrate(crate);
        source.sendSuccess(() -> MessageUtil.success("commands.neoessentials.crate.key_item_set", crate.displayName), false);
        return 1;
    }

    private static int setBlock(CommandSourceStack source, String crateId) {
        CrateDefinition crate = CrateManager.getInstance().getCrate(crateId);
        if (crate == null) {
            source.sendFailure(MessageUtil.error("commands.neoessentials.crate.not_found", crateId));
            return 0;
        }
        if (!(source.getEntity() instanceof ServerPlayer player) || !(player.level() instanceof ServerLevel level)) {
            source.sendFailure(MessageUtil.error("commands.neoessentials.general.player_only"));
            return 0;
        }
        BlockPos pos = lookingAtBlock(player);
        if (pos == null) {
            source.sendFailure(MessageUtil.error("commands.neoessentials.crate.no_block_in_sight"));
            return 0;
        }
        CrateManager.getInstance().setBlock(level, pos, crate.id);
        com.zerog.neoessentials.crates.CrateHologramManager.createOrUpdateCrateHologram(crate, level, pos);
        source.sendSuccess(() -> MessageUtil.success("commands.neoessentials.crate.block_set", crate.displayName), false);
        return 1;
    }

    private static int removeBlock(CommandSourceStack source) {
        if (!(source.getEntity() instanceof ServerPlayer player) || !(player.level() instanceof ServerLevel level)) {
            source.sendFailure(MessageUtil.error("commands.neoessentials.general.player_only"));
            return 0;
        }
        BlockPos pos = lookingAtBlock(player);
        if (pos == null || !CrateManager.getInstance().removeBlock(level, pos)) {
            source.sendFailure(MessageUtil.error("commands.neoessentials.crate.no_block_in_sight"));
            return 0;
        }
        com.zerog.neoessentials.crates.CrateHologramManager.deleteCrateHologram(level, pos);
        source.sendSuccess(() -> MessageUtil.success("commands.neoessentials.crate.block_removed"), false);
        return 1;
    }

    private static BlockPos lookingAtBlock(ServerPlayer player) {
        var hit = player.pick(6.0, 0f, false);
        if (hit.getType() != HitResult.Type.BLOCK) return null;
        return ((net.minecraft.world.phys.BlockHitResult) hit).getBlockPos();
    }
}
