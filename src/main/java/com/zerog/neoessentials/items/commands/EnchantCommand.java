
package com.zerog.neoessentials.items.commands;

import com.zerog.neoessentials.config.GlobalConfig;
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
        dispatcher.register(
            Commands.literal("enchant")
                .requires(cs -> cs.getEntity() instanceof ServerPlayer)
                .executes(ctx -> {
                    ServerPlayer player = ctx.getSource().getPlayerOrException();
                    if (!com.zerog.neoessentials.api.permissions.PermissionAPI.hasPermission(player.getUUID(), "neoessentials.item.enchant")) {
                        ctx.getSource().sendFailure(net.minecraft.network.chat.Component.translatable("commands.neoessentials.no_permission"));
                        return 0;
                    }
                    ctx.getSource().sendFailure(net.minecraft.network.chat.Component.translatable("commands.neoessentials.enchant.usage"));
                    return 0;
                })
        );
        dispatcher.register(
            Commands.literal("enchanthand")
                .requires(cs -> cs.getEntity() instanceof ServerPlayer)
                .executes(ctx -> {
                    ServerPlayer player = ctx.getSource().getPlayerOrException();
                    if (!com.zerog.neoessentials.api.permissions.PermissionAPI.hasPermission(player.getUUID(), "neoessentials.item.enchant")) {
                        ctx.getSource().sendFailure(net.minecraft.network.chat.Component.translatable("commands.neoessentials.no_permission"));
                        return 0;
                    }
                    ctx.getSource().sendFailure(net.minecraft.network.chat.Component.translatable("commands.neoessentials.enchanthand.usage"));
                    return 0;
                })
        );
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
