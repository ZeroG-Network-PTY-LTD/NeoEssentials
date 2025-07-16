package com.zerog.neoessentials.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.zerog.neoessentials.NeoEssentials;
import com.zerog.neoessentials.data.JailManager;
import com.zerog.neoessentials.utils.TextUtil;
import com.zerog.neoessentials.utils.TimeUtil;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.TimeArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import java.util.Date;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Implements jail-related commands.
 */
public class JailCommands {

    private final SimpleCommandExceptionType JAIL_ALREADY_EXISTS = new SimpleCommandExceptionType(
            Component.literal("A jail with that name already exists."));
    private final SimpleCommandExceptionType JAIL_DOESNT_EXIST = new SimpleCommandExceptionType(
            Component.literal("That jail doesn't exist."));

    /**
     * Registers all jail-related commands with the dispatcher.
     *
     * @param dispatcher The command dispatcher
     */
    public void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        registerSetJailCommand(dispatcher);
        registerDelJailCommand(dispatcher);
        registerJailsCommand(dispatcher);
        registerToggleJailCommand(dispatcher);
    }

    /**
     * Registers the setjail command.
     *
     * @param dispatcher The command dispatcher
     */
    private void registerSetJailCommand(CommandDispatcher<CommandSourceStack> dispatcher) {
        // /setjail <name>
        LiteralArgumentBuilder<CommandSourceStack> setJailCommand = Commands.literal("setjail")
                .requires(source -> CommandManager.hasPermission(source, "neoessentials.setjail"))
                .then(Commands.argument("name", StringArgumentType.word())
                        .executes(this::executeSetJail));

        // Register aliases
        dispatcher.register(setJailCommand);
        dispatcher.register(Commands.literal("createjail")
                .requires(source -> CommandManager.hasPermission(source, "neoessentials.setjail"))
                .then(Commands.argument("name", StringArgumentType.word())
                        .executes(this::executeSetJail)));
    }

    /**
     * Registers the deljail command.
     *
     * @param dispatcher The command dispatcher
     */
    private void registerDelJailCommand(CommandDispatcher<CommandSourceStack> dispatcher) {
        // /deljail <name>
        LiteralArgumentBuilder<CommandSourceStack> delJailCommand = Commands.literal("deljail")
                .requires(source -> CommandManager.hasPermission(source, "neoessentials.deljail"))
                .then(Commands.argument("name", StringArgumentType.word())
                        .executes(this::executeDelJail));

        // Register aliases
        dispatcher.register(delJailCommand);
        dispatcher.register(Commands.literal("remjail")
                .requires(source -> CommandManager.hasPermission(source, "neoessentials.deljail"))
                .then(Commands.argument("name", StringArgumentType.word())
                        .executes(this::executeDelJail)));
        dispatcher.register(Commands.literal("rmjail")
                .requires(source -> CommandManager.hasPermission(source, "neoessentials.deljail"))
                .then(Commands.argument("name", StringArgumentType.word())
                        .executes(this::executeDelJail)));
    }

    /**
     * Registers the jails command.
     *
     * @param dispatcher The command dispatcher
     */
    private void registerJailsCommand(CommandDispatcher<CommandSourceStack> dispatcher) {
        // /jails
        LiteralArgumentBuilder<CommandSourceStack> jailsCommand = Commands.literal("jails")
                .requires(source -> CommandManager.hasPermission(source, "neoessentials.jails"))
                .executes(this::executeJails);

        dispatcher.register(jailsCommand);
    }

    /**
     * Registers the togglejail command.
     *
     * @param dispatcher The command dispatcher
     */
    private void registerToggleJailCommand(CommandDispatcher<CommandSourceStack> dispatcher) {
        // /togglejail <player> [jailname] [time]
        LiteralArgumentBuilder<CommandSourceStack> toggleJailCommand = Commands.literal("togglejail")
                .requires(source -> CommandManager.hasPermission(source, "neoessentials.jail"))
                .then(Commands.argument("player", EntityArgument.player())
                        .executes(this::executeToggleJailNoArgs)
                        .then(Commands.argument("jailname", StringArgumentType.word())
                                .executes(this::executeToggleJail)
                                .then(Commands.argument("time", StringArgumentType.greedyString())
                                        .executes(this::executeToggleJailWithTime))));

        // Register aliases
        dispatcher.register(toggleJailCommand);
        dispatcher.register(Commands.literal("jail")
                .requires(source -> CommandManager.hasPermission(source, "neoessentials.jail"))
                .then(Commands.argument("player", EntityArgument.player())
                        .executes(this::executeToggleJailNoArgs)
                        .then(Commands.argument("jailname", StringArgumentType.word())
                                .executes(this::executeToggleJail)
                                .then(Commands.argument("time", StringArgumentType.greedyString())
                                        .executes(this::executeToggleJailWithTime)))));
        dispatcher.register(Commands.literal("unjail")
                .requires(source -> CommandManager.hasPermission(source, "neoessentials.jail"))
                .then(Commands.argument("player", EntityArgument.player())
                        .executes(this::executeUnjail)));
    }

    /**
     * Executes the setjail command.
     *
     * @param context The command context
     * @return 1 if successful, 0 otherwise
     */
    private int executeSetJail(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        ServerPlayer player = source.getPlayerOrException();
        String jailName = StringArgumentType.getString(context, "name");

        JailManager jailManager = NeoEssentials.getInstance().getDataManager().getJailManager();
        
        if (jailManager.jailExists(jailName)) {
            throw JAIL_ALREADY_EXISTS.create();
        }
        
        ServerLevel level = player.serverLevel();
        BlockPos pos = player.blockPosition();
        
        jailManager.addJail(jailName, level, pos);
        
        source.sendSuccess(() -> Component.literal(TextUtil.formatText(
                "&aJail &6" + jailName + " &ahas been set at your location.")), true);
        
        return 1;
    }

    /**
     * Executes the deljail command.
     *
     * @param context The command context
     * @return 1 if successful, 0 otherwise
     */
    private int executeDelJail(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        String jailName = StringArgumentType.getString(context, "name");

        JailManager jailManager = NeoEssentials.getInstance().getDataManager().getJailManager();
        
        if (!jailManager.jailExists(jailName)) {
            throw JAIL_DOESNT_EXIST.create();
        }
        
        jailManager.removeJail(jailName);
        
        source.sendSuccess(() -> Component.literal(TextUtil.formatText(
                "&aJail &6" + jailName + " &ahas been removed.")), true);
        
        return 1;
    }

    /**
     * Executes the jails command.
     *
     * @param context The command context
     * @return 1 if successful, 0 otherwise
     */
    private int executeJails(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        JailManager jailManager = NeoEssentials.getInstance().getDataManager().getJailManager();
        
        StringBuilder sb = new StringBuilder();
        sb.append("&aAvailable Jails: &6");
        
        boolean first = true;
        for (Map.Entry<String, JailManager.JailLocation> entry : jailManager.getJails()) {
            if (!first) {
                sb.append("&a, &6");
            }
            sb.append(entry.getKey());
            first = false;
        }
        
        if (first) {
            sb.append("&cNone");
        }
        
        source.sendSuccess(() -> Component.literal(TextUtil.formatText(sb.toString())), false);
        
        return 1;
    }

    /**
     * Executes the togglejail command with no jail name or time arguments.
     *
     * @param context The command context
     * @return 1 if successful, 0 otherwise
     */
    private int executeToggleJailNoArgs(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        ServerPlayer targetPlayer = EntityArgument.getPlayer(context, "player");
        
        JailManager jailManager = NeoEssentials.getInstance().getDataManager().getJailManager();
        
        if (jailManager.isJailed(targetPlayer)) {
            // Unjail the player
            return executeUnjail(context);
        } else {
            // Need a jail name to jail the player
            source.sendFailure(Component.literal(TextUtil.formatText(
                    "&cYou need to specify a jail name to jail this player.")));
            return 0;
        }
    }

    /**
     * Executes the togglejail command with a jail name but no time argument.
     *
     * @param context The command context
     * @return 1 if successful, 0 otherwise
     */
    private int executeToggleJail(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        ServerPlayer targetPlayer = EntityArgument.getPlayer(context, "player");
        String jailName = StringArgumentType.getString(context, "jailname");
        
        JailManager jailManager = NeoEssentials.getInstance().getDataManager().getJailManager();
        
        if (jailManager.isJailed(targetPlayer)) {
            // Unjail the player
            return executeUnjail(context);
        } else {
            // Jail the player indefinitely
            if (!jailManager.jailExists(jailName)) {
                throw JAIL_DOESNT_EXIST.create();
            }
            
<<<<<<< HEAD
<<<<<<< HEAD
<<<<<<< HEAD
<<<<<<< HEAD
            if (targetPlayer.hasPermissions(4) || com.zerog.neoessentials.utils.PermissionUtil.hasPermission((ServerPlayer)targetPlayer, "neoessentials.jail.exempt")) {
=======
            if (targetPlayer.hasPermissions(4) || CommandManager.hasPermission(targetPlayer, "neoessentials.jail.exempt")) {
>>>>>>> 2b0efb3 (Implement powertool and jail management systems)
=======
            if (targetPlayer.hasPermissions(4) || com.zerog.neoessentials.utils.PermissionUtil.hasPermission(targetPlayer, "neoessentials.jail.exempt")) {
>>>>>>> e7f85f6 (fix: Update permission check in JailCommands to use PermissionUtil)
=======
            if (targetPlayer.hasPermissions(4) || com.zerog.neoessentials.utils.PermissionUtil.hasPermission((ServerPlayer)targetPlayer, "neoessentials.jail.exempt")) {
>>>>>>> 18240f3 (fix: Update permission checks in JailCommands, MessagingCommands, and KitManager to ensure proper player type handling)
=======
            if (targetPlayer.hasPermissions(4) || com.zerog.neoessentials.utils.PermissionUtil.hasPermission((ServerPlayer)targetPlayer, "neoessentials.jail.exempt")) {
>>>>>>> 4fee73b0b24b6301947b09da0d1e52696e353f1d
                if (!source.hasPermission(4)) { // If the source is not op level 4
                    source.sendFailure(Component.literal(TextUtil.formatText(
                            "&cYou cannot jail an operator or someone with the exempt permission.")));
                    return 0;
                }
            }
            
            jailManager.jailPlayer(targetPlayer, jailName, -1, "Jailed by " + source.getTextName());
            
            // Notify the player and staff
            targetPlayer.sendSystemMessage(Component.literal(TextUtil.formatText(
                    "&cYou have been jailed in &6" + jailName + " &cindefinitely.")));
            
            source.sendSuccess(() -> Component.literal(TextUtil.formatText(
                    "&aPlayer &6" + targetPlayer.getScoreboardName() + " &ahas been jailed in &6" + jailName + " &aindefinitely.")), true);
            
            return 1;
        }
    }

    /**
     * Executes the togglejail command with a jail name and time argument.
     *
     * @param context The command context
     * @return 1 if successful, 0 otherwise
     */
    private int executeToggleJailWithTime(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        ServerPlayer targetPlayer = EntityArgument.getPlayer(context, "player");
        String jailName = StringArgumentType.getString(context, "jailname");
        String timeString = StringArgumentType.getString(context, "time");
        
        JailManager jailManager = NeoEssentials.getInstance().getDataManager().getJailManager();
        
        if (jailManager.isJailed(targetPlayer)) {
            // Unjail the player
            return executeUnjail(context);
        } else {
            // Jail the player for a specific time
            if (!jailManager.jailExists(jailName)) {
                throw JAIL_DOESNT_EXIST.create();
            }
            
<<<<<<< HEAD
<<<<<<< HEAD
<<<<<<< HEAD
<<<<<<< HEAD
            if (targetPlayer.hasPermissions(4) || com.zerog.neoessentials.utils.PermissionUtil.hasPermission((ServerPlayer)targetPlayer, "neoessentials.jail.exempt")) {
=======
            if (targetPlayer.hasPermissions(4) || CommandManager.hasPermission(targetPlayer, "neoessentials.jail.exempt")) {
>>>>>>> 2b0efb3 (Implement powertool and jail management systems)
=======
            if (targetPlayer.hasPermissions(4) || com.zerog.neoessentials.utils.PermissionUtil.hasPermission(targetPlayer, "neoessentials.jail.exempt")) {
>>>>>>> e7f85f6 (fix: Update permission check in JailCommands to use PermissionUtil)
=======
            if (targetPlayer.hasPermissions(4) || com.zerog.neoessentials.utils.PermissionUtil.hasPermission((ServerPlayer)targetPlayer, "neoessentials.jail.exempt")) {
>>>>>>> 18240f3 (fix: Update permission checks in JailCommands, MessagingCommands, and KitManager to ensure proper player type handling)
=======
            if (targetPlayer.hasPermissions(4) || com.zerog.neoessentials.utils.PermissionUtil.hasPermission((ServerPlayer)targetPlayer, "neoessentials.jail.exempt")) {
>>>>>>> 4fee73b0b24b6301947b09da0d1e52696e353f1d
                if (!source.hasPermission(4)) { // If the source is not op level 4
                    source.sendFailure(Component.literal(TextUtil.formatText(
                            "&cYou cannot jail an operator or someone with the exempt permission.")));
                    return 0;
                }
            }
            
            try {
                Date releaseTime = TimeUtil.parseTimeSpecification(timeString);
                long duration = (releaseTime.getTime() - System.currentTimeMillis()) / 1000; // Convert to seconds
                
                jailManager.jailPlayer(targetPlayer, jailName, duration, "Jailed by " + source.getTextName() + " for " + timeString);
                
                // Notify the player and staff
                targetPlayer.sendSystemMessage(Component.literal(TextUtil.formatText(
                        "&cYou have been jailed in &6" + jailName + " &cfor &6" + TimeUtil.formatTimeDuration(duration) + "&c.")));
                
                source.sendSuccess(() -> Component.literal(TextUtil.formatText(
                        "&aPlayer &6" + targetPlayer.getScoreboardName() + " &ahas been jailed in &6" + jailName + 
                        " &afor &6" + TimeUtil.formatTimeDuration(duration) + "&a.")), true);
                
                return 1;
            } catch (IllegalArgumentException e) {
                source.sendFailure(Component.literal(TextUtil.formatText(
                        "&cInvalid time format. Use format like: 1d2h30m")));
                return 0;
            }
        }
    }

    /**
     * Executes the unjail command.
     *
     * @param context The command context
     * @return 1 if successful, 0 otherwise
     */
    private int executeUnjail(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        ServerPlayer targetPlayer = EntityArgument.getPlayer(context, "player");
        
        JailManager jailManager = NeoEssentials.getInstance().getDataManager().getJailManager();
        
        if (!jailManager.isJailed(targetPlayer)) {
            source.sendFailure(Component.literal(TextUtil.formatText(
                    "&cPlayer &6" + targetPlayer.getScoreboardName() + " &cis not jailed.")));
            return 0;
        }
        
        // Get jail data before unjailing for the message
        JailManager.JailData jailData = jailManager.getJailData(targetPlayer);
        String jailName = jailData != null ? jailData.getJailName() : "unknown";
        
        jailManager.unjailPlayer(targetPlayer, true);
        
        // Notify the player and staff
        targetPlayer.sendSystemMessage(Component.literal(TextUtil.formatText(
                "&aYou have been released from jail.")));
        
        source.sendSuccess(() -> Component.literal(TextUtil.formatText(
                "&aPlayer &6" + targetPlayer.getScoreboardName() + " &ahas been released from &6" + jailName + "&a.")), true);
        
        return 1;
    }
}
