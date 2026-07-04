package com.zerog.neoessentials.util.commands;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;

/**
 * Utility class for common command helper functions.
 * Reduces code duplication across command implementations.
 */
public class CommandUtil {
    
    /**
     * Get user-friendly world/dimension name from level
     * 
     * @param level The level to get the name from
     * @return User-friendly world name
     */
    public static String getWorldName(Level level) {
        return getWorldName(level.dimension().identifier().toString());
    }
    
    /**
     * Get user-friendly world/dimension name from dimension key string
     * 
     * @param dimensionKey The dimension key (e.g., "minecraft:overworld")
     * @return User-friendly world name
     */
    public static String getWorldName(String dimensionKey) {
        return switch (dimensionKey) {
            case "minecraft:overworld" -> "Overworld";
            case "minecraft:the_nether" -> "The Nether";
            case "minecraft:the_end" -> "The End";
            default -> dimensionKey;
        };
    }
    
    /**
     * Get dimension name/key from level
     * 
     * @param level The level to get the dimension name from
     * @return Dimension key (e.g., "minecraft:overworld")
     */
    public static String getDimensionName(Level level) {
        return level.dimension().identifier().toString();
    }
    
    /**
     * Get biome name from biome, level, and position
     * 
     * @param biome The biome
     * @param level The level
     * @param pos The position
     * @return Formatted biome name
     */
    public static String getBiomeName(Biome biome, Level level, BlockPos pos) {
        Identifier biomeKey = level.registryAccess()
            .lookupOrThrow(net.minecraft.core.registries.Registries.BIOME)
            .getKey(biome);
        
        if (biomeKey != null) {
            // Remove minecraft: prefix and format nicely
            return formatBiomeName(biomeKey.toString());
        }
        return "Unknown";
    }
    
    /**
     * Format biome name for display (removes prefix, capitalizes)
     * 
     * @param biomeName Raw biome name
     * @return Formatted biome name
     */
    public static String formatBiomeName(String biomeName) {
        return biomeName.replaceAll("minecraft:", "")
                       .replaceAll("_", " ")
                       .trim();
    }
    
    /**
     * Get cardinal direction from yaw rotation
     * 
     * @param yaw Player's yaw rotation
     * @return Cardinal direction (N, NE, E, SE, S, SW, W, NW)
     */
    public static String getCardinalDirection(float yaw) {
        // Normalize yaw to 0-360 range
        yaw = ((yaw % 360) + 360) % 360;
        
        if (yaw >= 315 || yaw < 45) return "South";
        if (yaw >= 45 && yaw < 135) return "West";
        if (yaw >= 135 && yaw < 225) return "North";
        if (yaw >= 225 && yaw < 315) return "East";
        return "South"; // fallback
    }
    
    /**
     * Get cardinal direction from relative position
     * 
     * @param deltaX X offset
     * @param deltaZ Z offset
     * @return Cardinal direction (N, NE, E, SE, S, SW, W, NW)
     */
    public static String getDirectionFromOffset(double deltaX, double deltaZ) {
        double angle = Math.toDegrees(Math.atan2(deltaZ, deltaX));
        angle = ((angle % 360) + 360) % 360; // Normalize to 0-360
        
        // Convert to minecraft coordinate system (South = 0°)
        angle = (angle + 90) % 360;
        
        if (angle >= 315 || angle < 45) return "South";
        if (angle >= 45 && angle < 135) return "West";
        if (angle >= 135 && angle < 225) return "North";
        if (angle >= 225 && angle < 315) return "East";
        return "South";
    }
    
    /**
     * Get simple cardinal direction from relative position (8-way)
     * 
     * @param relativeX X offset
     * @param relativeZ Z offset
     * @return Cardinal direction (E, SE, S, SW, W, NW, N, NE)
     */
    public static String getSimpleDirection(double relativeX, double relativeZ) {
        double angle = Math.atan2(relativeZ, relativeX) * 180.0 / Math.PI;
        
        // Normalize angle to 0-360
        if (angle < 0) angle += 360;
        
        // Convert to cardinal directions
        if (angle >= 337.5 || angle < 22.5) return "E";
        else if (angle >= 22.5 && angle < 67.5) return "SE";
        else if (angle >= 67.5 && angle < 112.5) return "S";
        else if (angle >= 112.5 && angle < 157.5) return "SW";
        else if (angle >= 157.5 && angle < 202.5) return "W";
        else if (angle >= 202.5 && angle < 247.5) return "NW";
        else if (angle >= 247.5 && angle < 292.5) return "N";
        else return "NE";
    }
    
    /**
     * Format distance in a readable way
     * 
     * @param distance Distance in blocks
     * @param decimals Number of decimal places
     * @return Formatted distance string
     */
    public static String formatDistance(double distance, int decimals) {
        String format = "%." + decimals + "f";
        return String.format(format, distance);
    }
    
    /**
     * Check if a dimension is the Overworld
     * 
     * @param level The level to check
     * @return true if Overworld
     */
    public static boolean isOverworld(Level level) {
        return level.dimension() == Level.OVERWORLD;
    }
    
    /**
     * Check if a dimension is the Nether
     * 
     * @param level The level to check
     * @return true if Nether
     */
    public static boolean isNether(Level level) {
        return level.dimension() == Level.NETHER;
    }
    
    /**
     * Check if a dimension is the End
     * 
     * @param level The level to check
     * @return true if End
     */
    public static boolean isEnd(Level level) {
        return level.dimension() == Level.END;
    }
    
    /**
     * Convert Overworld coordinates to Nether coordinates
     * 
     * @param overworldCoord Overworld coordinate (X or Z)
     * @return Nether coordinate
     */
    public static int overworldToNether(int overworldCoord) {
        return overworldCoord / 8;
    }
    
    /**
     * Convert Nether coordinates to Overworld coordinates
     * 
     * @param netherCoord Nether coordinate (X or Z)
     * @return Overworld coordinate
     */
    public static int netherToOverworld(int netherCoord) {
        return netherCoord * 8;
    }
    
    // Private constructor to prevent instantiation
    private CommandUtil() {
        throw new UnsupportedOperationException("Utility class");
    }
}
