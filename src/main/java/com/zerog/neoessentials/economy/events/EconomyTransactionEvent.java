package com.zerog.neoessentials.economy.events;

import java.math.BigDecimal;
import java.util.UUID;
import net.neoforged.bus.api.Event;

public class EconomyTransactionEvent extends Event {
    public enum Type {
        PAY, ADMIN_GIVE, ADMIN_TAKE, ADMIN_SET
    }

    private final Type type;
    private final UUID sender;
    private final UUID recipient;
    private final BigDecimal amount;
    private final String reason;

    public EconomyTransactionEvent(Type type, UUID sender, UUID recipient, BigDecimal amount, String reason) {
        this.type = type;
        this.sender = sender;
        this.recipient = recipient;
        this.amount = amount;
        this.reason = reason;
    }

    public Type getType() { return type; }
    public UUID getSender() { return sender; }
    public UUID getRecipient() { return recipient; }
    public BigDecimal getAmount() { return amount; }
    public String getReason() { return reason; }
}
