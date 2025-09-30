package com.zerog.neoessentials.api.event;

/**
 * Fired when a withdrawal is made from a player's account.
 */
public class EconomyWithdrawEvent extends EconomyEvent {
    public EconomyWithdrawEvent(java.util.UUID playerId, double amount) {
        super(playerId, amount);
    }
}
