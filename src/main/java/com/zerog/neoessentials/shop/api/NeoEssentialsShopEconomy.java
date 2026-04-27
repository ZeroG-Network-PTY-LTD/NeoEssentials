package com.zerog.neoessentials.shop.api;

import com.zerog.neoessentials.economy.managers.EconomyManager;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Default {@link ShopEconomyAdapter} that delegates to the built-in
 * {@link EconomyManager}.
 */
public class NeoEssentialsShopEconomy implements ShopEconomyAdapter {

    @Override
    public String getProviderName() {
        return "NeoEssentials built-in economy";
    }

    @Override
    public BigDecimal getBalance(UUID player) {
        return EconomyManager.getInstance().getBalance(player);
    }

    @Override
    public boolean hasBalance(UUID player, BigDecimal amount) {
        return getBalance(player).compareTo(amount) >= 0;
    }

    @Override
    public boolean debit(UUID player, BigDecimal amount) {
        return EconomyManager.getInstance().subtractBalance(player, amount);
    }

    @Override
    public boolean credit(UUID player, BigDecimal amount) {
        return EconomyManager.getInstance().addBalance(player, amount);
    }

    @Override
    public String format(BigDecimal amount) {
        String symbol = EconomyManager.getInstance().getCurrencySymbol();
        return symbol + amount.toPlainString();
    }
}

