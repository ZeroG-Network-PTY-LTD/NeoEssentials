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
import net.minecraft.world.level.GameRules;

public class TimeCommand {
    private static final int DAY_TIME = 1000;
    private static final int NIGHT_TIME = 13000;
    private static final int NOON_TIME = 6000;
    private static final int MIDNIGHT_TIME = 18000;

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("time")
            .requires(source -> PermissionUtil.hasPermissionOrOp(source, PermissionNodes.MODERATION_BASIC))
            .then(Commands.literal("set")
                .then(Commands.literal("day")
                    .executes(TimeCommand::setDay)
                )
                .then(Commands.literal("night")
                    .executes(TimeCommand::setNight)
                )
                .then(Commands.literal("noon")
                    .executes(TimeCommand::setNoon)
                )
                .then(Commands.literal("midnight")
                    .executes(TimeCommand::setMidnight)
                )
                .then(Commands.argument("time", IntegerArgumentType.integer(0))
                    .executes(TimeCommand::setTime)
                )
            )
            .then(Commands.literal("add")
                .then(Commands.argument("time", IntegerArgumentType.integer(0))
                    .executes(TimeCommand::addTime)
                )
            )
            .then(Commands.literal("query")
                .then(Commands.literal("daytime")
                    .executes(TimeCommand::queryDaytime)
                )
                .then(Commands.literal("gametime")
                    .executes(TimeCommand::queryGametime)
                )
            )
            .then(Commands.literal("freeze")
                .executes(TimeCommand::freezeTime)
            )
            .then(Commands.literal("unfreeze")
                .executes(TimeCommand::unfreezeTime)
            )
        );
    }

    /**
     * Set time to day (1000 ticks)
     */
    private static int setDay(CommandContext<CommandSourceStack> context) {
        return setWorldTime(context.getSource(), DAY_TIME, "day");
    }

    /**
     * Set time to night (13000 ticks)
     */
    private static int setNight(CommandContext<CommandSourceStack> context) {
        return setWorldTime(context.getSource(), NIGHT_TIME, "night");
    }

    /**
     * Set time to noon (6000 ticks)
     */
    private static int setNoon(CommandContext<CommandSourceStack> context) {
        return setWorldTime(context.getSource(), NOON_TIME, "noon");
    }

    /**
     * Set time to midnight (18000 ticks)
     */
    private static int setMidnight(CommandContext<CommandSourceStack> context) {
        return setWorldTime(context.getSource(), MIDNIGHT_TIME, "midnight");
    }

    /**
     * Set time to specific value
     */
    private static int setTime(CommandContext<CommandSourceStack> context) {
        int time = IntegerArgumentType.getInteger(context, "time");
        return setWorldTime(context.getSource(), time, String.valueOf(time));
    }

    /**
     * Add time to current world time
     */
    private static int addTime(CommandContext<CommandSourceStack> context) {
        int timeToAdd = IntegerArgumentType.getInteger(context, "time");
        CommandSourceStack source = context.getSource();
        
        try {
            ServerLevel level = source.getLevel();
            long currentTime = level.getDayTime();
            long newTime = currentTime + timeToAdd;
            
            level.setDayTime(newTime);
            
            source.sendSuccess(() -> Component.literal("Added " + timeToAdd + " to the time"), true);
            return 1;
            
        } catch (Exception e) {
            source.sendFailure(Component.literal("Failed to add time: " + e.getMessage()));
            return 0;
        }
    }

    /**
     * Query current daytime
     */
    private static int queryDaytime(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        
        try {
            ServerLevel level = source.getLevel();
            long dayTime = level.getDayTime() % 24000;
            
            source.sendSuccess(() -> Component.literal("The time is " + dayTime), false);
            return (int) dayTime;
            
        } catch (Exception e) {
            source.sendFailure(Component.literal("Failed to query time: " + e.getMessage()));
            return 0;
        }
    }

    /**
     * Query current gametime
     */
    private static int queryGametime(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        
        try {
            ServerLevel level = source.getLevel();
            long gameTime = level.getGameTime();
            
            source.sendSuccess(() -> Component.literal("The game time is " + gameTime), false);
            return (int) (gameTime % Integer.MAX_VALUE);
            
        } catch (Exception e) {
            source.sendFailure(Component.literal("Failed to query game time: " + e.getMessage()));
            return 0;
        }
    }

    /**
     * Freeze time (disable daylight cycle)
     */
    private static int freezeTime(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        
        try {
            ServerLevel level = source.getLevel();
            level.getGameRules().getRule(GameRules.RULE_DAYLIGHT).set(false, level.getServer());
            
            source.sendSuccess(() -> Component.literal("Time has been frozen"), true);
            return 1;
            
        } catch (Exception e) {
            source.sendFailure(Component.literal("Failed to freeze time: " + e.getMessage()));
            return 0;
        }
    }

    /**
     * Unfreeze time (enable daylight cycle)
     */
    private static int unfreezeTime(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        
        try {
            ServerLevel level = source.getLevel();
            level.getGameRules().getRule(GameRules.RULE_DAYLIGHT).set(true, level.getServer());
            
            source.sendSuccess(() -> Component.literal("Time has been unfrozen"), true);
            return 1;
            
        } catch (Exception e) {
            source.sendFailure(Component.literal("Failed to unfreeze time: " + e.getMessage()));
            return 0;
        }
    }

    /**
     * Core method to set world time
     */
    private static int setWorldTime(CommandSourceStack source, long time, String timeName) {
        try {
            ServerLevel level = source.getLevel();
            level.setDayTime(time);
            
            source.sendSuccess(() -> Component.literal("Set the time to " + timeName), true);
            return 1;
            
        } catch (Exception e) {
            source.sendFailure(Component.literal("Failed to set time: " + e.getMessage()));
            return 0;
        }
    }
}
