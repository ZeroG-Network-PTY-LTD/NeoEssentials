package com.zerog.neoessentials.vault;

import com.zerog.neoessentials.config.ConfigManager;
import com.zerog.neoessentials.vault.api.VaultServiceRegistry;
import com.zerog.neoessentials.vault.api.VaultServiceRegistry.ServicePriority;
import com.zerog.neoessentials.vault.impl.NeoEssentialsChat;
import com.zerog.neoessentials.vault.impl.NeoEssentialsEconomy;
import com.zerog.neoessentials.vault.impl.NeoEssentialsPermission;
import com.zerog.neoessentials.vault.impl.SGEconomyAdapter;
import com.zerog.neoessentials.vault.impl.VaultShopEconomyAdapter;
import com.zerog.neoessentials.shop.api.ShopEconomyRegistry;
import com.zerog.neoessentials.shop.api.NeoEssentialsShopEconomy;
import net.neoforged.fml.ModList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * NeoEssentials Vault sub-system manager.
 * <p>
 * Initialised at server start.  Registers the three built-in NeoEssentials
 * providers (Economy, Permission, Chat) into {@link VaultServiceRegistry} so
 * that other NeoForge mods can access them via the Vault API without depending
 * on any specific implementation.
 *
 * <h3>Usage for other mod developers</h3>
 * <pre>{@code
 * // Economy
 * VaultServiceRegistry.getInstance().getEconomy().ifPresent(eco -> {
 *     eco.depositPlayer(playerId, 100.0);
 *     double bal = eco.getBalance(playerId);
 * });
 *
 * // Permissions
 * VaultServiceRegistry.getInstance().getPermission().ifPresent(perm -> {
 *     boolean has = perm.playerHas(playerId, "yourmod.use");
 *     perm.playerAddGroup(playerId, "vip");
 * });
 *
 * // Chat metadata (prefix/suffix)
 * VaultServiceRegistry.getInstance().getChat().ifPresent(chat -> {
 *     String prefix = chat.getPlayerPrefix(playerId);
 *     String groupPrefix = chat.getGroupPrefix("admin");
 * });
 * }</pre>
 *
 * <h3>Registering your own provider (higher priority overrides built-in)</h3>
 * <pre>{@code
 * VaultServiceRegistry.getInstance().registerEconomy(
 *     myEconomyImpl, ServicePriority.HIGH, "mymod");
 * }</pre>
 */
public class VaultManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(VaultManager.class);
    private static boolean initialised = false;

    /** Called once during server startup (after permission system is ready). */
    public static void initialize() {
        if (initialised) {
            LOGGER.warn("[VaultAPI] VaultManager.initialize() called more than once — skipping");
            return;
        }

        LOGGER.info("[VaultAPI] Initialising NeoEssentials Vault API...");

        VaultServiceRegistry registry = VaultServiceRegistry.getInstance();

        // Register built-in Economy provider
        try {
            NeoEssentialsEconomy economy = new NeoEssentialsEconomy();
            registry.registerEconomy(economy, ServicePriority.NORMAL, "neoessentials");
        } catch (Exception e) {
            LOGGER.error("[VaultAPI] Failed to register Economy provider: {}", e.getMessage(), e);
        }

        // Register built-in Permission provider
        try {
            NeoEssentialsPermission permission = new NeoEssentialsPermission();
            registry.registerPermission(permission, ServicePriority.NORMAL, "neoessentials");
        } catch (Exception e) {
            LOGGER.error("[VaultAPI] Failed to register Permission provider: {}", e.getMessage(), e);
        }

        // Register built-in Chat provider
        try {
            NeoEssentialsChat chat = new NeoEssentialsChat();
            registry.registerChat(chat, ServicePriority.NORMAL, "neoessentials");
        } catch (Exception e) {
            LOGGER.error("[VaultAPI] Failed to register Chat provider: {}", e.getMessage(), e);
        }

        // Third-party economy mods this build knows how to bridge — each is a one-directional
        // adapter WE wrote (the mod itself has no NeoEssentials awareness), gated behind
        // economy.useExternalEconomy so an operator has to opt in before it can override the
        // built-in economy above. Registered at HIGH priority, which is all that's needed for
        // it to take over — see VaultServiceRegistry's priority-sorted lookup.
        if (ConfigManager.isUsingExternalEconomy()) {
            if (ModList.get().isLoaded("sg_economy")) {
                try {
                    registry.registerEconomy(new SGEconomyAdapter(), ServicePriority.HIGH, "sg_economy");
                } catch (Exception e) {
                    LOGGER.error("[VaultAPI] Failed to register SG Economy API adapter: {}", e.getMessage(), e);
                }
            }
        }

        registry.logStatus();
        initialised = true;
        LOGGER.info("[VaultAPI] Vault API ready.");

        // Bridge: replace the shop's economy adapter with a VaultShopEconomyAdapter that
        // dynamically delegates to whichever VaultEconomy has the highest active priority.
        // This means third-party mods only need to register a VaultEconomy at HIGH/HIGHEST
        // priority and the shop system picks it up automatically.
        try {
            ShopEconomyRegistry.getInstance().register(
                new VaultShopEconomyAdapter(new NeoEssentialsShopEconomy()));
            LOGGER.info("[VaultAPI] VaultShopEconomyAdapter registered — shop now uses VaultServiceRegistry.");
        } catch (Exception e) {
            LOGGER.error("[VaultAPI] Failed to register VaultShopEconomyAdapter: {}", e.getMessage(), e);
        }
    }

    /** Called during server shutdown to clear all registrations. */
    public static void shutdown() {
        VaultServiceRegistry.getInstance().clear();
        initialised = false;
        LOGGER.info("[VaultAPI] Vault API shut down.");
    }

    /** Convenience accessor — economy (may be empty if disabled). */
    public static java.util.Optional<com.zerog.neoessentials.vault.api.VaultEconomy> getEconomy() {
        return VaultServiceRegistry.getInstance().getEconomy();
    }

    /** Convenience accessor — permission. */
    public static java.util.Optional<com.zerog.neoessentials.vault.api.VaultPermission> getPermission() {
        return VaultServiceRegistry.getInstance().getPermission();
    }

    /** Convenience accessor — chat metadata. */
    public static java.util.Optional<com.zerog.neoessentials.vault.api.VaultChat> getChat() {
        return VaultServiceRegistry.getInstance().getChat();
    }

    private VaultManager() {}
}

