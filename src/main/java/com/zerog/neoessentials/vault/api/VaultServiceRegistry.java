package com.zerog.neoessentials.vault.api;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * NeoEssentials Vault Service Registry.
 * <p>
 * Central registry for {@link VaultEconomy}, {@link VaultPermission}, and {@link VaultChat}
 * implementations. Third-party NeoForge mods can register their own implementations and
 * query the highest-priority active provider.
 * <p>
 * NeoEssentials automatically registers its built-in implementations at server start with
 * {@link ServicePriority#NORMAL}. Third-party mods should register with
 * {@link ServicePriority#HIGH} or {@link ServicePriority#HIGHEST} to override the default.
 */
public final class VaultServiceRegistry {

    private static final Logger LOGGER = LoggerFactory.getLogger(VaultServiceRegistry.class);

    /** Registration priority — higher priority providers take precedence. */
    public enum ServicePriority {
        LOWEST(0), LOW(1), NORMAL(2), HIGH(3), HIGHEST(4);

        final int value;
        ServicePriority(int value) { this.value = value; }
    }

    /** A registered service provider with metadata. */
    public static class Registration<T> {
        public final T provider;
        public final ServicePriority priority;
        public final String registeredBy; // mod id or name

        Registration(T provider, ServicePriority priority, String registeredBy) {
            this.provider = provider;
            this.priority = priority;
            this.registeredBy = registeredBy;
        }
    }

    // ── Singleton ─────────────────────────────────────────────────────────────

    private static final VaultServiceRegistry INSTANCE = new VaultServiceRegistry();
    public static VaultServiceRegistry getInstance() { return INSTANCE; }
    private VaultServiceRegistry() {}

    // ── Registrations ─────────────────────────────────────────────────────────

    private final List<Registration<VaultEconomy>>    economyProviders    = new ArrayList<>();
    private final List<Registration<VaultPermission>> permissionProviders = new ArrayList<>();
    private final List<Registration<VaultChat>>       chatProviders       = new ArrayList<>();

    // ── Economy ───────────────────────────────────────────────────────────────

    /**
     * Register an economy implementation.
     * @param economy    the implementation
     * @param priority   the priority (higher wins when multiple are registered)
     * @param registrant the mod id registering this provider
     */
    public void registerEconomy(VaultEconomy economy, ServicePriority priority, String registrant) {
        economyProviders.add(new Registration<>(economy, priority, registrant));
        economyProviders.sort(Comparator.comparingInt((Registration<VaultEconomy> r) -> r.priority.value).reversed());
        LOGGER.info("[VaultAPI] Economy provider registered: {} (priority={}, by={})",
            economy.getName(), priority, registrant);
    }

    public void registerEconomy(VaultEconomy economy) {
        registerEconomy(economy, ServicePriority.NORMAL, "unknown");
    }

    /**
     * Get the highest-priority active economy, or empty if none registered.
     */
    public Optional<VaultEconomy> getEconomy() {
        return economyProviders.stream()
            .filter(r -> r.provider.isEnabled())
            .map(r -> r.provider)
            .findFirst();
    }

    /** Get all registered economy providers. */
    public List<Registration<VaultEconomy>> getEconomyProviders() {
        return List.copyOf(economyProviders);
    }

    // ── Permission ────────────────────────────────────────────────────────────

    public void registerPermission(VaultPermission permission, ServicePriority priority, String registrant) {
        permissionProviders.add(new Registration<>(permission, priority, registrant));
        permissionProviders.sort(Comparator.comparingInt((Registration<VaultPermission> r) -> r.priority.value).reversed());
        LOGGER.info("[VaultAPI] Permission provider registered: {} (priority={}, by={})",
            permission.getName(), priority, registrant);
    }

    public void registerPermission(VaultPermission permission) {
        registerPermission(permission, ServicePriority.NORMAL, "unknown");
    }

    public Optional<VaultPermission> getPermission() {
        return permissionProviders.stream()
            .filter(r -> r.provider.isEnabled())
            .map(r -> r.provider)
            .findFirst();
    }

    public List<Registration<VaultPermission>> getPermissionProviders() {
        return List.copyOf(permissionProviders);
    }

    // ── Chat ──────────────────────────────────────────────────────────────────

    public void registerChat(VaultChat chat, ServicePriority priority, String registrant) {
        chatProviders.add(new Registration<>(chat, priority, registrant));
        chatProviders.sort(Comparator.comparingInt((Registration<VaultChat> r) -> r.priority.value).reversed());
        LOGGER.info("[VaultAPI] Chat provider registered: {} (priority={}, by={})",
            chat.getName(), priority, registrant);
    }

    public void registerChat(VaultChat chat) {
        registerChat(chat, ServicePriority.NORMAL, "unknown");
    }

    public Optional<VaultChat> getChat() {
        return chatProviders.stream()
            .filter(r -> r.provider.isEnabled())
            .map(r -> r.provider)
            .findFirst();
    }

    public List<Registration<VaultChat>> getChatProviders() {
        return List.copyOf(chatProviders);
    }

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    /** Clear all registrations (called on server stop). */
    public void clear() {
        economyProviders.clear();
        permissionProviders.clear();
        chatProviders.clear();
        LOGGER.info("[VaultAPI] Service registry cleared.");
    }

    /** Log current registration status. */
    public void logStatus() {
        LOGGER.info("[VaultAPI] === Vault Service Status ===");
        LOGGER.info("[VaultAPI]  Economy:    {} provider(s) - active: {}",
            economyProviders.size(), getEconomy().map(VaultEconomy::getName).orElse("none"));
        LOGGER.info("[VaultAPI]  Permission: {} provider(s) - active: {}",
            permissionProviders.size(), getPermission().map(VaultPermission::getName).orElse("none"));
        LOGGER.info("[VaultAPI]  Chat:       {} provider(s) - active: {}",
            chatProviders.size(), getChat().map(VaultChat::getName).orElse("none"));
        LOGGER.info("[VaultAPI] ==============================");
    }
}

