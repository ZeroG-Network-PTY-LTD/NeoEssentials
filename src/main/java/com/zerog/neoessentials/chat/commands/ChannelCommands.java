package com.zerog.neoessentials.chat.commands;

import com.google.gson.JsonObject;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.zerog.neoessentials.chat.ChatHandler;
import com.zerog.neoessentials.config.ConfigManager;
import com.zerog.neoessentials.util.MessageUtil;
import com.zerog.neoessentials.util.PermissionValidator;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.level.ServerPlayer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Chat channel commands (/local, /global, /staff, etc.)
 * Allows players to switch between different chat channels.
 */
public class ChannelCommands {
    private static final Logger LOGGER = LoggerFactory.getLogger(ChannelCommands.class);

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        try {
            // Load channel configuration
            JsonObject mainConfig = ConfigManager.getInstance().getConfig(ConfigManager.MAIN_CONFIG);
            JsonObject chatConfig = mainConfig.has("chat") ? mainConfig.getAsJsonObject("chat") : null;
            JsonObject channelsConfig = chatConfig != null && chatConfig.has("channels") ? chatConfig.getAsJsonObject("channels") : null;

            if (channelsConfig == null) {
                LOGGER.warn("No channels configuration found, skipping channel command registration");
                return;
            }

            // Check master switch
            boolean channelsEnabled = true;
            if (channelsConfig.has("enabled")) {
                channelsEnabled = channelsConfig.get("enabled").getAsBoolean();
            }

            if (!channelsEnabled) {
                LOGGER.info("Chat channels system is disabled, skipping channel command registration");
                return;
            }

            // Register commands for each configured channel
            int registeredCount = 0;
            for (String channelName : channelsConfig.keySet()) {
                // Skip metadata fields
                if (channelName.equals("enabled") || channelName.endsWith("-description")) {
                    continue;
                }

                JsonObject channelObj = channelsConfig.getAsJsonObject(channelName);

                // Check if channel is enabled
                if (channelObj.has("enabled") && !channelObj.get("enabled").getAsBoolean()) {
                    LOGGER.debug("Channel '{}' is disabled, skipping command registration", channelName);
                    continue;
                }

                // Get command name
                String command = channelObj.has("command") ? channelObj.get("command").getAsString() : channelName;

                // Get permission if specified
                String permission = channelObj.has("permission") ? channelObj.get("permission").getAsString() : null;

                // Register main command
                registerChannelCommand(dispatcher, command, channelName, permission);
                registeredCount++;

                // Register aliases
                if (channelObj.has("aliases") && channelObj.get("aliases").isJsonArray()) {
                    var aliases = channelObj.getAsJsonArray("aliases");
                    for (var aliasElement : aliases) {
                        String alias = aliasElement.getAsString();
                        registerChannelCommand(dispatcher, alias, channelName, permission);
                        registeredCount++;
                    }
                }
            }

            LOGGER.info("Registered {} channel commands", registeredCount);

        } catch (Exception e) {
            LOGGER.error("Failed to register channel commands: {}", e.getMessage(), e);
        }
    }

    /**
     * Register a single channel command
     */
    private static void registerChannelCommand(CommandDispatcher<CommandSourceStack> dispatcher, String commandName, String channelName, String permission) {
        dispatcher.register(Commands.literal(commandName)
            .executes(ctx -> switchChannel(ctx, channelName, permission))
            .then(Commands.argument("message", StringArgumentType.greedyString())
                .executes(ctx -> {
                    // Switch to channel and send message in one command
                    int result = switchChannel(ctx, channelName, permission);
                    if (result == 1) {
                        // Channel switched successfully, now send the message
                        String message = StringArgumentType.getString(ctx, "message");
                        ServerPlayer player = ctx.getSource().getPlayerOrException();

                        // Trigger chat event with the message
                        // This will be handled by ChatHandler which will see the player is now in this channel
                        player.connection.chat(message, System.currentTimeMillis(), null, false);
                    }
                    return result;
                })
            )
        );
    }

    /**
     * Switch player to specified channel
     */
    private static int switchChannel(CommandContext<CommandSourceStack> ctx, String channelName, String permission) {
        try {
            ServerPlayer player = ctx.getSource().getPlayerOrException();

            // Check permission if required
            if (permission != null && !permission.isEmpty()) {
                PermissionValidator.PermissionResult permResult = PermissionValidator.validatePermission(ctx.getSource(), permission);
                if (!permResult.hasPermission()) {
                    ctx.getSource().sendFailure(MessageUtil.error(permResult.getErrorMessage()));
                    return 0;
                }
            }

            // Set player's channel
            ChatHandler.setPlayerChannel(player.getUUID(), channelName);

            // Send confirmation
            ctx.getSource().sendSuccess(() -> MessageUtil.success("commands.neoessentials.channel.switched", channelName), false);

            LOGGER.debug("Player {} switched to channel: {}", player.getName().getString(), channelName);
            return 1;

        } catch (Exception e) {
            LOGGER.error("Error switching channel: {}", e.getMessage(), e);
            ctx.getSource().sendFailure(MessageUtil.error("commands.neoessentials.channel.error"));
            return 0;
        }
    }
}

