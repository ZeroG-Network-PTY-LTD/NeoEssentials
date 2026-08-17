package com.zerog.neoessentials.shop.api;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.zerog.neoessentials.logging.LogCategory;
import com.zerog.neoessentials.logging.NeoLog;

import java.util.concurrent.atomic.AtomicReference;

/**
 * Registry that holds the active {@link ShopEconomyAdapter}.
 *
 * <p>On startup the built-in {@link NeoEssentialsShopEconomy} is used.
 * External integrations (e.g. Vault) can call {@link #register(ShopEconomyAdapter)}
 * to replace it.
 */
public class ShopEconomyRegistry {

    private static final Logger LOGGER = LoggerFactory.getLogger(ShopEconomyRegistry.class);

    private static final ShopEconomyRegistry INSTANCE = new ShopEconomyRegistry();
    public static ShopEconomyRegistry getInstance() { return INSTANCE; }

    private final AtomicReference<ShopEconomyAdapter> adapter =
            new AtomicReference<>(new NeoEssentialsShopEconomy());

    private ShopEconomyRegistry() {}

    /**
     * Replace the active economy adapter.
     * Call this during mod/server startup once your economy provider is ready.
     */
    public void register(ShopEconomyAdapter newAdapter) {
        ShopEconomyAdapter old = adapter.getAndSet(newAdapter);
        NeoLog.info(LOGGER, LogCategory.GENERAL, "[ChestShop] Economy adapter changed: '{}' → '{}'",
                old.getProviderName(), newAdapter.getProviderName());
    }

    /** @return the active economy adapter, never null. */
    public ShopEconomyAdapter getAdapter() {
        return adapter.get();
    }
}

