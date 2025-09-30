package com.zerog.neoessentials.utils;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.enchantment.Enchantment;

public class EnchantmentUtils {
    public static Holder<Enchantment> getEnchantment(MinecraftServer server, String namespace, String path) {
    var registry = server.registryAccess().registryOrThrow(Registries.ENCHANTMENT);
    ResourceLocation id = ResourceLocation.parse(namespace + ":" + path);
    ResourceKey<Enchantment> key = ResourceKey.create(Registries.ENCHANTMENT, id);
    return registry.getHolderOrThrow(key);
    }
}
