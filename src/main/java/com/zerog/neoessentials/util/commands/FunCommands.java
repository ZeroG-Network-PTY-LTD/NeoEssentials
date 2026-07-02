package com.zerog.neoessentials.util.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.zerog.neoessentials.api.permissions.PermissionAPI;
import com.zerog.neoessentials.config.ConfigManager;
import com.zerog.neoessentials.util.MessageUtil;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Cat;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.item.FireworkRocketItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.FireworkExplosion;
import net.minecraft.world.item.component.Fireworks;
import net.minecraft.world.level.Level;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Random;

/**
 * Fun & miscellaneous commands ported from EssentialsX:
 * /firework [clear|power|fire|color|shape|effect] — edit/fire held firework rockets
 * /nuke [player...]                               — rain TNT on a player
 * /itemdb [item]                                  — look up item registry info
 * /potion [clear|add <effect>]                    — edit potion effects on held potion item
 * /info                                           — show server info/rules pages
 */
@SuppressWarnings("resource") // ServerLevel is not AutoCloseable — IDE false positive
public class FunCommands {
    private static final Logger LOGGER = LoggerFactory.getLogger(FunCommands.class);

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        registerFirework(dispatcher);
        registerNuke(dispatcher);
        registerAntioch(dispatcher);
        registerKittyCannon(dispatcher);
        registerBeezooka(dispatcher);
        registerItemDb(dispatcher);
        registerPotion(dispatcher);
        registerInfo(dispatcher);
        registerRest(dispatcher);
        registerBackup(dispatcher);
    }

    // ── /firework ─────────────────────────────────────────────────────────────
    // Essentials: edit or fire held firework rockets.
    // Subcommands: clear, power <n>, fire [n], color <hex[,hex]> [fade:<hex>] [shape:<shape>] [effect:<effect>]
    private static void registerFirework(CommandDispatcher<CommandSourceStack> d) {
        d.register(Commands.literal("firework")
            .requires(src -> {
                var p = src.getPlayer();
                return p == null || PermissionAPI.hasPermission(p.getUUID(), "neoessentials.firework");
            })
            // /firework clear
            .then(Commands.literal("clear")
                .executes(ctx -> executeFireworkClear(ctx))
            )
            // /firework power <0-4>
            .then(Commands.literal("power")
                .then(Commands.argument("level", IntegerArgumentType.integer(0, 127))
                    .executes(ctx -> executeFireworkPower(ctx, IntegerArgumentType.getInteger(ctx, "level")))
                )
            )
            // /firework fire [amount]
            .then(Commands.literal("fire")
                .requires(src -> {
                    var p = src.getPlayer();
                    return p == null || PermissionAPI.hasPermission(p.getUUID(), "neoessentials.firework.fire");
                })
                .executes(ctx -> executeFireworkFire(ctx, 1))
                .then(Commands.argument("amount", IntegerArgumentType.integer(1, 50))
                    .executes(ctx -> executeFireworkFire(ctx, IntegerArgumentType.getInteger(ctx, "amount")))
                )
            )
            // /firework color <hex[,hex]> [fade:<hex[,hex]>] [shape:<shape>] [effect:<effect>]
            .then(Commands.literal("color")
                .then(Commands.argument("options", StringArgumentType.greedyString())
                    .suggests((ctx, b) -> SharedSuggestionProvider.suggest(
                        List.of("FF0000", "00FF00", "0000FF", "FFFF00", "FF00FF", "00FFFF",
                            "FFFFFF", "000000", "FF8800", "8800FF"), b))
                    .executes(ctx -> executeFireworkColor(ctx, StringArgumentType.getString(ctx, "options")))
                )
            )
        );
    }

    private static int executeFireworkClear(CommandContext<CommandSourceStack> ctx) {
        var src = ctx.getSource();
        var player = src.getPlayer();
        if (player == null) { src.sendFailure(MessageUtil.error("commands.neoessentials.general.player_only")); return 0; }

        ItemStack held = player.getMainHandItem();
        if (!(held.getItem() instanceof FireworkRocketItem)) {
            src.sendFailure(MessageUtil.error("commands.neoessentials.firework.not_firework"));
            return 0;
        }
        // Replace Fireworks component with empty one
        Fireworks current = held.get(DataComponents.FIREWORKS);
        int power = current != null ? current.flightDuration() : 1;
        held.set(DataComponents.FIREWORKS, new Fireworks(power, List.of()));
        src.sendSuccess(() -> MessageUtil.success("commands.neoessentials.firework.cleared"), false);
        return 1;
    }

    private static int executeFireworkPower(CommandContext<CommandSourceStack> ctx, int level) {
        var src = ctx.getSource();
        var player = src.getPlayer();
        if (player == null) { src.sendFailure(MessageUtil.error("commands.neoessentials.general.player_only")); return 0; }

        ItemStack held = player.getMainHandItem();
        if (!(held.getItem() instanceof FireworkRocketItem)) {
            src.sendFailure(MessageUtil.error("commands.neoessentials.firework.not_firework"));
            return 0;
        }
        Fireworks current = held.get(DataComponents.FIREWORKS);
        List<FireworkExplosion> effects = current != null ? current.explosions() : List.of();
        held.set(DataComponents.FIREWORKS, new Fireworks((byte) Math.min(level, 127), effects));
        final int fl = level;
        src.sendSuccess(() -> MessageUtil.success("commands.neoessentials.firework.power_set", fl), false);
        return 1;
    }

    private static int executeFireworkFire(CommandContext<CommandSourceStack> ctx, int amount) {
        var src = ctx.getSource();
        var player = src.getPlayer();
        if (player == null) { src.sendFailure(MessageUtil.error("commands.neoessentials.general.player_only")); return 0; }

        ItemStack held = player.getMainHandItem();
        if (!(held.getItem() instanceof FireworkRocketItem)) {
            src.sendFailure(MessageUtil.error("commands.neoessentials.firework.not_firework"));
            return 0;
        }

        var level = com.zerog.neoessentials.util.LevelCompat.of(player);
        for (int i = 0; i < amount; i++) {
            var look = player.getLookAngle().normalize();
            var fw = new net.minecraft.world.entity.projectile.FireworkRocketEntity(
                level, held.copy(),
                player.getX(), player.getEyeY(), player.getZ(),
                true);
            fw.setDeltaMovement(look.x * 0.5, look.y * 0.5, look.z * 0.5);
            level.addFreshEntity(fw);
        }
        final int fa = amount;
        src.sendSuccess(() -> MessageUtil.success("commands.neoessentials.firework.fired", fa), false);
        LOGGER.info("{} fired {}x firework", player.getName().getString(), amount);
        return 1;
    }

    private static int executeFireworkColor(CommandContext<CommandSourceStack> ctx, String options) {
        var src = ctx.getSource();
        var player = src.getPlayer();
        if (player == null) { src.sendFailure(MessageUtil.error("commands.neoessentials.general.player_only")); return 0; }

        ItemStack held = player.getMainHandItem();
        if (!(held.getItem() instanceof FireworkRocketItem)) {
            src.sendFailure(MessageUtil.error("commands.neoessentials.firework.not_firework"));
            return 0;
        }

        // Parse: color:<hex[,hex]> [fade:<hex[,hex]>] [shape:<shape>] [effect:<trail|twinkle>]
        List<Integer> colors = parseColorList(options, "");
        List<Integer> fadeColors = parseColorList(options, "fade:");
        FireworkExplosion.Shape shape = parseFireworkShape(options);
        boolean trail = options.contains("trail");
        boolean twinkle = options.contains("twinkle") || options.contains("flicker");

        if (colors.isEmpty()) {
            src.sendFailure(MessageUtil.error("commands.neoessentials.firework.no_color"));
            return 0;
        }

        var intColors = new it.unimi.dsi.fastutil.ints.IntArrayList(
            colors.stream().mapToInt(Integer::intValue).toArray());
        var intFades = new it.unimi.dsi.fastutil.ints.IntArrayList(
            fadeColors.stream().mapToInt(Integer::intValue).toArray());
        FireworkExplosion explosion = new FireworkExplosion(shape, intColors, intFades, trail, twinkle);

        Fireworks current = held.get(DataComponents.FIREWORKS);
        int power = current != null ? current.flightDuration() : 1;
        List<FireworkExplosion> existingEffects = current != null ? current.explosions() : List.of();
        // Add the new explosion to the list
        var newEffects = new java.util.ArrayList<>(existingEffects);
        newEffects.add(explosion);
        held.set(DataComponents.FIREWORKS, new Fireworks(power, newEffects));
        src.sendSuccess(() -> MessageUtil.success("commands.neoessentials.firework.effect_added"), false);
        return 1;
    }

    /** Parse comma-separated hex colors, optionally after a prefix like "fade:" */
    private static List<Integer> parseColorList(String input, String prefix) {
        String lc = input.toLowerCase();
        int idx = prefix.isEmpty() ? 0 : lc.indexOf(prefix);
        if (idx < 0) return List.of();

        // Find the color segment: from after prefix until next space
        String segment = input.substring(idx + prefix.length()).trim();
        String colorPart = segment.split("\\s+")[0].replace(",", " ").trim();

        List<Integer> result = new java.util.ArrayList<>();
        for (String hex : colorPart.split("[,\\s]+")) {
            hex = hex.trim().replace("#", "");
            if (hex.isEmpty()) continue;
            try {
                result.add((int) Long.parseLong(hex, 16));
            } catch (NumberFormatException ignored) {}
        }
        return result;
    }

    /** Parse shape from options string */
    private static FireworkExplosion.Shape parseFireworkShape(String options) {
        String lc = options.toLowerCase();
        if (lc.contains("star"))   return FireworkExplosion.Shape.STAR;
        if (lc.contains("large")) return FireworkExplosion.Shape.LARGE_BALL;
        if (lc.contains("creeper")) return FireworkExplosion.Shape.CREEPER;
        if (lc.contains("burst")) return FireworkExplosion.Shape.BURST;
        return FireworkExplosion.Shape.SMALL_BALL; // default "ball"
    }

    // ── /nuke [player...] ─────────────────────────────────────────────────────
    // Essentials: rain TNT on target player(s). Admin-only fun command.
    private static void registerNuke(CommandDispatcher<CommandSourceStack> d) {
        d.register(Commands.literal("nuke")
            .requires(src -> {
                var p = src.getPlayer();
                return p == null || PermissionAPI.hasPermission(p.getUUID(), "neoessentials.nuke");
            })
            // /nuke — nuke all online players
            .executes(ctx -> executeNuke(ctx, null))
            // /nuke <player>
            .then(Commands.argument("target", StringArgumentType.word())
                .suggests((ctx, b) -> SharedSuggestionProvider.suggest(
                    ctx.getSource().getServer().getPlayerNames(), b))
                .executes(ctx -> executeNuke(ctx, StringArgumentType.getString(ctx, "target")))
            )
        );
    }

    private static int executeNuke(CommandContext<CommandSourceStack> ctx, String targetName) {
        var src = ctx.getSource();

        List<ServerPlayer> targets;
        if (targetName != null) {
            ServerPlayer p = src.getServer().getPlayerList().getPlayerByName(targetName);
            if (p == null) {
                src.sendFailure(MessageUtil.error("commands.neoessentials.general.player_not_found", targetName));
                return 0;
            }
            targets = List.of(p);
        } else {
            targets = src.getServer().getPlayerList().getPlayers();
        }

        int nuked = 0;
        for (ServerPlayer target : targets) {
            ServerLevel level = com.zerog.neoessentials.util.LevelCompat.of(target);
            // Message the target
            target.sendSystemMessage(Component.literal("§c☢ INCOMING NUKE! ☢"));
            // Spawn a 5x5 grid of TNT 64 blocks above the player
            int bx = target.getBlockX();
            int bz = target.getBlockZ();
            int topY = level.getMaxBuildHeight();
            for (int x = -10; x <= 10; x += 5) {
                for (int z = -10; z <= 10; z += 5) {
                    PrimedTnt tnt = EntityType.TNT.create(level);
                    if (tnt != null) {
                        tnt.moveTo(bx + x, topY, bz + z);
                        tnt.setFuse(80); // 4 seconds
                        level.addFreshEntity(tnt);
                    }
                }
            }
            nuked++;
        }
        final int fn = nuked;
        src.sendSuccess(() -> MessageUtil.success("commands.neoessentials.nuke.success", fn), true);
        LOGGER.info("{} nuked {} player(s)", src.getTextName(), nuked);
        return 1;
    }

    // ── /itemdb [item] ────────────────────────────────────────────────────────
    // Essentials: look up the registry ID, namespace, damage, and names for an item.
    private static void registerItemDb(CommandDispatcher<CommandSourceStack> d) {
        d.register(Commands.literal("itemdb")
            .requires(src -> {
                var p = src.getPlayer();
                return p == null || PermissionAPI.hasPermission(p.getUUID(), "neoessentials.itemdb");
            })
            // /itemdb — use held item
            .executes(ctx -> executeItemDb(ctx, null))
            // /itemdb <item>
            .then(Commands.argument("item", StringArgumentType.word())
                .suggests((ctx, b) -> SharedSuggestionProvider.suggest(
                    BuiltInRegistries.ITEM.keySet().stream()
                        .map(ResourceLocation::toString), b))
                .executes(ctx -> executeItemDb(ctx, StringArgumentType.getString(ctx, "item")))
            )
        );
    }

    private static int executeItemDb(CommandContext<CommandSourceStack> ctx, String itemArg) {
        var src = ctx.getSource();

        ItemStack stack;
        if (itemArg == null) {
            var player = src.getPlayer();
            if (player == null) { src.sendFailure(MessageUtil.error("commands.neoessentials.general.player_only")); return 0; }
            stack = player.getMainHandItem();
            if (stack.isEmpty()) {
                src.sendFailure(MessageUtil.error("commands.neoessentials.itemdb.nothing_held"));
                return 0;
            }
        } else {
            // Look up item by name
            String id = itemArg.contains(":") ? itemArg : "minecraft:" + itemArg;
            ResourceLocation loc = ResourceLocation.tryParse(id);
            if (loc == null) {
                src.sendFailure(MessageUtil.error("commands.neoessentials.itemdb.unknown", itemArg));
                return 0;
            }
            var item = BuiltInRegistries.ITEM.get(loc);
            if (item == Items.AIR && !itemArg.equalsIgnoreCase("air")) {
                // Try path-only search
                item = BuiltInRegistries.ITEM.entrySet().stream()
                    .filter(e -> e.getKey().location().getPath().equals(itemArg.toLowerCase()))
                    .map(java.util.Map.Entry::getValue)
                    .findFirst().orElse(null);
            }
            if (item == null || (item == Items.AIR && !itemArg.equalsIgnoreCase("air"))) {
                src.sendFailure(MessageUtil.error("commands.neoessentials.itemdb.unknown", itemArg));
                return 0;
            }
            stack = new ItemStack(item);
        }

        // Get registry ID
        var itemKey = BuiltInRegistries.ITEM.getKey(stack.getItem());
        String registryId = itemKey != null ? itemKey.toString() : "unknown";
        String displayName = stack.getItem().getDescription().getString();
        int maxStack = stack.getMaxStackSize();
        Integer maxDamageComp = stack.get(DataComponents.MAX_DAMAGE);
        int maxDamage = maxDamageComp != null ? maxDamageComp : 0;

        // Build output
        var sb = new StringBuilder();
        sb.append("§e--- §fItem Info: §b").append(displayName).append(" §e---\n");
        sb.append("§7ID: §f").append(registryId).append("\n");
        sb.append("§7Max Stack: §f").append(maxStack);
        if (maxDamage > 0) {
            sb.append("  §7Max Durability: §f").append(maxDamage);
        }

        // Show custom name if present
        var customName = stack.get(DataComponents.CUSTOM_NAME);
        if (customName != null) {
            sb.append("\n§7Custom Name: §f").append(customName.getString());
        }

        src.sendSuccess(() -> Component.literal(sb.toString()), false);
        return 1;
    }

    // ── /potion [clear|add <effect> [duration] [amplifier]] ──────────────────
    // Essentials: add/clear effects on a held potion. Like /effect but targets held item, not player.
    private static void registerPotion(CommandDispatcher<CommandSourceStack> d) {
        d.register(Commands.literal("potion")
            .requires(src -> {
                var p = src.getPlayer();
                return p == null || PermissionAPI.hasPermission(p.getUUID(), "neoessentials.potion");
            })
            .then(Commands.literal("clear")
                .executes(ctx -> executePotionClear(ctx))
            )
            .then(Commands.literal("add")
                .then(Commands.argument("effect", StringArgumentType.word())
                    .suggests((ctx, b) -> SharedSuggestionProvider.suggest(
                        BuiltInRegistries.MOB_EFFECT.keySet().stream()
                            .map(ResourceLocation::getPath), b))
                    .executes(ctx -> executePotionAdd(ctx,
                        StringArgumentType.getString(ctx, "effect"), 30, 0))
                    .then(Commands.argument("duration", IntegerArgumentType.integer(1, 1000000))
                        .executes(ctx -> executePotionAdd(ctx,
                            StringArgumentType.getString(ctx, "effect"),
                            IntegerArgumentType.getInteger(ctx, "duration"), 0))
                        .then(Commands.argument("amplifier", IntegerArgumentType.integer(0, 255))
                            .executes(ctx -> executePotionAdd(ctx,
                                StringArgumentType.getString(ctx, "effect"),
                                IntegerArgumentType.getInteger(ctx, "duration"),
                                IntegerArgumentType.getInteger(ctx, "amplifier")))
                        )
                    )
                )
            )
        );
    }

    private static int executePotionClear(CommandContext<CommandSourceStack> ctx) {
        var src = ctx.getSource();
        var player = src.getPlayer();
        if (player == null) { src.sendFailure(MessageUtil.error("commands.neoessentials.general.player_only")); return 0; }

        ItemStack held = player.getMainHandItem();
        if (!isPotionItem(held)) {
            src.sendFailure(MessageUtil.error("commands.neoessentials.potion.not_potion"));
            return 0;
        }

        held.remove(DataComponents.POTION_CONTENTS);
        src.sendSuccess(() -> MessageUtil.success("commands.neoessentials.potion.cleared"), false);
        return 1;
    }

    private static int executePotionAdd(CommandContext<CommandSourceStack> ctx,
            String effectId, int durationSecs, int amplifier) {
        var src = ctx.getSource();
        var player = src.getPlayer();
        if (player == null) { src.sendFailure(MessageUtil.error("commands.neoessentials.general.player_only")); return 0; }

        ItemStack held = player.getMainHandItem();
        if (!isPotionItem(held)) {
            src.sendFailure(MessageUtil.error("commands.neoessentials.potion.not_potion"));
            return 0;
        }

        // Resolve effect
        String id = effectId.contains(":") ? effectId : "minecraft:" + effectId;
        ResourceLocation loc = ResourceLocation.tryParse(id);
        var effectHolder = loc != null ? BuiltInRegistries.MOB_EFFECT.get(loc) : null;
        if (effectHolder == null) {
            effectHolder = BuiltInRegistries.MOB_EFFECT.entrySet().stream()
                .filter(e -> e.getKey().location().getPath().equals(effectId.toLowerCase()))
                .map(java.util.Map.Entry::getValue).findFirst().orElse(null);
        }
        if (effectHolder == null) {
            src.sendFailure(MessageUtil.error("commands.neoessentials.effect.unknown", effectId));
            return 0;
        }

        var instance = new net.minecraft.world.effect.MobEffectInstance(
            net.minecraft.core.Holder.direct(effectHolder),
            durationSecs * 20, amplifier, false, true);

        // Get or create PotionContents
        var existing = held.get(DataComponents.POTION_CONTENTS);
        List<net.minecraft.world.effect.MobEffectInstance> custom;
        if (existing != null) {
            custom = new java.util.ArrayList<>(existing.customEffects());
        } else {
            custom = new java.util.ArrayList<>();
        }
        custom.add(instance);

        net.minecraft.world.item.alchemy.PotionContents newContents = existing != null
            ? new net.minecraft.world.item.alchemy.PotionContents(existing.potion(), existing.customColor(), custom)
            : new net.minecraft.world.item.alchemy.PotionContents(java.util.Optional.empty(),
                java.util.Optional.empty(), custom);

        held.set(DataComponents.POTION_CONTENTS, newContents);
        final String fid = effectId; final int fa = amplifier; final int fd = durationSecs;
        src.sendSuccess(() -> MessageUtil.success("commands.neoessentials.potion.added", fid, fa, fd), false);
        return 1;
    }

    private static boolean isPotionItem(ItemStack stack) {
        return stack.getItem() == Items.POTION
            || stack.getItem() == Items.SPLASH_POTION
            || stack.getItem() == Items.LINGERING_POTION
            || stack.getItem() == Items.TIPPED_ARROW;
    }

    // ── /info ─────────────────────────────────────────────────────────────────
    // Essentials: show server info pages from config.
    private static void registerInfo(CommandDispatcher<CommandSourceStack> d) {
        d.register(Commands.literal("info")
            .requires(src -> {
                var p = src.getPlayer();
                return p == null || PermissionAPI.hasPermission(p.getUUID(), "neoessentials.info");
            })
            .executes(ctx -> executeInfo(ctx))
        );
    }

    private static int executeInfo(CommandContext<CommandSourceStack> ctx) {
        var src = ctx.getSource();
        var player = src.getPlayer();

        String motd = com.zerog.neoessentials.util.motd.MotdManager.getInstance().getActiveMotd();
        if (motd == null) motd = "";

        String playerName = player != null ? player.getName().getString() : "Server";
        String motdResolved = motd.replace("{player}", playerName).replace("{name}", playerName);

        // Show MOTD + a pointer to /rules as /info
        src.sendSuccess(() -> Component.literal(
            "§b============ §fServer Info §b============\n" +
            motdResolved + "\n" +
            "§7Type §f/rules §7to view server rules."
        ), false);
        return 1;
    }

    // ── Helpers ───────────────────────────────────────────────────────────────
    @SuppressWarnings("unused")
    private static BlockPos getHighestBlock(ServerLevel level, int x, int z) {
        return BlockPos.containing(x, level.getMaxBuildHeight(), z);
    }

    // ── /antioch ──────────────────────────────────────────────────────────────
    // Essentials: Commandantioch — spawn a lit TNT at the block you're looking at.
    // Easter egg: with any argument, broadcasts the "Holy Hand Grenade" message.
    private static void registerAntioch(CommandDispatcher<CommandSourceStack> d) {
        d.register(Commands.literal("antioch")
            .requires(src -> {
                var p = src.getPlayer();
                return p == null || PermissionAPI.hasPermission(p.getUUID(), "neoessentials.antioch");
            })
            .executes(ctx -> executeAntioch(ctx, false))
            .then(Commands.argument("flavour", StringArgumentType.greedyString())
                .executes(ctx -> executeAntioch(ctx, true))
            )
        );
    }

    private static int executeAntioch(CommandContext<CommandSourceStack> ctx, boolean flavour) {
        var src = ctx.getSource();
        var player = src.getPlayer();
        if (player == null) { src.sendFailure(MessageUtil.error("commands.neoessentials.general.player_only")); return 0; }

        if (flavour) {
            var server = src.getServer();
            server.getPlayerList().broadcastSystemMessage(
                Component.literal("§6...lobbest thou thy Holy Hand Grenade of Antioch towards thy foe,"), false);
            server.getPlayerList().broadcastSystemMessage(
                Component.literal("§6who being naughty in My sight, shall snuff it."), false);
        }

        var hit = player.pick(20, 1.0f, false);
        var pos = hit.getLocation();
        ServerLevel level = com.zerog.neoessentials.util.LevelCompat.of(player);
        PrimedTnt tnt = EntityType.TNT.create(level);
        if (tnt != null) {
            tnt.moveTo(pos.x, pos.y, pos.z);
            tnt.setFuse(80);
            level.addFreshEntity(tnt);
        }
        src.sendSuccess(() -> MessageUtil.success("commands.neoessentials.antioch.success"), false);
        return 1;
    }

    // ── /kittycannon ──────────────────────────────────────────────────────────
    // Essentials: Commandkittycannon — launch a baby cat that explodes on landing.
    private static final Random RANDOM = new Random();

    private static void registerKittyCannon(CommandDispatcher<CommandSourceStack> d) {
        d.register(Commands.literal("kittycannon")
            .requires(src -> {
                var p = src.getPlayer();
                return p == null || PermissionAPI.hasPermission(p.getUUID(), "neoessentials.kittycannon");
            })
            .executes(ctx -> executeKittyCannon(ctx))
        );
    }

    private static int executeKittyCannon(CommandContext<CommandSourceStack> ctx) {
        var src = ctx.getSource();
        var player = src.getPlayer();
        if (player == null) { src.sendFailure(MessageUtil.error("commands.neoessentials.general.player_only")); return 0; }

        ServerLevel level = com.zerog.neoessentials.util.LevelCompat.of(player);
        Cat cat = EntityType.CAT.create(level);
        if (cat == null) {
            src.sendFailure(MessageUtil.error("commands.neoessentials.general.error"));
            return 0;
        }

        // Pick a random cat variant from the registry
        var variants = net.minecraft.core.registries.BuiltInRegistries.CAT_VARIANT.holders().toList();
        if (!variants.isEmpty()) {
            cat.setVariant(variants.get(RANDOM.nextInt(variants.size())));
        }
        cat.setAge(-24000); // negative age = baby in vanilla AgeableMob
        cat.moveTo(player.getX(), player.getEyeY(), player.getZ());
        var look = player.getLookAngle().normalize().scale(2.0);
        cat.setDeltaMovement(look.x, look.y, look.z);
        level.addFreshEntity(cat);

        // Schedule explosion after 20 ticks (1 second)
        final var catRef = cat;
        com.zerog.neoessentials.scheduler.DelayedTaskScheduler.schedule(20, () -> {
            if (catRef.isAlive()) {
                var loc = catRef.position();
                catRef.discard();
                level.explode(null, loc.x, loc.y, loc.z, 0f,
                    Level.ExplosionInteraction.NONE);
            }
        });

        src.sendSuccess(() -> MessageUtil.success("commands.neoessentials.kittycannon.fired"), false);
        return 1;
    }

    // ── /beezooka ─────────────────────────────────────────────────────────────
    // NeoEssentials: launch an angry bee in your look direction. It targets whoever it hits first.
    private static void registerBeezooka(CommandDispatcher<CommandSourceStack> d) {
        d.register(Commands.literal("beezooka")
            .requires(src -> {
                var p = src.getPlayer();
                return p == null || PermissionAPI.hasPermission(p.getUUID(), "neoessentials.beezooka");
            })
            .executes(ctx -> executeBeezooka(ctx, 1))
            .then(Commands.argument("amount", IntegerArgumentType.integer(1, 20))
                .executes(ctx -> executeBeezooka(ctx, IntegerArgumentType.getInteger(ctx, "amount")))
            )
        );
    }

    private static int executeBeezooka(CommandContext<CommandSourceStack> ctx, int amount) {
        var src = ctx.getSource();
        var player = ctx.getSource().getPlayer();
        if (player == null) { src.sendFailure(MessageUtil.error("commands.neoessentials.general.player_only")); return 0; }

        ServerLevel level = com.zerog.neoessentials.util.LevelCompat.of(player);
        int spawned = 0;
        for (int i = 0; i < amount; i++) {
            var bee = EntityType.BEE.create(level);
            if (bee != null) {
                bee.moveTo(player.getX(), player.getEyeY(), player.getZ());
                var look = player.getLookAngle().normalize().scale(1.5 + RANDOM.nextDouble() * 0.5);
                bee.setDeltaMovement(look.x, look.y + 0.1, look.z);
                bee.setRemainingPersistentAngerTime(400 + RANDOM.nextInt(400)); // angry for 20–40s
                bee.setTarget(null); // will auto-target nearby enemies
                level.addFreshEntity(bee);
                spawned++;
            }
        }
        final int fs = spawned;
        src.sendSuccess(() -> MessageUtil.success("commands.neoessentials.beezooka.fired", fs), false);
        return 1;
    }

    // ── /rest [player] ────────────────────────────────────────────────────────
    // Essentials: Commandrest — reset the player's "time since last rest" stat,
    // which controls phantom spawning (players who haven't slept get phantoms after 3 in-game days).
    private static void registerRest(CommandDispatcher<CommandSourceStack> d) {
        d.register(Commands.literal("rest")
            .requires(src -> {
                var p = src.getPlayer();
                return p == null || PermissionAPI.hasPermission(p.getUUID(), "neoessentials.rest");
            })
            .executes(ctx -> executeRest(ctx, null))
            .then(Commands.argument("target", StringArgumentType.word())
                .suggests((ctx, b) -> SharedSuggestionProvider.suggest(
                    ctx.getSource().getServer().getPlayerNames(), b))
                .requires(src -> src.getPlayer() == null
                    || PermissionAPI.hasPermission(src.getPlayer().getUUID(), "neoessentials.rest.others"))
                .executes(ctx -> executeRest(ctx, StringArgumentType.getString(ctx, "target")))
            )
        );
    }

    private static int executeRest(CommandContext<CommandSourceStack> ctx, String targetName) {
        var src = ctx.getSource();
        ServerPlayer target;
        if (targetName != null) {
            target = src.getServer().getPlayerList().getPlayerByName(targetName);
            if (target == null) {
                src.sendFailure(MessageUtil.error("commands.neoessentials.general.player_not_found", targetName));
                return 0;
            }
        } else {
            target = src.getPlayer();
            if (target == null) { src.sendFailure(MessageUtil.error("commands.neoessentials.general.player_only")); return 0; }
        }

        // Reset TIME_SINCE_REST stat — prevents phantom spawning
        target.resetStat(net.minecraft.stats.Stats.CUSTOM.get(net.minecraft.stats.Stats.TIME_SINCE_REST));

        final String name = target.getName().getString();
        if (targetName != null) {
            src.sendSuccess(() -> MessageUtil.success("commands.neoessentials.rest.other", name), true);
            target.sendSystemMessage(MessageUtil.success("commands.neoessentials.rest.self"));
        } else {
            src.sendSuccess(() -> MessageUtil.success("commands.neoessentials.rest.self"), false);
        }
        return 1;
    }

    // ── /backup ───────────────────────────────────────────────────────────────
    // Essentials: Commandbackup — trigger save-all then run the configured backup command.
    private static void registerBackup(CommandDispatcher<CommandSourceStack> d) {
        d.register(Commands.literal("backup")
            .requires(src -> {
                var p = src.getPlayer();
                return p == null || PermissionAPI.hasPermission(p.getUUID(), "neoessentials.backup");
            })
            .executes(ctx -> executeBackup(ctx))
        );
    }

    private static int executeBackup(CommandContext<CommandSourceStack> ctx) {
        var src = ctx.getSource();
        var server = src.getServer();

        // Step 1: save-all
        src.sendSuccess(() -> Component.literal("§eSaving world data..."), false);
        server.getAllLevels().forEach(level -> level.save(null, true, false));

        // Step 2: run configured backup command (if any)
        String backupCmd = ConfigManager.getInstance().getBackupCommand();
        if (backupCmd != null && !backupCmd.isBlank() && !backupCmd.equalsIgnoreCase("save-all")) {
            src.sendSuccess(() -> Component.literal("§eRunning backup command: §f" + backupCmd), false);
            try {
                new ProcessBuilder(backupCmd.split("\\s+"))
                    .inheritIO()
                    .start();
            } catch (Exception e) {
                src.sendFailure(Component.literal("§cBackup command failed: " + e.getMessage()));
                LOGGER.error("Backup command '{}' failed: {}", backupCmd, e.getMessage(), e);
                return 0;
            }
        }

        src.sendSuccess(() -> MessageUtil.success("commands.neoessentials.backup.done"), true);
        LOGGER.info("{} triggered a server backup", src.getTextName());
        return 1;
    }
}









