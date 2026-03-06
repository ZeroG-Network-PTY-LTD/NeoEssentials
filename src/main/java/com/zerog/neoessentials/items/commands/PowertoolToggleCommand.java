package com.zerog.neoessentials.items.commands;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;

/**
 * @deprecated All powertool toggle logic is now in {@link PowertoolCommand}.
 *             This class is kept as a compatibility shim and does nothing.
 *             {@code /powertooltoggle} is registered by {@link PowertoolCommand#register}.
 */
@Deprecated
public class PowertoolToggleCommand {

    /** @deprecated Delegates to {@link PowertoolCommand#isPowertoolEnabled(java.util.UUID)}. */
    @Deprecated
    public static boolean isPowertoolEnabled(java.util.UUID playerUUID, String itemId) {
        return PowertoolCommand.isPowertoolEnabled(playerUUID);
    }

    /**
     * @deprecated /powertooltoggle is now registered inside {@link PowertoolCommand#register}.
     *             Calling this is a no-op to avoid double-registration.
     */
    @Deprecated
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        // no-op: /powertooltoggle is already registered by PowertoolCommand.register()
    }
}
