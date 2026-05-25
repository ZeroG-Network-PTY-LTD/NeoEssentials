package com.zerog.neoessentials.api.event;

import java.math.BigDecimal;

/**
 * Fired when a withdrawal is made from a player's account.
 * This event is cancellable — cancel it to veto the withdrawal.
 */
public class EconomyWithdrawEvent extends EconomyEvent {
    public EconomyWithdrawEvent(java.util.UUID playerId, double amount) {
        super(playerId, amount);
    }
    public EconomyWithdrawEvent(java.util.UUID playerId, BigDecimal amount) {
        super(playerId, amount);
    }
}
