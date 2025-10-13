package com.zerog.neoessentials.teleportation.TeleportRequests;

/**
 * Enumeration for different types of teleportation requests
 */
public enum TeleportRequestType {
    /**
     * TPA - Requester wants to teleport TO the target
     */
    TPA("tpa"),
    
    /**
     * TPAHERE - Requester wants the target to teleport TO them
     */
    TPAHERE("tpahere");
    
    private final String command;
    
    TeleportRequestType(String command) {
        this.command = command;
    }
    
    public String getCommand() {
        return command;
    }
    
    @Override
    public String toString() {
        return command;
    }
    
    /**
     * Get type from command string
     */
    public static TeleportRequestType fromCommand(String command) {
        for (TeleportRequestType type : values()) {
            if (type.command.equalsIgnoreCase(command)) {
                return type;
            }
        }
        return null;
    }
}