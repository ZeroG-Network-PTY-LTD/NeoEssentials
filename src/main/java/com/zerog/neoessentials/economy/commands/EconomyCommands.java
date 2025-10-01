
package com.zerog.neoessentials.economy.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.zerog.neoessentials.config.CommandModuleConfig;
import net.minecraft.commands.CommandSourceStack;

// NOTE: Use LangUtil.translate for all user-facing messages to ensure proper localization.

public class EconomyCommands {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        CommandModuleConfig config = CommandModuleConfig.load(new java.io.File("config/neoessentials/config.json"));
        if (!config.enabled) return;
        if (config.isCommandEnabled("balance") || config.isCommandEnabled("bal")) {
            BalanceCommand.register(dispatcher);
        }
        if (config.isCommandEnabled("eco")) {
            EcoCommand.register(dispatcher);
        }
        if (config.isCommandEnabled("pay")) {
            PayCommand.register(dispatcher);
        }
        if (config.isCommandEnabled("baltop")) {
            BaltopCommand.register(dispatcher);
        }
        if (config.isCommandEnabled("paytoggle")) {
            PayToggleCommand.register(dispatcher);
        }
    }
}

//
