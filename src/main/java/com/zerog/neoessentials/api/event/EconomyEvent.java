package com.zerog.neoessentials.api.event;

import net.neoforged.bus.api.Event;
import net.neoforged.bus.api.ICancellableEvent;
import java.math.BigDecimal;
import java.util.UUID;

/**
 * Base class for all economy-related events.
 * <p>
 * These events are fired on the NeoForge event bus and are cancellable,
 * allowing other mods to veto economy transactions.
 */
public abstract class EconomyEvent extends Event implements ICancellableEvent {
    private final UUID playerId;
    private final double amount;
    private final BigDecimal bigDecimalAmount;

    public EconomyEvent(UUID playerId, double amount) {
        this.playerId = playerId;
        this.amount = amount;
        this.bigDecimalAmount = BigDecimal.valueOf(amount);
    }

    public EconomyEvent(UUID playerId, BigDecimal amount) {
        this.playerId = playerId;
        this.bigDecimalAmount = amount;
        this.amount = amount.doubleValue();
    }

    public UUID getPlayerId() {
        return playerId;
    }

    public double getAmount() {
        return amount;
    }

    /** Get the transaction amount as BigDecimal for precision. */
    public BigDecimal getBigDecimalAmount() {
        return bigDecimalAmount;
    }
}
