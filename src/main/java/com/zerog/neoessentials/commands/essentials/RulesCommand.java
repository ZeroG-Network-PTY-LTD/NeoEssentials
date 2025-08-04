package com.zerog.neoessentials.commands.essentials;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.zerog.neoessentials.util.MessageUtil;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.Arrays;
import java.util.List;

/**
 * Rules command implementation for NeoEssentials
 * Displays server rules to players
 * 
 * Commands:
 * - /rules - Display server rules
 * 
 * @author ZeroG
 * @since 2.0.0
 */
public class RulesCommand {
    
    // Default server rules - in production this would come from config
    private static final List<String> DEFAULT_RULES = Arrays.asList(
        "§6=== Server Rules ===",
        "§71. §aRespect all players and staff",
        "§72. §aNo griefing or stealing",
        "§73. §aNo spamming in chat",
        "§74. §aNo inappropriate language or content",
        "§75. §aNo cheating, hacking, or exploiting",
        "§76. §aFollow staff instructions",
        "§77. §aUse common sense and be kind",
        "§78. §aReport issues to staff",
        "§79. §aHave fun and enjoy the server!",
        "",
        "§7For help, use §a/help §7or contact staff",
        "§7Breaking rules may result in §ckick§7, §cmute§7, or §cban"
    );
    
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("rules")
            .executes(RulesCommand::showRules));
    }
    
    /**
     * Execute /rules command to display server rules
     */
    private static int showRules(CommandContext<CommandSourceStack> context) {
        // Send rules to all players or console
        if (context.getSource().getEntity() instanceof ServerPlayer player) {
            // Send to specific player
            for (String rule : DEFAULT_RULES) {
                MessageUtil.sendMessage(player, rule);
            }
        } else {
            // Send to console
            for (String rule : DEFAULT_RULES) {
                context.getSource().sendSuccess(() -> Component.literal(rule), false);
            }
        }
        
        return 1;
    }
    
    /**
     * Get server rules (for potential future API use)
     */
    public static List<String> getRules() {
        return DEFAULT_RULES;
    }
}
