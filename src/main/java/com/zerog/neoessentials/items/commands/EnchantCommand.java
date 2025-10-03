
package com.zerog.neoessentials.items.commands;

import com.zerog.neoessentials.config.GlobalConfig;
import com.zerog.neoessentials.config.ConfigUtil;
import com.zerog.neoessentials.util.MessageUtil;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;

public class EnchantCommand {
    /**
     * Register the /enchant and /enchanthand commands (alias).
     */
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        if (!ConfigUtil.isCommandEnabled("enchant")) return;
        dispatcher.register(
            Commands.literal("enchant")
                .requires(cs -> cs.getEntity() instanceof ServerPlayer)
                .then(Commands.argument("enchantment", net.minecraft.commands.arguments.ResourceLocationArgument.id())
                    .suggests((ctx, builder) -> {
                        // Suggest all available enchantments
                        return net.minecraft.commands.SharedSuggestionProvider.suggestResource(
                            ctx.getSource().getServer().registryAccess()
                                .registryOrThrow(net.minecraft.core.registries.Registries.ENCHANTMENT)
                                .keySet(), builder
                        );
                    })
                    .then(Commands.argument("level", com.mojang.brigadier.arguments.IntegerArgumentType.integer(1))
                        .executes(ctx -> executeEnchant(ctx, false))
                    )
                    .executes(ctx -> executeEnchant(ctx, false)) // Default level 1
                )
                .executes(ctx -> {
                    ctx.getSource().sendFailure(MessageUtil.error("commands.neoessentials.enchant.usage"));
                    return 0;
                })
        );
        dispatcher.register(
            Commands.literal("enchanthand")
                .requires(cs -> cs.getEntity() instanceof ServerPlayer)
                .then(Commands.argument("enchantment", net.minecraft.commands.arguments.ResourceLocationArgument.id())
                    .suggests((ctx, builder) -> {
                        // Suggest all available enchantments
                        return net.minecraft.commands.SharedSuggestionProvider.suggestResource(
                            ctx.getSource().getServer().registryAccess()
                                .registryOrThrow(net.minecraft.core.registries.Registries.ENCHANTMENT)
                                .keySet(), builder
                        );
                    })
                    .then(Commands.argument("level", com.mojang.brigadier.arguments.IntegerArgumentType.integer(1))
                        .executes(ctx -> executeEnchant(ctx, true))
                    )
                    .executes(ctx -> executeEnchant(ctx, true)) // Default level 1
                )
                .executes(ctx -> {
                    ctx.getSource().sendFailure(MessageUtil.error("commands.neoessentials.enchanthand.usage"));
                    return 0;
                })
        );
    }

    /**
     * Execute the enchant command.
     */
    private static int executeEnchant(com.mojang.brigadier.context.CommandContext<CommandSourceStack> ctx, boolean handOnly) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        
        // Check permission
        if (!com.zerog.neoessentials.api.permissions.PermissionAPI.hasPermission(player.getUUID(), "neoessentials.item.enchant")) {
            ctx.getSource().sendFailure(MessageUtil.error("commands.neoessentials.no_permission"));
            return 0;
        }
        
        // Get enchantment from argument
        net.minecraft.resources.ResourceLocation enchantId = net.minecraft.commands.arguments.ResourceLocationArgument.getId(ctx, "enchantment");
        
        // Get level (default to 1 if not provided)
        int levelTemp = 1;
        try {
            levelTemp = com.mojang.brigadier.arguments.IntegerArgumentType.getInteger(ctx, "level");
        } catch (IllegalArgumentException ignored) {
            // Use default level 1
        }
        final int level = levelTemp;
        
        // Get the enchantment from registry
        net.minecraft.core.Registry<Enchantment> enchantRegistry = player.getServer()
            .registryAccess()
            .registryOrThrow(net.minecraft.core.registries.Registries.ENCHANTMENT);
        
        if (!enchantRegistry.containsKey(enchantId)) {
            ctx.getSource().sendFailure(MessageUtil.error("commands.neoessentials.enchant.unknown", enchantId.toString()));
            return 0;
        }
        
        Enchantment enchantment = enchantRegistry.get(enchantId);
        if (enchantment == null) {
            ctx.getSource().sendFailure(MessageUtil.error("commands.neoessentials.enchant.unknown", enchantId.toString()));
            return 0;
        }
        
        // Get item to enchant
        ItemStack stack = player.getMainHandItem();
        if (stack.isEmpty()) {
            ctx.getSource().sendFailure(MessageUtil.error("commands.neoessentials.enchant.no_item"));
            return 0;
        }
        
        // Apply enchantment
        if (applyEnchantment(player, stack, enchantment, level)) {
            ctx.getSource().sendSuccess(() -> MessageUtil.success(
                "commands.neoessentials.enchant.success", 
                enchantId.toString(), 
                level,
                stack.getDisplayName().getString()
            ), false);
            return 1;
        } else {
            ctx.getSource().sendFailure(MessageUtil.error("commands.neoessentials.enchant.failed"));
            return 0;
        }
    }

    /**
     * Apply an enchantment to an item, enforcing the unsafe-enchantments config.
     * @param player The player
     * @param stack The item stack
     * @param enchantment The enchantment
     * @param level The enchantment level
     * @return true if enchantment was applied, false if blocked
     */
    public static boolean applyEnchantment(ServerPlayer player, ItemStack stack, Enchantment enchantment, int level) {
        if (stack == null || enchantment == null) return false;

        // Respect unsafe-enchantments config
        if (!GlobalConfig.isUnsafeEnchantmentsAllowed() && level > enchantment.getMaxLevel()) {
            return false;
        }

        // Copy existing enchantments or start new
        ItemEnchantments ench = stack.get(DataComponents.ENCHANTMENTS);
        Object2IntOpenHashMap<Holder<Enchantment>> enchMap = new Object2IntOpenHashMap<>();
        if (ench != null) {
            for (Object2IntMap.Entry<Holder<Enchantment>> entry : ench.entrySet()) {
                enchMap.put(entry.getKey(), entry.getIntValue());
            }
        }

        // Try to resolve the registry key for the enchantment
        ResourceLocation id = null;
        try {
            // Try NeoForge then Forge, fallback to null
            Class<?> forgeRegistries = Class.forName("net.neoforged.neoforge.registries.ForgeRegistries");
            Object enchantments = forgeRegistries.getField("ENCHANTMENTS").get(null);
            id = (ResourceLocation) enchantments.getClass().getMethod("getKey", Object.class).invoke(enchantments, enchantment);
        } catch (Throwable t) {
            try {
                Class<?> forgeRegistries = Class.forName("net.minecraftforge.registries.ForgeRegistries");
                Object enchantments = forgeRegistries.getField("ENCHANTMENTS").get(null);
                id = (ResourceLocation) enchantments.getClass().getMethod("getKey", Object.class).invoke(enchantments, enchantment);
            } catch (Throwable ignored) {}
        }
        if (id == null) return false;

        Holder<Enchantment> holder = player.getServer()
            .registryAccess()
            .registryOrThrow(Registries.ENCHANTMENT)
            .getHolderOrThrow(ResourceKey.create(Registries.ENCHANTMENT, id));

        // Apply / replace
        enchMap.put(holder, level);

        // Set updated enchantments using the most compatible constructor
        try {
            ItemEnchantments updated = ItemEnchantments.class.getConstructor(Object2IntOpenHashMap.class, boolean.class)
                .newInstance(enchMap, true);
            stack.set(DataComponents.ENCHANTMENTS, updated);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
}
