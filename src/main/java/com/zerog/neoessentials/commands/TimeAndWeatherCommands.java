package com.zerog.neoessentials.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.zerog.neoessentials.utils.MessageUtil;
import com.zerog.neoessentials.utils.PermissionUtil;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.storage.ServerLevelData;

/**
 * Handles time and weather related commands
 */
public class TimeAndWeatherCommands {

    /**
     * Register all time and weather related commands
     * 
     * @param dispatcher The command dispatcher
     */
    public void register(CommandDispatcher<CommandSourceStack> dispatcher) {        // /day - Set time to day
        dispatcher.register(
            Commands.literal("day")
                .requires(source -> PermissionUtil.hasPermission(source, "neoessentials.command.time"))
                .executes(this::executeDay)
        );
        
        // /night - Set time to night
        dispatcher.register(
            Commands.literal("night")
                .requires(source -> PermissionUtil.hasPermission(source, "neoessentials.command.time"))
                .executes(this::executeNight)
        );
          // /time <set|add> <time> - Set or add time
        dispatcher.register(
            Commands.literal("time")
                .requires(source -> PermissionUtil.hasPermission(source, "neoessentials.command.time"))
                .then(Commands.literal("set")
                    .then(Commands.argument("time", IntegerArgumentType.integer(0, 24000))
                        .executes(context -> executeTimeSet(context, IntegerArgumentType.getInteger(context, "time")))
                    )
                    .then(Commands.literal("day")
                        .executes(context -> executeTimeSet(context, 1000))
                    )
                    .then(Commands.literal("noon")
                        .executes(context -> executeTimeSet(context, 6000))
                    )
                    .then(Commands.literal("night")
                        .executes(context -> executeTimeSet(context, 13000))
                    )
                    .then(Commands.literal("midnight")
                        .executes(context -> executeTimeSet(context, 18000))
                    )
                    .then(Commands.literal("morning")
                        .executes(context -> executeTimeSet(context, 0))
                    )
                    .then(Commands.literal("sunset")
                        .executes(context -> executeTimeSet(context, 12000))
                    )
                    .then(Commands.literal("sunrise")
                        .executes(context -> executeTimeSet(context, 23000))
                    )
                )
                .then(Commands.literal("add")
                    .then(Commands.argument("time", IntegerArgumentType.integer(0, 24000))
                        .executes(context -> executeTimeAdd(context, IntegerArgumentType.getInteger(context, "time")))
                    )
                )
        );
          // /weather <clear|rain|thunder> [duration] - Set weather
        dispatcher.register(
            Commands.literal("weather")
                .requires(source -> PermissionUtil.hasPermission(source, "neoessentials.command.weather"))
                .then(Commands.literal("clear")
                    .executes(context -> executeWeather(context, "clear", 6000))
                    .then(Commands.argument("duration", IntegerArgumentType.integer(1, 1000000))
                        .executes(context -> executeWeather(context, "clear", IntegerArgumentType.getInteger(context, "duration") * 20))
                    )
                )
                .then(Commands.literal("rain")
                    .executes(context -> executeWeather(context, "rain", 6000))
                    .then(Commands.argument("duration", IntegerArgumentType.integer(1, 1000000))
                        .executes(context -> executeWeather(context, "rain", IntegerArgumentType.getInteger(context, "duration") * 20))
                    )
                )
                .then(Commands.literal("thunder")
                    .executes(context -> executeWeather(context, "thunder", 6000))
                    .then(Commands.argument("duration", IntegerArgumentType.integer(1, 1000000))
                        .executes(context -> executeWeather(context, "thunder", IntegerArgumentType.getInteger(context, "duration") * 20))
                    )
                )
        );
    }
    
    /**
     * Execute the /day command
     * 
     * @param context The command context
     * @return Command result
     */
    private int executeDay(CommandContext<CommandSourceStack> context) {
        return executeTimeSet(context, 1000);
    }
    
