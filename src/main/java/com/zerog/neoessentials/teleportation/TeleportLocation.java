package com.zerog.neoessentials.teleportation;

import com.google.gson.JsonObject;
import com.zerog.neoessentials.logging.LogCategory;
import com.zerog.neoessentials.logging.NeoLog;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import net.neoforged.neoforge.server.ServerLifecycleHooks;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Represents a teleportation location with world, position, and rotation data
 */
public class TeleportLocation {
    private static final Logger LOGGER = LoggerFactory.getLogger(TeleportLocation.class);
    private final String worldName;
    private final double x;
    private final double y;
    private final double z;
    private final float yaw;
    private final float pitch;
    private final long timestamp;
    private final String createdBy;
    
    public TeleportLocation(String worldName, double x, double y, double z, float yaw, float pitch, String createdBy) {
        this.worldName = worldName;
        this.x = x;
        this.y = y;
        this.z = z;
        // Validate and sanitize rotation values to prevent NaN propagation
        this.yaw = Float.isNaN(yaw) || Float.isInfinite(yaw) ? 0.0f : yaw;
        this.pitch = Float.isNaN(pitch) || Float.isInfinite(pitch) ? 0.0f : pitch;
        this.timestamp = System.currentTimeMillis();
        this.createdBy = createdBy;
    }
    
    @SuppressWarnings("resource") // Level is managed by Minecraft, not closeable by us
    public TeleportLocation(ServerPlayer player) {
        this(player.level().dimension().identifier().toString(),
             player.getX(),
             player.getY(),
             player.getZ(),
             // Sanitize rotation inline - NaN from player edge cases (respawn, dimension change, etc.)
             (Float.isNaN(player.getYRot()) || Float.isInfinite(player.getYRot())) ? 0.0f : player.getYRot(),
             (Float.isNaN(player.getXRot()) || Float.isInfinite(player.getXRot())) ? 0.0f : player.getXRot(),
             player.getName().getString());
    }
    
    public TeleportLocation(ServerLevel level, BlockPos pos, float yaw, float pitch, String createdBy) {
        this(level.dimension().identifier().toString(),
             pos.getX() + 0.5,
             pos.getY(),
             pos.getZ() + 0.5,
             yaw,
             pitch,
             createdBy);
    }
    
    // Getters
    public String getWorldName() { return worldName; }
    public double getX() { return x; }
    public double getY() { return y; }
    public double getZ() { return z; }
    public float getYaw() { return yaw; }
    public float getPitch() { return pitch; }
    public long getTimestamp() { return timestamp; }
    public String getCreatedBy() { return createdBy; }
    
    /**
     * Get the ServerLevel for this location
     */
    public ServerLevel getLevel() {
        try {
            Identifier worldKey;

            // Parse Identifier manually to avoid classloading issues
            if (worldName.contains(":")) {
                String[] parts = worldName.split(":", 2);
                worldKey = Identifier.fromNamespaceAndPath(parts[0], parts[1]);
            } else {
                worldKey = Identifier.fromNamespaceAndPath("minecraft", worldName);
            }

            var server = ServerLifecycleHooks.getCurrentServer();
            if (server == null) return null;

            return server.getLevel(
                net.minecraft.resources.ResourceKey.create(
                    net.minecraft.core.registries.Registries.DIMENSION, 
                    worldKey
                )
            );
        } catch (Exception e) {
            NeoLog.debug(LOGGER, LogCategory.TELEPORTATION, "getLevel() failed to resolve world '{}': {}", worldName, e.getMessage());
            return null;
        }
    }

    /**
     * Check if this location is safe for teleportation.
     * A location is safe when:
     *  - the chunk is loaded
     *  - the block at feet-level and head-level are passable (air / non-solid)
     *  - the block below feet is solid and not dangerous (lava, fire, cactus…)
     */
    public boolean isSafe() {
        ServerLevel level = getLevel();
        if (level == null) return false;

        BlockPos pos = new BlockPos((int) Math.floor(x), (int) Math.floor(y), (int) Math.floor(z));

        if (!level.isLoaded(pos)) return false;

        BlockPos ground = pos.below();
        BlockPos head   = pos.above();

        net.minecraft.world.level.block.state.BlockState groundState = level.getBlockState(ground);
        net.minecraft.world.level.block.state.BlockState feetState   = level.getBlockState(pos);
        net.minecraft.world.level.block.state.BlockState headState   = level.getBlockState(head);

        // Ground must be solid: has a full (non-empty) collision shape and is not air
        if (groundState.isAir() || groundState.getCollisionShape(level, ground).isEmpty()) return false;

        // Feet and head must be passable
        if (!feetState.getCollisionShape(level, pos).isEmpty() &&
            !feetState.isAir()) return false;
        if (!headState.getCollisionShape(level, head).isEmpty() &&
            !headState.isAir()) return false;

        // Reject dangerous ground / feet blocks
        if (isDangerous(level, ground)) return false;
        if (isDangerous(level, pos))    return false;

        return true;
    }

