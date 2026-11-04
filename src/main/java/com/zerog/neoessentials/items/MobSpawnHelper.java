package com.zerog.neoessentials.items;

import com.zerog.neoessentials.api.permissions.PermissionAPI;
import com.zerog.neoessentials.config.ConfigManager;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;

import java.util.List;
import java.util.UUID;

/**
 * Helper class for managing /spawnmob entity spawning with permission checks and blacklist
 * validation. Same shape as {@link ItemSpawnHelper} - both are opt-in (disabled by default,
 * see spawnmobSettings.permission-based-mob-spawn) so existing servers relying on the base
 * "neoessentials.spawnmob" permission alone see no behavior change.
 */
public class MobSpawnHelper {

    /**
     * Check if a player can spawn a specific entity type via /spawnmob.
     * Validates against blacklist and (if enabled) per-entity permission requirements.
     *
     * @param player the player attempting to spawn
     * @param entityType the entity type to spawn
     * @return SpawnResult with success status and error message if failed
     */
    public static SpawnResult canSpawnMob(UUID player, EntityType<?> entityType) {
        ResourceLocation entityId = BuiltInRegistries.ENTITY_TYPE.getKey(entityType);
        String entityIdString = entityId.toString();

        List<String> blacklist = ConfigManager.getMobSpawnBlacklist();
        if (blacklist.contains(entityIdString)) {
            return SpawnResult.failure("Entity '" + entityIdString + "' is blacklisted and cannot be spawned");
        }

        if (ConfigManager.isPermissionBasedMobSpawn()) {
            if (!PermissionAPI.hasPermission(player, "neoessentials.spawnmob.spawn")) {
                return SpawnResult.failure("You don't have permission to spawn mobs");
            }

            // Per-entity permission (format: neoessentials.spawnmob.spawn.<namespace>.<entity>)
            String specificPerm = "neoessentials.spawnmob.spawn." + entityIdString.replace(":", ".");
            if (!PermissionAPI.hasPermission(player, specificPerm)) {
                // Wildcard for namespace (neoessentials.spawnmob.spawn.minecraft.*)
                String namespacePerm = "neoessentials.spawnmob.spawn." + entityId.getNamespace() + ".*";
                if (!PermissionAPI.hasPermission(player, namespacePerm)) {
                    return SpawnResult.failure("You don't have permission to spawn '" + entityIdString + "'");
                }
            }
        }

        return SpawnResult.success();
    }

    /** Result of a spawn permission check. */
    public static class SpawnResult {
        private final boolean success;
        private final String errorMessage;

        private SpawnResult(boolean success, String errorMessage) {
            this.success = success;
            this.errorMessage = errorMessage;
        }

        public static SpawnResult success() {
            return new SpawnResult(true, null);
        }

        public static SpawnResult failure(String message) {
            return new SpawnResult(false, message);
        }

        public boolean isSuccess() {
            return success;
        }

        public String getErrorMessage() {
            return errorMessage;
        }
    }
}