    /**
     * Execute the /night command
     * 
     * @param context The command context
     * @return Command result
     */
    private int executeNight(CommandContext<CommandSourceStack> context) {
        return executeTimeSet(context, 13000);
    }
      /**
     * Execute the /time set command
     * 
     * @param context The command context
     * @param time The time to set
     * @return Command result
     */
    private int executeTimeSet(CommandContext<CommandSourceStack> context, int time) {
        try {
            // Set the time in all dimensions/levels, not just the current one
            for (ServerLevel serverLevel : context.getSource().getServer().getAllLevels()) {
                ((ServerLevelData) serverLevel.getLevelData()).setDayTime(time);
            }
            
            // Send message with more user-friendly time names
            String timeDescription;
            switch (time) {
                case 0:
                    timeDescription = "morning (dawn)";
                    break;
                case 1000:
                    timeDescription = "day";
                    break;
                case 6000:
                    timeDescription = "noon";
                    break;
                case 12000:
                    timeDescription = "sunset";
                    break;
                case 13000:
                    timeDescription = "night";
                    break;
                case 18000:
                    timeDescription = "midnight";
                    break;
                case 23000:
                    timeDescription = "sunrise";
                    break;
                default:
                    timeDescription = String.format("%d (raw tick value)", time);
                    break;
            }
            
            MutableComponent message = Component.literal("Time set to " + timeDescription);
            
            if (context.getSource().getEntity() instanceof ServerPlayer player) {
                MessageUtil.sendSuccess(player, message);
            } else {
                context.getSource().sendSuccess(() -> message, true);
            }
            
            return 1;
        } catch (Exception e) {
            MutableComponent errorMessage = Component.literal("Failed to set time: " + e.getMessage());
            context.getSource().sendFailure(errorMessage);
            return 0;
        }
    }
    
    /**
     * Execute the /time add command
     * 
     * @param context The command context
     * @param time The time to add
     * @return Command result
     */    private int executeTimeAdd(CommandContext<CommandSourceStack> context, int time) {
        // Get the time from the current level
        long currentTime = context.getSource().getLevel().getDayTime();
        long newTime = currentTime + time;
        
        // Add the time in all dimensions/levels
        for (ServerLevel serverLevel : context.getSource().getServer().getAllLevels()) {
            ((ServerLevelData) serverLevel.getLevelData()).setDayTime(newTime);
        }
        
        // Send message
        MutableComponent message = Component.literal("Added " + time + " to the time");
        
        if (context.getSource().getEntity() instanceof ServerPlayer player) {
            MessageUtil.sendSuccess(player, message);
        } else {
            context.getSource().sendSuccess(() -> message, true);
        }
        
        return 1;
    }
      /**
     * Execute the /weather command
     * 
     * @param context The command context
     * @param weatherType The type of weather
     * @param duration The duration in ticks
     * @return Command result
     */
    private int executeWeather(CommandContext<CommandSourceStack> context, String weatherType, int duration) {
        try {
            // Apply weather to all dimensions, with special handling for each weather type
            for (ServerLevel level : context.getSource().getServer().getAllLevels()) {
                // Skip dimensions that don't support weather like the Nether and End
                if (level.dimension() == net.minecraft.world.level.Level.NETHER || 
                    level.dimension() == net.minecraft.world.level.Level.END) {
                    continue;
                }
                
                switch (weatherType) {
                    case "clear":
                        // Clear weather by setting rain and thunder to false
                        level.setWeatherParameters(duration, 0, false, false);
                        break;
                    case "rain":
                        // Set rain duration and enable rain
                        level.setWeatherParameters(0, duration, true, false);
                        break;
                    case "thunder":
                        // Set thunder duration and enable both rain and thunder
                        level.setWeatherParameters(0, duration, true, true);
                        break;
                    default:
                        return 0;
                }
            }
            
            // Format duration in a user-friendly way
            String durationStr;
            int seconds = duration / 20;
            if (seconds > 60) {
                int minutes = seconds / 60;
                durationStr = minutes + " minute" + (minutes > 1 ? "s" : "");
            } else {
                durationStr = seconds + " second" + (seconds > 1 ? "s" : "");
            }
            
            // Send message
            MutableComponent message = Component.literal("Weather set to " + weatherType + " for " + durationStr);
            
            if (context.getSource().getEntity() instanceof ServerPlayer player) {
                MessageUtil.sendSuccess(player, message);
            } else {
                context.getSource().sendSuccess(() -> message, true);
            }
            
            return 1;
        } catch (Exception e) {
            MutableComponent errorMessage = Component.literal("Failed to set weather: " + e.getMessage());
            context.getSource().sendFailure(errorMessage);
            return 0;
        }
    }
}
