package com.zerog.neoessentials.security;

import com.zerog.neoessentials.util.CommandSourceHelper;
import com.zerog.neoessentials.util.InputValidator;
import com.zerog.neoessentials.util.MessageUtil;
import com.zerog.neoessentials.config.ConfigManager;
import com.zerog.neoessentials.logging.LogCategory;
import com.zerog.neoessentials.logging.NeoLog;
import com.zerog.neoessentials.moderation.FreezeManager;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.CommandEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Enforces maxCommandLength and freeze restrictions for PLAYER commands only.
 * Does NOT interfere with console, command blocks, or other mods.
 *
 * <p>Configuration Options:</p>
 * <ul>
 *   <li>security.enableCommandLengthEnforcer - Enable/disable this enforcer (default: true)</li>
 *   <li>security.enableInputValidation - Enable/disable input validation (default: true)</li>
 *   <li>security.maxCommandLength - Maximum command length (default: 256)</li>
 *   <li>moderation.freezeSettings.enableFreezeSystem - Enable freeze system (default: true)</li>
 *   <li>moderation.freezeSettings.preventCommands - Prevent commands when frozen (default: true)</li>
 * </ul>
 */
@EventBusSubscriber(modid = "neoessentials")
public class CommandLengthEnforcer {
    private static final Logger LOGGER = LoggerFactory.getLogger(CommandLengthEnforcer.class);

    /**
     * Command event handler - ONLY processes player commands.
     * Priority: NORMAL (runs after other mods can process but before execution)
     */
    @SubscribeEvent(priority = EventPriority.NORMAL)
    public static void onCommand(CommandEvent event) {
        // CRITICAL: Only process if source is a ServerPlayer
        // This ensures console, command blocks, and other mods are NEVER affected
        if (!CommandSourceHelper.isPlayer(event.getParseResults().getContext().getSource())) {
            return; // Exit immediately for non-player sources
        }

        ServerPlayer player = CommandSourceHelper.getPlayer(event.getParseResults().getContext().getSource());
        if (player == null) {
            return; // Safety check - should never happen but prevents NPE
        }

        // Get the raw command string
        String rawCommand = event.getParseResults().getReader().getString();

        // Gated by logging.categories.commands.normal (default on) — every OTHER command log
        // in the mod only fires for specific commands (reload, language, etc.) or for commands
        // this enforcer actually blocks; this is the only place that sees every player command
        // unconditionally, so it's the one spot that can log all of them to console/latest.log.
        NeoLog.info(LOGGER, LogCategory.COMMANDS, "{} issued command: /{}", player.getName().getString(), rawCommand);

        // ========================================
        // FREEZE SYSTEM CHECK (First Priority)
        // ========================================
        if (shouldCheckFreezeRestrictions()) {
            if (handleFreezeRestriction(event, player, rawCommand)) {
                return; // Command blocked by freeze system
            }
        }

        // ========================================
        // COMMAND LENGTH ENFORCEMENT (Second Priority)
        // ========================================
        if (shouldEnforceCommandLength()) {
            handleCommandLengthValidation(event, player, rawCommand);
        }
    }

    /**
     * Check if freeze restrictions should be applied.
     * Controlled by config: moderation.freezeSettings.enableFreezeSystem and preventCommands
     */
    private static boolean shouldCheckFreezeRestrictions() {
        return ConfigManager.isFreezeSystemEnabled()
            && ConfigManager.isFreezePreventCommandsEnabled();
    }

    /**
     * Check if command length enforcement should be applied.
     * Controlled by config: security.enableCommandLengthEnforcer (new)
     */
    private static boolean shouldEnforceCommandLength() {
        return ConfigManager.getInstance().isCommandLengthEnforcerEnabled();
    }

    /**
     * Handle freeze restriction logic.
     * Returns true if command was blocked, false if allowed.
     */
    private static boolean handleFreezeRestriction(CommandEvent event, ServerPlayer player, String rawCommand) {
        FreezeManager freezeManager = FreezeManager.getInstance();

        // Check if player is frozen
        if (!freezeManager.isPlayerFrozen(player.getUUID())) {
            return false; // Player not frozen, allow command
        }

        // Parse command name
        String command = rawCommand.startsWith("/") ? rawCommand.substring(1) : rawCommand;
        String commandName = command.split(" ", 2)[0].toLowerCase();

        // Check if command is in allowed list for frozen players
        java.util.List<String> allowedCommands = ConfigManager.getFreezeAllowedCommands();
        if (allowedCommands.contains(commandName)) {
            NeoLog.debug(LOGGER, LogCategory.GENERAL, "Frozen player {} used allowed command: {}", player.getName().getString(), commandName);
            return false; // Command is allowed even when frozen
        }

        // Block the command
        event.setCanceled(true);
        player.sendSystemMessage(MessageUtil.error("commands.neoessentials.freeze.cannot_use_commands"));
        NeoLog.info(LOGGER, LogCategory.GENERAL, "Blocked command from frozen player {}: {}", player.getName().getString(), rawCommand);
        return true; // Command was blocked
    }

    /**
     * Handle command length validation.
     */
    private static void handleCommandLengthValidation(CommandEvent event, ServerPlayer player, String rawCommand) {
        // Remove leading slash if present
        String command = rawCommand.startsWith("/") ? rawCommand.substring(1) : rawCommand;

        // Validate command using InputValidator
        InputValidator.ValidationResult result = InputValidator.validateCommand(command);

        if (!result.isValid()) {
            // Block the command
            event.setCanceled(true);
            player.sendSystemMessage(MessageUtil.error(result.getErrorMessage()));
            NeoLog.info(LOGGER, LogCategory.GENERAL, "Blocked invalid command from {}: {} (Reason: {})",
                player.getName().getString(),
                command.length() > 50 ? command.substring(0, 50) + "..." : command,
                result.getErrorMessage());
        }
    }
}
