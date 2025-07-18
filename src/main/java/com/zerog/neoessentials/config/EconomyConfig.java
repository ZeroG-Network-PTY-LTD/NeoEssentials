package com.zerog.neoessentials.config;

import java.util.HashMap;
import java.util.Map;

/**
 * Economy configuration for NeoEssentials
 * 
 * @author ZeroG
 * @since 2.0.0
 */
public class EconomyConfig {
    
    public boolean enabled = true;
    public String currencyName = "Coins";
    public String currencySymbol = "$";
    public double startingBalance = 1000.0;
    public double maxBalance = 1000000.0;
    public String balanceFormat = "{SYMBOL}{AMOUNT}";
    
    // Shop settings
    public boolean enableSignShops = true;
    public boolean enableGUIShops = true;
    public double shopTaxRate = 0.05; // 5% tax
    
    // Payment settings
    public double minPayment = 0.01;
    public double maxPayment = 100000.0;
    public boolean enablePaymentTax = false;
    public double paymentTaxRate = 0.02; // 2% tax
    
    // Interest settings
    public boolean enableInterest = false;
    public double interestRate = 0.01; // 1% daily
    public int interestIntervalHours = 24;
    
    public static EconomyConfig createDefault() {
        return new EconomyConfig();
    }
}
