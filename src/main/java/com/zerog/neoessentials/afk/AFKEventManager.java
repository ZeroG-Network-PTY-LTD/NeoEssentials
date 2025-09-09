package com.zerog.neoessentials.afk;

import net.neoforged.neoforge.common.NeoForge;

public class AFKEventManager {
    
    private static AFKEventManager instance;
    private AFKEventListener eventListener;

    public static AFKEventManager getInstance() {
        if (instance == null) {
            instance = new AFKEventManager();
        }
        return instance;
    }

    public void init() {
        // Register AFK event listener to NeoForge event bus
        eventListener = new AFKEventListener();
        NeoForge.EVENT_BUS.register(eventListener);
    }

    public void shutdown() {
        // Unregister events if needed
        if (eventListener != null) {
            NeoForge.EVENT_BUS.unregister(eventListener);
        }
    }
}
