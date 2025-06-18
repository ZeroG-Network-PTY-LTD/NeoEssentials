<<<<<<< HEAD
public class Forge1201AdapterFactory {
    
=======
package com.zerog.neoessentials.adapter;

import com.zerog.neoessentials.common.adapter.AdapterFactory;
import com.zerog.neoessentials.common.adapter.ICommandAdapter;
import com.zerog.neoessentials.common.adapter.ILocationAdapter;
import com.zerog.neoessentials.common.adapter.IPermissionAdapter;
import com.zerog.neoessentials.common.adapter.IPlayerAdapter;

/**
 * Factory for creating adapter implementations for Forge 1.20.1
 */
public class Forge1201AdapterFactory implements AdapterFactory {
    private static final Forge1201AdapterFactory INSTANCE = new Forge1201AdapterFactory();
    
    private final LocationAdapter locationAdapter = new LocationAdapter();
    private final PermissionAdapter permissionAdapter = new PermissionAdapter();
    private final PlayerAdapter playerAdapter = new PlayerAdapter();
    private final CommandAdapter commandAdapter = new CommandAdapter();
    
    // Private constructor to enforce singleton pattern
    private Forge1201AdapterFactory() {
    }
    
    public static Forge1201AdapterFactory getInstance() {
        return INSTANCE;
    }
    
    @Override
    public ILocationAdapter getLocationAdapter() {
        return locationAdapter;
    }
    
    @Override
    public IPermissionAdapter getPermissionAdapter() {
        return permissionAdapter;
    }
    
    @Override
    public IPlayerAdapter getPlayerAdapter() {
        return playerAdapter;
    }
    
    @Override
    public ICommandAdapter getCommandAdapter() {
        return commandAdapter;
    }
>>>>>>> 7ac3350 (feat: Implement NeoEssentials for NeoForge 1.20.1 and 1.20.5)
}