    /** Returns true if the block at pos is dangerous to stand on/in. */
    private boolean isDangerous(ServerLevel level, BlockPos pos) {
        net.minecraft.world.level.block.Block block = level.getBlockState(pos).getBlock();
        return block == net.minecraft.world.level.block.Blocks.LAVA
            || block == net.minecraft.world.level.block.Blocks.WATER
            || block == net.minecraft.world.level.block.Blocks.FIRE
            || block == net.minecraft.world.level.block.Blocks.SOUL_FIRE
            || block == net.minecraft.world.level.block.Blocks.MAGMA_BLOCK
            || block == net.minecraft.world.level.block.Blocks.CACTUS
            || block == net.minecraft.world.level.block.Blocks.SWEET_BERRY_BUSH
            || block == net.minecraft.world.level.block.Blocks.WITHER_ROSE
            || block == net.minecraft.world.level.block.Blocks.NETHER_PORTAL
            || block == net.minecraft.world.level.block.Blocks.CAMPFIRE
            || block == net.minecraft.world.level.block.Blocks.SOUL_CAMPFIRE
            || block == net.minecraft.world.level.block.Blocks.POWDER_SNOW;
    }

    /**
     * Find a safe location near this position.
     * Strategy:
     *  1. Try the original location first.
     *  2. Scan a ±16 Y window centered on the destination Y at the same X,Z column.
     *     This finds a spot close to the actual destination (e.g. nether interior) and
     *     prevents the top-down scan from skipping past it to the nether roof or an
     *     underground cave below the ocean.
     *  3. Top-down scan at the same X,Z — capped to logical height so it never reaches
     *     above the nether ceiling (Y>127) or equivalent dimension caps.
     *  4. Expand in XZ radius up to 16, scanning a ±8 Y window at each column.
     */
    public TeleportLocation findSafeLocation() {
        ServerLevel level = getLevel();
        if (level == null) return null;

        if (isSafe()) return this;

        int ix = (int) Math.floor(x);
        int iy = (int) Math.floor(y);
        int iz = (int) Math.floor(z);

        // Step 2: Y-neighbourhood scan at same X,Z — prefer landing close to destination Y
        // Scan alternating: 0, +1, -1, +2, -2, … up to ±16 Y offset.
        for (int dy = 0; dy <= 16; dy++) {
            if (dy == 0) {
                // Already tried by isSafe() above; skip to avoid duplicate check
                continue;
            }
            for (int sign : new int[]{1, -1}) {
                BlockPos candidate = new BlockPos(ix, iy + sign * dy, iz);
                if (candidate.getY() < com.zerog.neoessentials.util.LevelHeightCompat.minBuildHeight(level) + 1 ||
                    candidate.getY() > com.zerog.neoessentials.util.LevelHeightCompat.maxBuildHeight(level) - 2) continue;
                TeleportLocation loc = new TeleportLocation(level, candidate, yaw, pitch, createdBy);
                if (loc.isSafe()) return loc;
            }
        }

        // Step 3: top-down column scan, capped to logical height to avoid nether roof
        TeleportLocation col = scanColumnTopDown(level, ix, iz);
        if (col != null) return col;

        // Step 4: expanding XZ radius
        for (int radius = 1; radius <= 16; radius++) {
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    if (Math.abs(dx) != radius && Math.abs(dz) != radius) continue;
                    // Quick ±8 Y scan around the stored Y
                    int bx = ix + dx, bz = iz + dz;
                    int startY = (int) Math.floor(y);
                    for (int dy = -8; dy <= 8; dy++) {
                        BlockPos testPos = new BlockPos(bx, startY + dy, bz);
                        TeleportLocation testLoc = new TeleportLocation(level, testPos, yaw, pitch, createdBy);
                        if (testLoc.isSafe()) return testLoc;
                    }
                }
            }
        }

        return null;
    }

    /**
     * Scan from just below the dimension's logical ceiling downward at the given X,Z to
     * find a safe surface spot.
     *
     * <p>Uses {@code level.dimensionType().logicalHeight()} rather than
     * {@code com.zerog.neoessentials.util.LevelHeightCompat.maxBuildHeight(level)} so that in the Nether
     * (logicalHeight=128, maxBuildHeight=256) the scan never starts above the bedrock
     * ceiling and therefore never finds Y=128 (directly above the bedrock roof) as a
     * valid landing spot.</p>
     */
    private TeleportLocation scanColumnTopDown(ServerLevel level, int bx, int bz) {
        // Cap to logical height — prevents landing above the nether ceiling (Y≥128)
        // or above any other dimension's intended play space.
        int logicalHeight = level.dimensionType().logicalHeight();
        int maxY = Math.min(com.zerog.neoessentials.util.LevelHeightCompat.maxBuildHeight(level) - 2, logicalHeight - 1);
        int minY = com.zerog.neoessentials.util.LevelHeightCompat.minBuildHeight(level) + 1;
        for (int by = maxY; by >= minY; by--) {
            BlockPos candidate = new BlockPos(bx, by, bz);
            TeleportLocation loc = new TeleportLocation(level, candidate, yaw, pitch, createdBy);
            if (loc.isSafe()) return loc;
        }
        return null;
    }
    
    /**
     * Get formatted coordinates string
     */
    @SuppressWarnings("unused") // Public API method - may be used by other plugins/mods
    public String getCoordinatesString() {
        return String.format("%.1f, %.1f, %.1f", x, y, z);
    }
    
    /**
     * Get formatted location string with world
     */
    public String getLocationString() {
        return String.format("%s (%.1f, %.1f, %.1f)", getWorldDisplayName(), x, y, z);
    }
    
    /**
     * Get display name for world
     */
    public String getWorldDisplayName() {
        if (worldName.contains("overworld")) return "Overworld";
        if (worldName.contains("nether")) return "Nether";
        if (worldName.contains("end")) return "End";
        return worldName;
    }
    
    /**
     * Calculate distance to another location (same world only)
     */
    public double distanceTo(TeleportLocation other) {
        if (!worldName.equals(other.worldName)) return Double.MAX_VALUE;
        
        double dx = x - other.x;
        double dy = y - other.y;
        double dz = z - other.z;
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }
    
    /**
     * Serialize to JSON
     */
    public JsonObject toJson() {
        JsonObject json = new JsonObject();
        json.addProperty("world", worldName);
        json.addProperty("x", x);
        json.addProperty("y", y);
        json.addProperty("z", z);
        json.addProperty("yaw", yaw);
        json.addProperty("pitch", pitch);
        json.addProperty("timestamp", timestamp);
        json.addProperty("createdBy", createdBy);
        return json;
    }
    
    /**
     * Convert to storable string format (JSON) for player data
     */
    @SuppressWarnings("unused") // Public API method - may be used by other plugins/mods
    public String toLocationString() {
        return toJson().toString();
    }
    
    /**
     * Deserialize from JSON
     */
    public static TeleportLocation fromJson(JsonObject json) {
        try {
            String world = json.get("world").getAsString();
            double x = json.get("x").getAsDouble();
            double y = json.get("y").getAsDouble();
            double z = json.get("z").getAsDouble();

            // Parse rotation values with validation
            float yaw = 0.0f;
            float pitch = 0.0f;

            if (json.has("yaw")) {
                yaw = json.get("yaw").getAsFloat();
                // Sanitize NaN/Infinite values that might be in saved data
                if (Float.isNaN(yaw) || Float.isInfinite(yaw)) {
                    yaw = 0.0f;
                }
            }

            if (json.has("pitch")) {
                pitch = json.get("pitch").getAsFloat();
                // Sanitize NaN/Infinite values that might be in saved data
                if (Float.isNaN(pitch) || Float.isInfinite(pitch)) {
                    pitch = 0.0f;
                }
            }

            String createdBy = json.has("createdBy") ? json.get("createdBy").getAsString() : "Unknown";
            
            // Timestamp is set in constructor, but we can preserve the original if present
            return new TeleportLocation(world, x, y, z, yaw, pitch, createdBy);
        } catch (Exception e) {
            NeoLog.debug(LOGGER, LogCategory.TELEPORTATION, "fromJson: failed to parse TeleportLocation from JSON: {}", e.getMessage());
            return null;
        }
    }
    
    /**
     * Parse location from string format (JSON or simplified format)
     */
    public static TeleportLocation fromLocationString(String locationString) {
        if (locationString == null || locationString.trim().isEmpty()) {
            return null;
        }
        
        try {
            // Try to parse as JSON first (preferred format)
            if (locationString.trim().startsWith("{")) {
                JsonObject json = com.google.gson.JsonParser.parseString(locationString).getAsJsonObject();
                return fromJson(json);
            }
            
            // Try to parse simplified format: "world,x,y,z,yaw,pitch"
            String[] parts = locationString.split(",");
            if (parts.length >= 4) {
                String world = parts[0].trim();
                double x = Double.parseDouble(parts[1].trim());
                double y = Double.parseDouble(parts[2].trim());
                double z = Double.parseDouble(parts[3].trim());
                float yaw = parts.length > 4 ? Float.parseFloat(parts[4].trim()) : 0.0f;
                float pitch = parts.length > 5 ? Float.parseFloat(parts[5].trim()) : 0.0f;
                
                return new TeleportLocation(world, x, y, z, yaw, pitch, "System");
            }
            
        } catch (Exception e) {
            NeoLog.debug(LOGGER, LogCategory.TELEPORTATION, "fromLocationString: invalid location format '{}': {}", locationString, e.getMessage());
        }

        return null;
    }
    
    @Override
    public String toString() {
        return String.format("TeleportLocation{world=%s, x=%.2f, y=%.2f, z=%.2f, yaw=%.1f, pitch=%.1f}", 
                           worldName, x, y, z, yaw, pitch);
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof TeleportLocation other)) return false;

        return worldName.equals(other.worldName) &&
               Math.abs(x - other.x) < 0.1 &&
               Math.abs(y - other.y) < 0.1 &&
               Math.abs(z - other.z) < 0.1;
    }
    
    @Override
    public int hashCode() {
        return worldName.hashCode() ^ Double.hashCode(x) ^ Double.hashCode(y) ^ Double.hashCode(z);
    }
}