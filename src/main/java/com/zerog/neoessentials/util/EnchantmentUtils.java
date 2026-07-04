package com.zerog.neoessentials.util;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.enchantment.Enchantment;
import java.util.Optional;

/**
 * Utility class for enchantment-related operations
 */
@SuppressWarnings("unused") // Public API utility class
public class EnchantmentUtils {
    
    /**
     * Get an enchantment holder by namespace and path
     * @param server The minecraft server instance
     * @param namespace The namespace (usually "minecraft")
     * @param path The enchantment path (e.g. "sharpness")
     * @return The enchantment holder
     * @throws IllegalArgumentException if enchantment not found
     */
    public static Holder<Enchantment> getEnchantment(MinecraftServer server, String namespace, String path) {
        var registry = server.registryAccess().registryOrThrow(Registries.ENCHANTMENT);
        Identifier id = Identifier.fromNamespaceAndPath(namespace, path);
        ResourceKey<Enchantment> key = ResourceKey.create(Registries.ENCHANTMENT, id);
        return registry.getHolderOrThrow(key);
    }
    
    /**
     * Safely get an enchantment holder by namespace and path
     * @param server The minecraft server instance
     * @param namespace The namespace (usually "minecraft")
     * @param path The enchantment path (e.g. "sharpness")
     * @return Optional containing the enchantment holder if found
     */
    public static Optional<Holder<Enchantment>> getEnchantmentSafely(MinecraftServer server, String namespace, String path) {
        try {
            var registry = server.registryAccess().registryOrThrow(Registries.ENCHANTMENT);
            Identifier id = Identifier.fromNamespaceAndPath(namespace, path);
            ResourceKey<Enchantment> key = ResourceKey.create(Registries.ENCHANTMENT, id);
            return registry.getHolder(key).map(holder -> holder);
        } catch (Exception e) {
            return Optional.empty();
        }
    }
    
    /**
     * Get an enchantment by Identifier
     * @param server The minecraft server instance
     * @param location The resource location of the enchantment
     * @return Optional containing the enchantment holder if found
     */
    public static Optional<Holder<Enchantment>> getEnchantment(MinecraftServer server, Identifier location) {
        try {
            var registry = server.registryAccess().registryOrThrow(Registries.ENCHANTMENT);
            ResourceKey<Enchantment> key = ResourceKey.create(Registries.ENCHANTMENT, location);
            return registry.getHolder(key).map(holder -> holder);
        } catch (Exception e) {
            return Optional.empty();
        }
    }
}
