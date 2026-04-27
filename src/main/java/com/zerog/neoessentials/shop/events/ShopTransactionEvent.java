package com.zerog.neoessentials.shop.events;

import com.zerog.neoessentials.shop.ShopTransaction;
import com.zerog.neoessentials.shop.model.ShopData;
import net.neoforged.bus.api.Event;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Fired on the NeoForge game event bus after a successful shop buy or sell.
 *
 * <p>Other systems can listen to this event to implement logging, analytics,
 * achievement hooks, etc. without modifying the core shop code.
 *
 * <pre>{@code
 * @SubscribeEvent
 * public static void onShopTransaction(ShopTransactionEvent event) {
 *     if (event.getTransactionType() == ShopTransactionEvent.Type.BUY) { ... }
 * }
 * }</pre>
 */
public class ShopTransactionEvent extends Event {

    public enum Type { BUY, SELL }

    private final ShopData shop;
    private final UUID actorUUID;
    private final Type transactionType;
    private final BigDecimal finalPrice;
    private final int quantity;

    public ShopTransactionEvent(ShopData shop, UUID actorUUID,
                                Type transactionType, BigDecimal finalPrice, int quantity) {
        this.shop            = shop;
        this.actorUUID       = actorUUID;
        this.transactionType = transactionType;
        this.finalPrice      = finalPrice;
        this.quantity        = quantity;
    }

    public ShopData getShop()                  { return shop; }
    public UUID getActorUUID()                 { return actorUUID; }
    public Type getTransactionType()           { return transactionType; }
    public BigDecimal getFinalPrice()          { return finalPrice; }
    public int getQuantity()                   { return quantity; }

    /** Convenience: true when a player bought from this shop. */
    public boolean isBuy()  { return transactionType == Type.BUY;  }
    /** Convenience: true when a player sold to this shop. */
    public boolean isSell() { return transactionType == Type.SELL; }
}

