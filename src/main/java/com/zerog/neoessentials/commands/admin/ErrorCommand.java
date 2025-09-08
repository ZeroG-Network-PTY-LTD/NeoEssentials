package com.zerog.neoessentials.commands.admin;

/**
 * Administrative command for managing error handling and diagnostics - SIMPLIFIED
 * TODO: Restore full command functionality when NeoForge imports work
 */
public class ErrorCommand {
    
    public static void register(Object dispatcher) {
        System.out.println("ErrorCommand registration disabled due to import issues");
        // TODO: Restore command registration when imports work
    }
    
    private static int executeStats(Object context) {
        System.out.println("Error stats command disabled");
        return 1;
    }
    
    private static int executeClear(Object context) {
        System.out.println("Error clear command disabled");
        return 1;
    }
}
