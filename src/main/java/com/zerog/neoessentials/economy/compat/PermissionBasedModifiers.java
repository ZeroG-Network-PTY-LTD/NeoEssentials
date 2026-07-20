package com.zerog.neoessentials.economy.compat;
import com.zerog.neoessentials.api.permissions.PermissionAPI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.math.BigDecimal;
import java.util.UUID;
/**
 * Permission-node based economy modifiers.
 *
 * Works with ANY permission backend (NeoEssentials built-in, FTB Ranks, LuckPerms, etc.)
 * because it uses the unified PermissionAPI.
 *
 * Permission nodes:
 *   neoessentials.economy.sellmultiplier.300  -> 3.0x sell multiplier
 *   neoessentials.economy.sellmultiplier.250  -> 2.5x
 *   neoessentials.economy.sellmultiplier.200  -> 2.0x
 *   neoessentials.economy.sellmultiplier.175  -> 1.75x
 *   neoessentials.economy.sellmultiplier.150  -> 1.5x
 *   neoessentials.economy.sellmultiplier.125  -> 1.25x
 *   neoessentials.economy.taxexempt           -> tax rate = 0%
 *   neoessentials.economy.nopaycooldown       -> bypass /pay cooldown
 *   neoessentials.economy.maxbalance.1000000  -> max balance 1,000,000 (any 7-digit value)
 *   neoessentials.economy.paylimit.100000     -> max /pay 100,000
 */
public class PermissionBasedModifiers implements EconomyModifierProvider {
    private static final Logger LOGGER = LoggerFactory.getLogger(PermissionBasedModifiers.class);
    // Supported sell-multiplier tiers (checked highest-first)
    private static final int[] SELL_MULTIPLIER_TIERS = {300, 250, 200, 175, 150, 125};
    @Override
    public String getName() { return "PermissionBased"; }

    /**
     * All permission checks in this class go through here rather than calling
     * {@link PermissionAPI#hasPermission} directly, so that whether OP-bypass extends to
     * these graded/opt-in economy bonuses is a single config-driven decision
     * ({@code permissions.opsBypassEconomyModifierPermissions}, default {@code false}) instead
     * of being hardcoded per node.
     */
    private static boolean check(UUID player, String node) {
        boolean allowOpBypass = com.zerog.neoessentials.config.ConfigManager.getInstance()
            .isOpsBypassEconomyModifierPermissionsEnabled();
        return allowOpBypass
            ? PermissionAPI.hasPermission(player, node)
            : PermissionAPI.hasPermissionStrict(player, node);
    }

    @Override
    public double getSellMultiplier(UUID player) {
        for (int tier : SELL_MULTIPLIER_TIERS) {
            if (check(player, "neoessentials.economy.sellmultiplier." + tier)) {
                return tier / 100.0;
            }
        }
        return 0.0; // no override
    }
    @Override
    public boolean isTaxExempt(UUID player) {
        return check(player, "neoessentials.economy.taxexempt");
    }
    @Override
    public boolean hasNoPayCooldown(UUID player) {
        return check(player, "neoessentials.economy.nopaycooldown");
    }
    @Override
    public BigDecimal getMaxBalance(UUID player) {
        // Check common max-balance tiers
        long[] maxBalTiers = {
            100_000_000L, 50_000_000L, 10_000_000L, 5_000_000L, 1_000_000L, 500_000L
        };
        for (long tier : maxBalTiers) {
            if (check(player, "neoessentials.economy.maxbalance." + tier)) {
                return BigDecimal.valueOf(tier);
            }
        }
        return null; // no override
    }
    @Override
    public BigDecimal getPayLimit(UUID player) {
        // Common pay-limit tiers
        long[] tiers = {1_000_000L, 500_000L, 100_000L, 50_000L, 25_000L};
        for (long tier : tiers) {
            if (check(player, "neoessentials.economy.paylimit." + tier)) {
                return BigDecimal.valueOf(tier);
            }
        }
        return null;
    }
}
