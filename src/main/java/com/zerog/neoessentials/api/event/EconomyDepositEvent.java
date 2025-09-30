package com.zerog.neoessentials.api.event;

/**
 * Fired when a deposit is made to a player's account.
 */
public class EconomyDepositEvent extends EconomyEvent {
    public EconomyDepositEvent(java.util.UUID playerId, double amount) {
        super(playerId, amount);
    }
}
