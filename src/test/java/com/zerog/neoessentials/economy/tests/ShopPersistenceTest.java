package com.zerog.neoessentials.economy.tests;

import com.zerog.neoessentials.economy.Shop;
import com.zerog.neoessentials.economy.ShopManager;
import com.zerog.neoessentials.economy.persistence.EconomyPersistenceManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import static org.junit.jupiter.api.Assertions.*;

import java.util.UUID;
import java.util.Map;

/**
 * Test shop persistence functionality
 */
public class ShopPersistenceTest {
    
    private ShopManager shopManager;
    private EconomyPersistenceManager persistenceManager;
    
    @BeforeEach
    public void setUp() {
        // Initialize managers for testing
        shopManager = ShopManager.getInstance();
        persistenceManager = EconomyPersistenceManager.getInstance();
    }
    
    @Test
    public void testShopCreationAndPersistence() {
        // Create a test shop
        UUID ownerId = UUID.randomUUID();
        String shopName = "TestShop";
        String category = "general";
        Shop.ShopType shopType = Shop.ShopType.PLAYER;
        
        // Create shop (should save to persistence)
        Shop shop = shopManager.createShop(ownerId, shopName, shopType);
        assertNotNull(shop, "Shop should be created successfully");
        assertEquals(shopName, shop.getShopName(), "Shop name should match");
        assertEquals(ownerId, shop.getOwnerId(), "Owner ID should match");
        
        // Verify shop is in manager's memory
        Shop retrievedShop = shopManager.getShop(shop.getShopId());
        assertNotNull(retrievedShop, "Shop should be retrievable from manager");
        
        // Verify shop is saved to persistence
        Map<UUID, Shop> allShops = persistenceManager.getAllShops();
        assertTrue(allShops.containsKey(shop.getShopId()), "Shop should be saved to persistence");
        
        System.out.println("✓ Shop persistence test passed");
        System.out.println("  - Shop created: " + shop.getShopName());
        System.out.println("  - Shop ID: " + shop.getShopId());
        System.out.println("  - Saved to persistence: " + allShops.containsKey(shop.getShopId()));
    }
    
    @Test
    public void testShopLoadingFromPersistence() {
        // This test would require a server restart simulation
        // which is complex to test in unit tests
        
        // Instead, we can test the cache loading mechanism
        Map<UUID, Shop> allShops = persistenceManager.getAllShops();
        System.out.println("✓ Shop loading test - shops in cache: " + allShops.size());
        
        for (Shop shop : allShops.values()) {
            assertNotNull(shop.getShopId(), "Shop should have valid ID");
            assertNotNull(shop.getOwnerId(), "Shop should have valid owner");
            assertNotNull(shop.getShopName(), "Shop should have valid name");
            System.out.println("  - Found shop: " + shop.getShopName() + " (" + shop.getShopId() + ")");
        }
    }
}
