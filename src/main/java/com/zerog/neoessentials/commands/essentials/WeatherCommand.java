package com.zerog.neoessentials.commands.essentials;

import com.zerog.neoessentials.util.PermissionUtil;
import com.zerog.neoessentials.permissions.PermissionNodes;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;

public class WeatherCommand {
    private static final int DEFAULT_DURATION = 600; // 30 seconds (600 ticks)
    private static final int MAX_DURATION = 1000000; // Maximum weather duration

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("weather")
            .requires(source -> PermissionUtil.hasPermissionOrOp(source, PermissionNodes.MODERATION_BASIC))
            .then(Commands.literal("clear")
                .executes(WeatherCommand::setClear)
                .then(Commands.argument("duration", IntegerArgumentType.integer(0, MAX_DURATION))
                    .executes(WeatherCommand::setClearWithDuration)
                )
            )
            .then(Commands.literal("rain")
                .executes(WeatherCommand::setRain)
                .then(Commands.argument("duration", IntegerArgumentType.integer(0, MAX_DURATION))
                    .executes(WeatherCommand::setRainWithDuration)
                )
            )
            .then(Commands.literal("thunder")
                .executes(WeatherCommand::setThunder)
                .then(Commands.argument("duration", IntegerArgumentType.integer(0, MAX_DURATION))
                    .executes(WeatherCommand::setThunderWithDuration)
                )
            )
        );
        
        // Alternative commands for convenience
        dispatcher.register(Commands.literal("sun")
            .requires(source -> PermissionUtil.hasPermissionOrOp(source, PermissionNodes.MODERATION_BASIC))
            .executes(WeatherCommand::setClear)
            .then(Commands.argument("duration", IntegerArgumentType.integer(0, MAX_DURATION))
                .executes(WeatherCommand::setClearWithDuration)
            )
        );
        
        dispatcher.register(Commands.literal("storm")
            .requires(source -> PermissionUtil.hasPermissionOrOp(source, PermissionNodes.MODERATION_BASIC))
            .executes(WeatherCommand::setThunder)
            .then(Commands.argument("duration", IntegerArgumentType.integer(0, MAX_DURATION))
                .executes(WeatherCommand::setThunderWithDuration)
            )
        );
    }

    /**
     * Set weather to clear with default duration
     */
    private static int setClear(CommandContext<CommandSourceStack> context) {
        return setWeather(context.getSource(), false, false, DEFAULT_DURATION, "clear");
    }

    /**
     * Set weather to clear with specified duration
     */
    private static int setClearWithDuration(CommandContext<CommandSourceStack> context) {
        int duration = IntegerArgumentType.getInteger(context, "duration");
        return setWeather(context.getSource(), false, false, duration, "clear");
    }

    /**
     * Set weather to rain with default duration
     */
    private static int setRain(CommandContext<CommandSourceStack> context) {
        return setWeather(context.getSource(), true, false, DEFAULT_DURATION, "rain");
    }

    /**
     * Set weather to rain with specified duration
     */
    private static int setRainWithDuration(CommandContext<CommandSourceStack> context) {
        int duration = IntegerArgumentType.getInteger(context, "duration");
        return setWeather(context.getSource(), true, false, duration, "rain");
    }

    /**
     * Set weather to thunder with default duration
     */
    private static int setThunder(CommandContext<CommandSourceStack> context) {
        return setWeather(context.getSource(), true, true, DEFAULT_DURATION, "thunder");
    }

    /**
     * Set weather to thunder with specified duration
     */
    private static int setThunderWithDuration(CommandContext<CommandSourceStack> context) {
        int duration = IntegerArgumentType.getInteger(context, "duration");
        return setWeather(context.getSource(), true, true, duration, "thunder");
    }

    /**
     * Core method to set weather
     */
    private static int setWeather(CommandSourceStack source, boolean raining, boolean thundering, int duration, String weatherType) {
        try {
            ServerLevel level = source.getLevel();
            
            // Set rain state
            level.setWeatherParameters(0, duration, raining, thundering);
            
            // Format duration for display
            String durationText = formatDuration(duration);
            
            source.sendSuccess(() -> Component.literal("Set weather to " + weatherType + " for " + durationText), true);
            return 1;
            
        } catch (Exception e) {
            source.sendFailure(Component.literal("Failed to set weather: " + e.getMessage()));
            return 0;
        }
    }

    /**
     * Format duration in ticks to a human-readable format
     */
    private static String formatDuration(int ticks) {
        if (ticks == 0) {
            return "indefinitely";
        }
        
        int seconds = ticks / 20; // 20 ticks per second
        
        if (seconds < 60) {
            return seconds + " seconds";
        } else if (seconds < 3600) {
            int minutes = seconds / 60;
            int remainingSeconds = seconds % 60;
            if (remainingSeconds == 0) {
                return minutes + " minutes";
            } else {
                return minutes + " minutes and " + remainingSeconds + " seconds";
            }
        } else {
            int hours = seconds / 3600;
            int remainingMinutes = (seconds % 3600) / 60;
            if (remainingMinutes == 0) {
                return hours + " hours";
            } else {
                return hours + " hours and " + remainingMinutes + " minutes";
            }
        }
    }
}
