package com.zerog.neoessentials.api.event;

import net.neoforged.bus.api.Event;
import java.util.UUID;

/**
 * Base class for all economy-related events.
 */
public abstract class EconomyEvent extends Event {
    private final UUID playerId;
    private final double amount;

    public EconomyEvent(UUID playerId, double amount) {
        this.playerId = playerId;
        this.amount = amount;
    }

    public UUID getPlayerId() {
        return playerId;
    }

    public double getAmount() {
        return amount;
    }
}
