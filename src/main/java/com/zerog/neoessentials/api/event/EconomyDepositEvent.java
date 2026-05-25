package com.zerog.neoessentials.api.event;

import java.math.BigDecimal;

/**
 * Fired when a deposit is made to a player's account.
 * This event is cancellable — cancel it to veto the deposit.
 */
public class EconomyDepositEvent extends EconomyEvent {
    public EconomyDepositEvent(java.util.UUID playerId, double amount) {
        super(playerId, amount);
    }
    public EconomyDepositEvent(java.util.UUID playerId, BigDecimal amount) {
        super(playerId, amount);
    }
}
