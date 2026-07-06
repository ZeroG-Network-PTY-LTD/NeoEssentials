package com.zerog.neoessentials.items.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.zerog.neoessentials.api.permissions.PermissionAPI;
import com.zerog.neoessentials.config.ConfigManager;
import com.zerog.neoessentials.kits.Kit;
import com.zerog.neoessentials.kits.KitManager;
import com.zerog.neoessentials.util.MessageUtil;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Miscellaneous item/kit/utility commands ported from EssentialsX:
 *
 * /condense           — convert items to their storage-block form (nuggets→ingots→blocks)
 * /showkit <kit>      — preview kit contents without claiming (Commandshowkit)
 * /powertoollist      — list all active powertools (Commandpowertoollist)
 * /customtext <page>  — display a custom server info page from a text file (Commandcustomtext)
 * /payconfirmtoggle   — toggle payment confirmation prompts (Commandpayconfirmtoggle)
 * /ciconfirmtoggle    — toggle /clearinventory confirmation prompts (Commandclearinventoryconfirmtoggle)
 */
public class MiscItemCommands {
    private static final Logger LOGGER = LoggerFactory.getLogger(MiscItemCommands.class);

    // Per-player toggles — survive server restart only via NeoEssentialsManager persistence
    private static final Set<UUID> payConfirmDisabled = ConcurrentHashMap.newKeySet();
    private static final Set<UUID> ciConfirmDisabled  = ConcurrentHashMap.newKeySet();

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        registerCondense(dispatcher);
        registerShowKit(dispatcher);
        registerPowertoolList(dispatcher);
        registerCustomText(dispatcher);
        registerPayConfirmToggle(dispatcher);
        registerCiConfirmToggle(dispatcher);
        registerItem(dispatcher);
        registerRToggle(dispatcher);
    }

    // ── /condense [item] ──────────────────────────────────────────────────────
    // Essentials: Commandcondense — compact items in inventory to their block forms.
    // E.g. 9 iron ingots → 1 iron block, 9 gold nuggets → 1 gold ingot.
    private static void registerCondense(CommandDispatcher<CommandSourceStack> d) {
        d.register(Commands.literal("condense")
            .requires(src -> {
                var p = src.getPlayer();
                return p == null || PermissionAPI.hasPermission(p.getUUID(), "neoessentials.condense");
            })
            .executes(ctx -> executeCondense(ctx, null))
            .then(Commands.argument("item", StringArgumentType.word())
                .suggests((ctx, b) -> SharedSuggestionProvider.suggest(
                    BuiltInRegistries.ITEM.keySet().stream().map(Identifier::getPath), b))
                .executes(ctx -> executeCondense(ctx, StringArgumentType.getString(ctx, "item")))
            )
        );
    }

    /**
     * Condensation recipes: input item → (amount needed, output item)
     * Based on vanilla crafting: 9 nuggets→ingot, 9 ingots→block, 9 seeds→block, etc.
     */
    private static final Map<String, CondenseRecipe> CONDENSE_MAP = new LinkedHashMap<>();

    static {
        // Nuggets → Ingots
        CONDENSE_MAP.put("minecraft:gold_nugget",   new CondenseRecipe(9, "minecraft:gold_ingot"));
        CONDENSE_MAP.put("minecraft:iron_nugget",   new CondenseRecipe(9, "minecraft:iron_ingot"));
        // Ingots → Blocks
        CONDENSE_MAP.put("minecraft:iron_ingot",    new CondenseRecipe(9, "minecraft:iron_block"));
        CONDENSE_MAP.put("minecraft:gold_ingot",    new CondenseRecipe(9, "minecraft:gold_block"));
        CONDENSE_MAP.put("minecraft:copper_ingot",  new CondenseRecipe(9, "minecraft:copper_block"));
        CONDENSE_MAP.put("minecraft:netherite_ingot", new CondenseRecipe(9, "minecraft:netherite_block"));
        CONDENSE_MAP.put("minecraft:diamond",       new CondenseRecipe(9, "minecraft:diamond_block"));
        CONDENSE_MAP.put("minecraft:emerald",       new CondenseRecipe(9, "minecraft:emerald_block"));
        CONDENSE_MAP.put("minecraft:lapis_lazuli",  new CondenseRecipe(9, "minecraft:lapis_block"));
        CONDENSE_MAP.put("minecraft:redstone",      new CondenseRecipe(9, "minecraft:redstone_block"));
        CONDENSE_MAP.put("minecraft:coal",          new CondenseRecipe(9, "minecraft:coal_block"));
        CONDENSE_MAP.put("minecraft:quartz",        new CondenseRecipe(4, "minecraft:quartz_block"));
        CONDENSE_MAP.put("minecraft:wheat",         new CondenseRecipe(9, "minecraft:hay_block"));
        CONDENSE_MAP.put("minecraft:snowball",      new CondenseRecipe(4, "minecraft:snow_block"));
        CONDENSE_MAP.put("minecraft:ice",           new CondenseRecipe(9, "minecraft:packed_ice"));
        CONDENSE_MAP.put("minecraft:packed_ice",    new CondenseRecipe(9, "minecraft:blue_ice"));
        CONDENSE_MAP.put("minecraft:bone_meal",     new CondenseRecipe(9, "minecraft:bone_block"));
        CONDENSE_MAP.put("minecraft:slime_ball",    new CondenseRecipe(9, "minecraft:slime_block"));
        CONDENSE_MAP.put("minecraft:honeycomb",     new CondenseRecipe(4, "minecraft:honeycomb_block"));
        CONDENSE_MAP.put("minecraft:amethyst_shard",new CondenseRecipe(4, "minecraft:amethyst_block"));
        CONDENSE_MAP.put("minecraft:dried_kelp",    new CondenseRecipe(9, "minecraft:dried_kelp_block"));
        CONDENSE_MAP.put("minecraft:melon_slice",   new CondenseRecipe(9, "minecraft:melon"));
        CONDENSE_MAP.put("minecraft:netherite_scrap",new CondenseRecipe(4, "minecraft:netherite_ingot"));
    }

    private record CondenseRecipe(int inputCount, String outputItemId) {}

    private static int executeCondense(CommandContext<CommandSourceStack> ctx, String filterItem) {
        var src = ctx.getSource();
        var player = src.getPlayer();
        if (player == null) { src.sendFailure(MessageUtil.error("commands.neoessentials.general.player_only")); return 0; }

        Inventory inv = player.getInventory();
        int convertCount = 0;

        for (Map.Entry<String, CondenseRecipe> entry : CONDENSE_MAP.entrySet()) {
            String inputId = entry.getKey();
            CondenseRecipe recipe = entry.getValue();

            // If a specific item was requested, filter
            if (filterItem != null) {
                String filter = filterItem.contains(":") ? filterItem : "minecraft:" + filterItem;
                if (!inputId.equals(filter)) continue;
            }

            Identifier inputLoc = Identifier.tryParse(inputId);
            Identifier outputLoc = Identifier.tryParse(recipe.outputItemId());
            if (inputLoc == null || outputLoc == null) continue;

            var inputItem = BuiltInRegistries.ITEM.getValue(inputLoc);
            var outputItem = BuiltInRegistries.ITEM.getValue(outputLoc);
            if (inputItem == null || outputItem == null) continue;

            // Count how many of this input item the player has
            int totalInput = 0;
            for (int i = 0; i < inv.getContainerSize(); i++) {
                ItemStack s = inv.getItem(i);
                if (!s.isEmpty() && s.getItem() == inputItem) {
                    totalInput += s.getCount();
                }
            }

            int sets = totalInput / recipe.inputCount();
            if (sets == 0) continue;

            int toRemove = sets * recipe.inputCount();

            // Remove input items
            int remaining = toRemove;
            for (int i = 0; i < inv.getContainerSize() && remaining > 0; i++) {
                ItemStack s = inv.getItem(i);
                if (!s.isEmpty() && s.getItem() == inputItem) {
                    int take = Math.min(remaining, s.getCount());
                    s.shrink(take);
                    remaining -= take;
                    if (s.isEmpty()) inv.setItem(i, ItemStack.EMPTY);
                }
            }

            // Give output items
            ItemStack output = new ItemStack(outputItem, sets);
            if (!player.getInventory().add(output)) {
                // Drop at player's feet if inventory full
                player.drop(output, false);
            }
            convertCount++;
        }

        if (convertCount > 0) {
            player.inventoryMenu.sendAllDataToRemote();
            final int fc = convertCount;
            src.sendSuccess(() -> MessageUtil.success("commands.neoessentials.condense.success", fc), false);
            LOGGER.info("{} condensed {} item type(s)", player.getName().getString(), convertCount);
        } else {
            src.sendFailure(MessageUtil.error("commands.neoessentials.condense.nothing"));
        }
        return convertCount > 0 ? 1 : 0;
    }

    // ── /showkit <kitname[,kitname2]> ────────────────────────────────────────
    // Essentials: Commandshowkit — preview kit contents in chat without claiming.
    private static void registerShowKit(CommandDispatcher<CommandSourceStack> d) {
        d.register(Commands.literal("showkit")
            .requires(src -> {
                var p = src.getPlayer();
                return p == null || PermissionAPI.hasPermission(p.getUUID(), "neoessentials.showkit");
            })
            .then(Commands.argument("kit", StringArgumentType.word())
                .suggests((ctx, b) -> SharedSuggestionProvider.suggest(
                    KitManager.getInstance().getKitNames(), b))
                .executes(ctx -> executeShowKit(ctx, StringArgumentType.getString(ctx, "kit")))
            )
        );
    }

    private static int executeShowKit(CommandContext<CommandSourceStack> ctx, String kitNames) {
        var src = ctx.getSource();
        int shown = 0;

        for (String kitName : kitNames.split(",")) {
            kitName = kitName.trim().toLowerCase();
            Kit kit = KitManager.getInstance().getKit(kitName);
            if (kit == null) {
                src.sendFailure(MessageUtil.error("commands.neoessentials.kits.not_found", kitName));
                continue;
            }

            src.sendSuccess(() -> MessageUtil.info("commands.neoessentials.showkit.header", kit.getDisplayName()), false);
            if (!kit.getDescription().isEmpty()) {
                final String desc = kit.getDescription();
                src.sendSuccess(() -> MessageUtil.component("commands.neoessentials.showkit.description", desc), false);
            }
            // Cooldown line
            long cdMs = kit.getCooldownMillis();
            String cdStr = cdMs <= 0 ? "No cooldown" : formatDuration(cdMs);
            final String fCdStr = cdStr;
            src.sendSuccess(() -> MessageUtil.component("commands.neoessentials.showkit.cooldown", fCdStr), false);
            // Items
            for (ItemStack stack : kit.getItems()) {
                String itemName = stack.getItem().getName(stack).getString();
                int count = stack.getCount();
                src.sendSuccess(() -> MessageUtil.component("commands.neoessentials.showkit.item_line", count, itemName), false);
            }
            shown++;
        }
        return shown > 0 ? 1 : 0;
    }

    // ── /powertoollist ────────────────────────────────────────────────────────
    // Essentials: Commandpowertoollist — show all active powertool bindings.
    private static void registerPowertoolList(CommandDispatcher<CommandSourceStack> d) {
        d.register(Commands.literal("powertoollist")
            .requires(src -> {
                var p = src.getPlayer();
                return p == null || PermissionAPI.hasPermission(p.getUUID(), "neoessentials.powertoollist");
            })
            .executes(ctx -> executePowertoolList(ctx))
        );
        // alias /ptlist
        d.register(Commands.literal("ptlist")
            .requires(src -> {
                var p = src.getPlayer();
                return p == null || PermissionAPI.hasPermission(p.getUUID(), "neoessentials.powertoollist");
            })
            .executes(ctx -> executePowertoolList(ctx))
        );
    }

    private static int executePowertoolList(CommandContext<CommandSourceStack> ctx) {
        var src = ctx.getSource();
        var player = src.getPlayer();
        if (player == null) { src.sendFailure(MessageUtil.error("commands.neoessentials.general.player_only")); return 0; }

        Map<String, String> powers = PowertoolCommand.getPlayerPowertools(player.getUUID());
        if (powers.isEmpty()) {
            src.sendFailure(MessageUtil.error("commands.neoessentials.powertoollist.empty"));
            return 0;
        }

        src.sendSuccess(() -> MessageUtil.info("commands.neoessentials.powertoollist.header", powers.size()), false);
        for (Map.Entry<String, String> entry : powers.entrySet()) {
            String itemName = entry.getKey().contains(":") ? entry.getKey().substring(entry.getKey().indexOf(':') + 1) : entry.getKey();
            String cmd = entry.getValue();
            src.sendSuccess(() -> MessageUtil.component("commands.neoessentials.powertoollist.entry", itemName, cmd), false);
        }
        return 1;
    }

    // ── /customtext [chapter] [page] ─────────────────────────────────────────
    // Essentials: Commandcustomtext — display a paginated custom text file.
    // Files live in config/neoessentials/text/<chapter>.txt
    private static final int LINES_PER_PAGE = 10;

    private static void registerCustomText(CommandDispatcher<CommandSourceStack> d) {
        d.register(Commands.literal("customtext")
            .requires(src -> {
                var p = src.getPlayer();
                return p == null || PermissionAPI.hasPermission(p.getUUID(), "neoessentials.customtext");
            })
            .executes(ctx -> executeCustomText(ctx, "info", 1))
            .then(Commands.argument("chapter", StringArgumentType.word())
                .executes(ctx -> executeCustomText(ctx, StringArgumentType.getString(ctx, "chapter"), 1))
                .then(Commands.argument("page", IntegerArgumentType.integer(1))
                    .executes(ctx -> executeCustomText(ctx,
                        StringArgumentType.getString(ctx, "chapter"),
                        IntegerArgumentType.getInteger(ctx, "page")))
                )
            )
        );
        // Also register /ctext as alias
        d.register(Commands.literal("ctext")
            .requires(src -> {
                var p = src.getPlayer();
                return p == null || PermissionAPI.hasPermission(p.getUUID(), "neoessentials.customtext");
            })
            .executes(ctx -> executeCustomText(ctx, "info", 1))
            .then(Commands.argument("chapter", StringArgumentType.word())
                .executes(ctx -> executeCustomText(ctx, StringArgumentType.getString(ctx, "chapter"), 1))
                .then(Commands.argument("page", IntegerArgumentType.integer(1))
                    .executes(ctx -> executeCustomText(ctx,
                        StringArgumentType.getString(ctx, "chapter"),
                        IntegerArgumentType.getInteger(ctx, "page")))
                )
            )
        );
    }

    private static int executeCustomText(CommandContext<CommandSourceStack> ctx, String chapter, int page) {
        var src = ctx.getSource();
        // Sanitize chapter name — allow only alphanumeric, dash, underscore
        String safeChapter = chapter.replaceAll("[^a-zA-Z0-9_\\-]", "");
        if (safeChapter.isEmpty()) {
            src.sendFailure(MessageUtil.error("commands.neoessentials.customtext.invalid_chapter"));
            return 0;
        }

        Path textDir = Paths.get("config", "neoessentials", "text");
        Path file = textDir.resolve(safeChapter + ".txt");

        if (!Files.exists(file)) {
            // Try to create the directory and a placeholder
            try {
                Files.createDirectories(textDir);
                if (safeChapter.equals("info")) {
                    Files.writeString(file,
                        "§6Welcome to the server!\n" +
                        "§7Edit this file at: config/neoessentials/text/info.txt\n" +
                        "§7You can use §r&§7 colour codes.\n");
                }
            } catch (IOException e) {
                LOGGER.warn("Could not create text dir: {}", e.getMessage());
            }
            if (!Files.exists(file)) {
                src.sendFailure(MessageUtil.error("commands.neoessentials.customtext.not_found", safeChapter));
                return 0;
            }
        }

        List<String> lines;
        try {
            lines = Files.readAllLines(file);
        } catch (IOException e) {
            LOGGER.error("Failed to read custom text file '{}': {}", file, e.getMessage());
            src.sendFailure(MessageUtil.error("commands.neoessentials.customtext.read_error"));
            return 0;
        }

        // Replace colour codes and {player} placeholder
        String playerName = src.getPlayer() != null ? src.getPlayer().getName().getString() : "Server";
        List<String> formatted = new ArrayList<>();
        for (String line : lines) {
            formatted.add(line.replace("&", "§").replace("{player}", playerName));
        }

        int totalPages = Math.max(1, (int) Math.ceil(formatted.size() / (double) LINES_PER_PAGE));
        int p = Math.max(1, Math.min(page, totalPages));
        int start = (p - 1) * LINES_PER_PAGE;
        int end = Math.min(start + LINES_PER_PAGE, formatted.size());

        final int fp = p, fTotalPages = totalPages;
        src.sendSuccess(() -> MessageUtil.component("commands.neoessentials.customtext.page_header", safeChapter, fp, fTotalPages), false);
        for (int i = start; i < end; i++) {
            String line = formatted.get(i);
            src.sendSuccess(() -> Component.literal(line), false);
        }
        if (totalPages > 1) {
            src.sendSuccess(() -> MessageUtil.component("commands.neoessentials.customtext.page_footer", safeChapter), false);
        }
        return 1;
    }

    // ── /payconfirmtoggle ────────────────────────────────────────────────────
    // Essentials: Commandpayconfirmtoggle — toggle whether /pay asks for confirmation.
    private static void registerPayConfirmToggle(CommandDispatcher<CommandSourceStack> d) {
        d.register(Commands.literal("payconfirmtoggle")
            .requires(src -> {
                var p = src.getPlayer();
                return p == null || PermissionAPI.hasPermission(p.getUUID(), "neoessentials.payconfirmtoggle");
            })
            .executes(ctx -> executePayConfirmToggle(ctx))
        );
    }

    private static int executePayConfirmToggle(CommandContext<CommandSourceStack> ctx) {
        var src = ctx.getSource();
        var player = src.getPlayer();
        if (player == null) { src.sendFailure(MessageUtil.error("commands.neoessentials.general.player_only")); return 0; }

        UUID uuid = player.getUUID();
        boolean nowDisabled;
        if (payConfirmDisabled.contains(uuid)) {
            payConfirmDisabled.remove(uuid);
            nowDisabled = false;
        } else {
            payConfirmDisabled.add(uuid);
            nowDisabled = true;
        }
        String state = nowDisabled ? "§cdisabled" : "§aenabled";
        src.sendSuccess(() -> MessageUtil.success("commands.neoessentials.payconfirmtoggle.toggled", state), false);
        return 1;
    }

    /** Returns true if pay confirmation prompts are disabled for this player. */
    public static boolean isPayConfirmDisabled(UUID uuid) {
        return payConfirmDisabled.contains(uuid);
    }

    // ── /ciconfirmtoggle ─────────────────────────────────────────────────────
    // Essentials: Commandclearinventoryconfirmtoggle — toggle /ci confirmation.
    private static void registerCiConfirmToggle(CommandDispatcher<CommandSourceStack> d) {
        d.register(Commands.literal("ciconfirmtoggle")
            .requires(src -> {
                var p = src.getPlayer();
                return p == null || PermissionAPI.hasPermission(p.getUUID(), "neoessentials.ciconfirmtoggle");
            })
            .executes(ctx -> executeCiConfirmToggle(ctx))
        );
        d.register(Commands.literal("clearinventoryconfirmtoggle")
            .requires(src -> {
                var p = src.getPlayer();
                return p == null || PermissionAPI.hasPermission(p.getUUID(), "neoessentials.ciconfirmtoggle");
            })
            .executes(ctx -> executeCiConfirmToggle(ctx))
        );
    }

    private static int executeCiConfirmToggle(CommandContext<CommandSourceStack> ctx) {
        var src = ctx.getSource();
        var player = src.getPlayer();
        if (player == null) { src.sendFailure(MessageUtil.error("commands.neoessentials.general.player_only")); return 0; }

        UUID uuid = player.getUUID();
        boolean nowDisabled;
        if (ciConfirmDisabled.contains(uuid)) {
            ciConfirmDisabled.remove(uuid);
            nowDisabled = false;
        } else {
            ciConfirmDisabled.add(uuid);
            nowDisabled = true;
        }
        String state = nowDisabled ? "§cdisabled" : "§aenabled";
        src.sendSuccess(() -> MessageUtil.success("commands.neoessentials.ciconfirmtoggle.toggled", state), false);
        return 1;
    }

    /** Returns true if /clearinventory confirmation prompts are disabled for this player. */
    public static boolean isCiConfirmDisabled(UUID uuid) {
        return ciConfirmDisabled.contains(uuid);
    }

    // ── /item <name> [amount] ─────────────────────────────────────────────────
    // Essentials: Commanditem — give self an item by registry name/alias.
    // Like /give but simpler: /item diamond 64
    private static void registerItem(CommandDispatcher<CommandSourceStack> d) {
        d.register(Commands.literal("item")
            .requires(src -> {
                var p = src.getPlayer();
                return p == null || PermissionAPI.hasPermission(p.getUUID(), "neoessentials.item");
            })
            .then(Commands.argument("item", StringArgumentType.word())
                .suggests((ctx, b) -> SharedSuggestionProvider.suggest(
                    BuiltInRegistries.ITEM.keySet().stream().map(Identifier::toString), b))
                .executes(ctx -> executeItem(ctx, StringArgumentType.getString(ctx, "item"), -1))
                .then(Commands.argument("amount", IntegerArgumentType.integer(1, 64))
                    .executes(ctx -> executeItem(ctx,
                        StringArgumentType.getString(ctx, "item"),
                        IntegerArgumentType.getInteger(ctx, "amount")))
                )
            )
        );
        // /i alias
        d.register(Commands.literal("i")
            .requires(src -> {
                var p = src.getPlayer();
                return p == null || PermissionAPI.hasPermission(p.getUUID(), "neoessentials.item");
            })
            .then(Commands.argument("item", StringArgumentType.word())
                .suggests((ctx, b) -> SharedSuggestionProvider.suggest(
                    BuiltInRegistries.ITEM.keySet().stream().map(Identifier::toString), b))
                .executes(ctx -> executeItem(ctx, StringArgumentType.getString(ctx, "item"), -1))
                .then(Commands.argument("amount", IntegerArgumentType.integer(1, 64))
                    .executes(ctx -> executeItem(ctx,
                        StringArgumentType.getString(ctx, "item"),
                        IntegerArgumentType.getInteger(ctx, "amount")))
                )
            )
        );
    }

    private static int executeItem(CommandContext<CommandSourceStack> ctx, String itemId, int amount) {
        var src = ctx.getSource();
        var player = src.getPlayer();
        if (player == null) { src.sendFailure(MessageUtil.error("commands.neoessentials.general.player_only")); return 0; }

        // Resolve item
        String id = itemId.contains(":") ? itemId : "minecraft:" + itemId;
        Identifier loc = Identifier.tryParse(id);
        if (loc == null) {
            src.sendFailure(MessageUtil.error("commands.neoessentials.item.unknown", itemId));
            return 0;
        }
        var item = BuiltInRegistries.ITEM.getValue(loc);
        if (item == net.minecraft.world.item.Items.AIR && !itemId.equalsIgnoreCase("air")) {
            // Try path-only search
            item = BuiltInRegistries.ITEM.entrySet().stream()
                .filter(e -> e.getKey().identifier().getPath().equals(itemId.toLowerCase()))
                .map(java.util.Map.Entry::getValue)
                .findFirst().orElse(null);
        }
        if (item == null || (item == net.minecraft.world.item.Items.AIR && !itemId.equalsIgnoreCase("air"))) {
            src.sendFailure(MessageUtil.error("commands.neoessentials.item.unknown", itemId));
            return 0;
        }

        int qty = amount > 0 ? amount : item.getDefaultMaxStackSize();
        var stack = new net.minecraft.world.item.ItemStack(item, qty);
        if (!player.getInventory().add(stack)) {
            player.drop(stack, false);
        }
        final String fname = item.getName(stack).getString();
        final int fqty = qty;
        src.sendSuccess(() -> MessageUtil.success("commands.neoessentials.item.given", fqty, fname), false);
        return 1;
    }

    // ── /rtoggle [on|off] ─────────────────────────────────────────────────────
    // Essentials: Commandrtoggle — toggle whether /r replies to the last person
    // who messaged YOU vs the last person YOU messaged.
    private static final Set<UUID> rToggleDisabled = ConcurrentHashMap.newKeySet();

    private static void registerRToggle(CommandDispatcher<CommandSourceStack> d) {
        d.register(Commands.literal("rtoggle")
            .requires(src -> {
                var p = src.getPlayer();
                return p == null || PermissionAPI.hasPermission(p.getUUID(), "neoessentials.rtoggle");
            })
            .executes(ctx -> executeRToggle(ctx, null, null))
            .then(Commands.literal("on").executes(ctx -> executeRToggle(ctx, null, true)))
            .then(Commands.literal("off").executes(ctx -> executeRToggle(ctx, null, false)))
            .then(Commands.argument("target", StringArgumentType.word())
                .suggests((ctx, b) -> SharedSuggestionProvider.suggest(
                    ctx.getSource().getServer().getPlayerNames(), b))
                .requires(src -> src.getPlayer() == null
                    || PermissionAPI.hasPermission(src.getPlayer().getUUID(), "neoessentials.rtoggle.others"))
                .executes(ctx -> executeRToggle(ctx, StringArgumentType.getString(ctx, "target"), null))
                .then(Commands.literal("on")
                    .executes(ctx -> executeRToggle(ctx, StringArgumentType.getString(ctx, "target"), true)))
                .then(Commands.literal("off")
                    .executes(ctx -> executeRToggle(ctx, StringArgumentType.getString(ctx, "target"), false)))
            )
        );
    }

    private static int executeRToggle(CommandContext<CommandSourceStack> ctx, String targetName, Boolean enable) {
        var src = ctx.getSource();
        net.minecraft.server.level.ServerPlayer target = targetName != null
            ? src.getServer().getPlayerList().getPlayerByName(targetName)
            : src.getPlayer();
        if (target == null) {
            if (targetName != null)
                src.sendFailure(MessageUtil.error("commands.neoessentials.general.player_not_found", targetName));
            else
                src.sendFailure(MessageUtil.error("commands.neoessentials.general.player_only"));
            return 0;
        }
        UUID uuid = target.getUUID();
        // disabled = reply to last-messager-of-you (Essentials default true = reply to last you messaged)
        boolean curDisabled = rToggleDisabled.contains(uuid);
        boolean newDisabled = enable != null ? !enable : !curDisabled;
        if (newDisabled) rToggleDisabled.add(uuid); else rToggleDisabled.remove(uuid);
        String label = newDisabled ? "§cdisabled" : "§aenabled";
        boolean isOther = src.getPlayer() == null || !src.getPlayer().getUUID().equals(uuid);
        if (isOther) {
            src.sendSuccess(() -> MessageUtil.success("commands.neoessentials.rtoggle.other",
                target.getName().getString(), label), false);
            target.sendSystemMessage(MessageUtil.info("commands.neoessentials.rtoggle.self", label));
        } else {
            src.sendSuccess(() -> MessageUtil.success("commands.neoessentials.rtoggle.self", label), false);
        }
        return 1;
    }

    /** Returns true if /r should reply to the last sender (disabled = false = use last messaged BY you). */
    public static boolean isRToggleEnabled(UUID uuid) {
        return !rToggleDisabled.contains(uuid);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────
    private static String formatDuration(long ms) {
        long secs = ms / 1000;
        if (secs < 60) return secs + "s";
        long mins = secs / 60; secs %= 60;
        if (mins < 60) return mins + "m" + (secs > 0 ? secs + "s" : "");
        long hours = mins / 60; mins %= 60;
        if (hours < 24) return hours + "h" + (mins > 0 ? mins + "m" : "");
        long days = hours / 24; hours %= 24;
        return days + "d" + (hours > 0 ? hours + "h" : "");
    }
}

