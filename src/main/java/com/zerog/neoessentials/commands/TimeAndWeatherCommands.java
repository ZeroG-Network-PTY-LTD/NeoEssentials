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
        ServerLevel level = context.getSource().getLevel();
        
        // Set the time in the level
        ((ServerLevelData) level.getLevelData()).setDayTime(time);
        
        // Send message
        MutableComponent message;
        if (time == 1000) {
            message = Component.literal("Time set to day");
        } else if (time == 6000) {
            message = Component.literal("Time set to noon");
        } else if (time == 13000) {
            message = Component.literal("Time set to night");
        } else if (time == 18000) {
            message = Component.literal("Time set to midnight");
        } else {
            message = Component.literal("Time set to " + time);
        }
        
        if (context.getSource().getEntity() instanceof ServerPlayer player) {
            MessageUtil.sendSuccess(player, message);
        } else {
            context.getSource().sendSuccess(() -> message, true);
        }
        
        return 1;
    }
    
    /**
     * Execute the /time add command
     * 
     * @param context The command context
     * @param time The time to add
     * @return Command result
     */
    private int executeTimeAdd(CommandContext<CommandSourceStack> context, int time) {
        ServerLevel level = context.getSource().getLevel();
        
        // Add the time in the level
        ((ServerLevelData) level.getLevelData()).setDayTime(level.getDayTime() + time);
        
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
        ServerLevel level = context.getSource().getLevel();
        
        // Set the weather based on the type
        switch (weatherType) {
            case "clear":
                level.setWeatherParameters(0, 0, false, false);
                level.setWeatherParameters(duration, 0, false, false);
                break;
            case "rain":
                level.setWeatherParameters(0, duration, true, false);
                break;
            case "thunder":
                level.setWeatherParameters(0, duration, true, true);
                break;
        }
        
        // Send message
        MutableComponent message = Component.literal("Weather set to " + weatherType + " for " + (duration / 20) + " seconds");
        
        if (context.getSource().getEntity() instanceof ServerPlayer player) {
            MessageUtil.sendSuccess(player, message);
        } else {
            context.getSource().sendSuccess(() -> message, true);
        }
        
        return 1;
    }
}
