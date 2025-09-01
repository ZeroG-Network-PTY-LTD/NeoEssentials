package com.zerog.neoessentials.config;

/**
 * Shops Configuration for NeoEssentials
 * Represents the shops.json file structure
 */
public class ShopsConfig {
    
    public General general = new General();
    public Discord discord = new Discord();
    
    public static class General {
        public boolean enabled = true;
        public boolean allowAdminShops = true;
        public boolean allowPlayerShops = true;
        public double defaultTaxRate = 0.00;
    }
    
    public static class Discord {
        public boolean enabled = true;
        public Notifications notifications = new Notifications();
        
        public static class Notifications {
            public ShopCreated shopCreated = new ShopCreated();
            
            public static class ShopCreated {
                public boolean enabled = true;
                public String channel = "general";
            }
        }
    }
}
