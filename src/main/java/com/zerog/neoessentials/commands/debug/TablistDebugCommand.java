package com.zerog.neoessentials.commands.debug;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.zerog.neoessentials.NeoEssentials;
import com.zerog.neoessentials.commands.CommandBase;
import com.zerog.neoessentials.debug.TablistDebugger;
import com.zerog.neoessentials.ui.tab.TabManager;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/**
 * Command to debug tablist issues
 */
public class TablistDebugCommand extends CommandBase {
    private final TabManager tabManager;
    
    public TablistDebugCommand(TabManager tabManager) {
        super("tabdebug");
        setDescription("Debug tablist configuration issues");
        setPermission("neoessentials.command.tabdebug");
        
        this.tabManager = tabManager;
    }
    
    @Override
    public LiteralArgumentBuilder<CommandSourceStack> build() {
        return Commands.literal(getName())
            .requires(source -> checkPermission(source))
            .executes(this::executeDebug);
    }
    
    private int executeDebug(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        
        try {
            if (!(source.getEntity() instanceof ServerPlayer)) {
                source.sendFailure(Component.literal("This command must be run by a player"));
                return 0;
            }
            
            ServerPlayer player = (ServerPlayer) source.getEntity();
            
            // Run the debug process
            TablistDebugger.debugTablist(tabManager, player);
            
            return Command.SINGLE_SUCCESS;
        } catch (Exception e) {
            source.sendFailure(Component.literal("Error: " + e.getMessage()));
            NeoEssentials.LOGGER.error("Error executing tabdebug command", e);
            return 0;
        }
    }
}
