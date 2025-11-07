package com.zerog.neoessentials.security;

import com.zerog.neoessentials.util.InputValidator;
import com.zerog.neoessentials.util.MessageUtil;
import com.zerog.neoessentials.config.ConfigManager;
import com.zerog.neoessentials.moderation.FreezeManager;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.CommandEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Enforces maxCommandLength for all player commands.
 */
@EventBusSubscriber(modid = "neoessentials")
public class CommandLengthEnforcer {
    private static final Logger LOGGER = LoggerFactory.getLogger(CommandLengthEnforcer.class);

    @SubscribeEvent
    public static void onCommand(CommandEvent event) {
        if (!(event.getParseResults().getContext().getSource().getEntity() instanceof ServerPlayer player)) {
            return; // Only enforce for players
        }


        // Freeze command blocking logic with allowedCommands support
    if (com.zerog.neoessentials.config.ConfigManager.isFreezeSystemEnabled() && com.zerog.neoessentials.config.ConfigManager.isFreezePreventCommandsEnabled()) {
            FreezeManager freezeManager = FreezeManager.getInstance();
            if (freezeManager.isPlayerFrozen(player.getUUID())) {
                String rawCmd = event.getParseResults().getReader().getString();
                String baseCmd = rawCmd.startsWith("/") ? rawCmd.substring(1) : rawCmd;
                String cmdName = baseCmd.split(" ", 2)[0].toLowerCase();
                java.util.List<String> allowed = ConfigManager.getFreezeAllowedCommands();
                if (!allowed.contains(cmdName)) {
                    event.setCanceled(true);
                    player.sendSystemMessage(MessageUtil.error("commands.neoessentials.freeze.cannot_use_commands"));
                    LOGGER.info("Blocked command from frozen player {}: {}", player.getName().getString(), rawCmd);
                    return;
                }
            }
        }

        String raw = event.getParseResults().getReader().getString();
        // Remove leading slash if present
        String command = raw.startsWith("/") ? raw.substring(1) : raw;
        InputValidator.ValidationResult result = InputValidator.validateCommand(command);
        if (!result.isValid()) {
            event.setCanceled(true);
            player.sendSystemMessage(MessageUtil.error(result.getErrorMessage()));
            LOGGER.info("Blocked overlong or unsafe command from {}: {}", player.getName().getString(), command);
        }
    }
}
