package com.zerog.neoessentials.shop.pricing;

import com.zerog.neoessentials.shop.events.ShopTransactionEvent;
import com.zerog.neoessentials.shop.model.ShopData;
import net.minecraft.server.level.ServerLevel;

import java.util.UUID;

/**
 * Immutable value object passed to every {@link PriceRule} so rules can make
 * contextual decisions (supply/demand, time-of-day, bulk quantity, etc.).
 */
public record PriceContext(
        ShopData shop,
        UUID actorUUID,
        int requestedQuantity,
        ShopTransactionEvent.Type transactionType,
        ServerLevel level,
        long serverTimeMs
) {
    /** Convenience constructor that captures the current system time. */
    public PriceContext(ShopData shop, UUID actorUUID, int requestedQuantity,
                        ShopTransactionEvent.Type transactionType, ServerLevel level) {
        this(shop, actorUUID, requestedQuantity, transactionType, level, System.currentTimeMillis());
    }
}


