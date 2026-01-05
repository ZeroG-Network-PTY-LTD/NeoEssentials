package com.zerog.neoessentials.util;

import net.minecraft.resources.ResourceLocation;

/**
 * Helper class to safely create ResourceLocation instances across different Minecraft versions.
 * This prevents classloading issues during JSON deserialization.
 */
public class ResourceLocationHelper {

    /**
     * Creates a ResourceLocation from namespace and path.
     * This method uses the modern API (1.21+) and is compatible across versions.
     *
     * @param namespace The namespace (e.g., "minecraft")
     * @param path The path (e.g., "diamond_sword")
     * @return A new ResourceLocation instance
     */
    public static ResourceLocation create(String namespace, String path) {
        // For Minecraft 1.21+ (including 1.21.11)
        // This method should work across all 1.21.x versions
        return ResourceLocation.fromNamespaceAndPath(namespace, path);
    }

    /**
     * Parses a ResourceLocation from a string in format "namespace:path" or just "path".
     *
     * @param locationString The location string
     * @return A new ResourceLocation instance
     */
    public static ResourceLocation parse(String locationString) {
        if (locationString.contains(":")) {
            String[] parts = locationString.split(":", 2);
            return create(parts[0], parts[1]);
        } else {
            return create("minecraft", locationString);
        }
    }
}

