package com.zerog.neoessentials.adapter;

import com.zerog.neoessentials.common.adapter.AdapterFactory;
import com.zerog.neoessentials.common.adapter.ICommandAdapter;
import com.zerog.neoessentials.common.adapter.ILocationAdapter;
import com.zerog.neoessentials.common.adapter.IPermissionAdapter;
import com.zerog.neoessentials.common.adapter.IPlayerAdapter;

/**
 * Factory for creating adapter implementations for NeoForge 1.20.5
 */
public class NeoForge1205AdapterFactory implements AdapterFactory {
    private static final NeoForge1205AdapterFactory INSTANCE = new NeoForge1205AdapterFactory();
    
    private final LocationAdapter locationAdapter = new LocationAdapter();
    private final PermissionAdapter permissionAdapter = new PermissionAdapter();
    private final PlayerAdapter playerAdapter = new PlayerAdapter();
    private final CommandAdapter commandAdapter = new CommandAdapter();
    
    // Private constructor to enforce singleton pattern
    private NeoForge1205AdapterFactory() {
    }
    
    public static NeoForge1205AdapterFactory getInstance() {
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
}
