package com.zerog.neoessentials.afk;

public class AFKEventManager {
    
    // Temporarily disabled event management due to import issues
    // TODO: Restore event management when NeoForge imports are stable
    
    private static AFKEventManager instance;

    public static AFKEventManager getInstance() {
        if (instance == null) {
            instance = new AFKEventManager();
        }
        return instance;
    }

    public void init() {
        // Placeholder for initialization
        // Will restore event bus registration when imports work
    }

    public void shutdown() {
        // Cleanup if needed
    }
}
