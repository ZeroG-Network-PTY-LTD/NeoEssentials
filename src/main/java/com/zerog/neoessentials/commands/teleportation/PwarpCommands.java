package com.zerog.neoessentials.commands.teleportation;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.zerog.neoessentials.api.permissions.PermissionAPI;
import com.zerog.neoessentials.teleportation.Warp.WarpManager;
import com.zerog.neoessentials.teleportation.TeleportLocation;
import com.zerog.neoessentials.util.MessageUtil;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.level.ServerPlayer;

/**
 * Commands for player warps:
 * - /pwarp <name> - Teleport to your player warp
 * - /setpwarp <name> - Create a player warp
 * - /delpwarp <name> - Delete a player warp
 * - /pwarps - List your player warps
 */
public class PwarpCommands {
    private static final String PERMISSION_PWARP = "neoessentials.teleport.pwarp";
    private static final String PERMISSION_SETPWARP = "neoessentials.teleport.pwarp.create";
    private static final String PERMISSION_DELPWARP = "neoessentials.teleport.pwarp.delete";
    private static final String PERMISSION_PWARPS = "neoessentials.teleport.pwarp.list";

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        // Check if teleportation module is enabled
        if (!com.zerog.neoessentials.config.ConfigManager.getInstance().isTeleportationEnabled()) {
            return;
        }

        WarpManager warpManager = WarpManager.getInstance();
        if (!warpManager.isPlayerWarpsEnabled()) return;

        com.zerog.neoessentials.config.ConfigManager config = com.zerog.neoessentials.config.ConfigManager.getInstance();

        // /pwarp <name>
        if (config.isCommandEnabled("pwarp")) {
            dispatcher.register(Commands.literal("pwarp")
                .requires(source -> source.getEntity() instanceof ServerPlayer player && PermissionAPI.hasPermission(player.getUUID(), PERMISSION_PWARP))
                .then(Commands.argument("name", StringArgumentType.word())
                    .executes(PwarpCommands::executePwarp)
                )
            );
        }
        // /setpwarp <name>
        if (config.isCommandEnabled("setpwarp")) {
            dispatcher.register(Commands.literal("setpwarp")
                .requires(source -> source.getEntity() instanceof ServerPlayer player && PermissionAPI.hasPermission(player.getUUID(), PERMISSION_SETPWARP))
                .then(Commands.argument("name", StringArgumentType.word())
                    .executes(PwarpCommands::executeSetPwarp)
                )
            );
        }
        // /delpwarp <name>
        if (config.isCommandEnabled("delpwarp")) {
            dispatcher.register(Commands.literal("delpwarp")
                .requires(source -> source.getEntity() instanceof ServerPlayer player && PermissionAPI.hasPermission(player.getUUID(), PERMISSION_DELPWARP))
                .then(Commands.argument("name", StringArgumentType.word())
                    .executes(PwarpCommands::executeDelPwarp)
                )
            );
        }
        // /pwarps
        if (config.isCommandEnabled("pwarps")) {
            dispatcher.register(Commands.literal("pwarps")
                .requires(source -> source.getEntity() instanceof ServerPlayer player && PermissionAPI.hasPermission(player.getUUID(), PERMISSION_PWARPS))
                .executes(PwarpCommands::executePwarps)
            );
        }
    }

    private static int executePwarp(CommandContext<CommandSourceStack> context) {
        ServerPlayer player = (ServerPlayer) context.getSource().getEntity();
        String warpName = StringArgumentType.getString(context, "name");
        WarpManager warpManager = WarpManager.getInstance();
        // Jail escape prevention
        com.zerog.neoessentials.config.ConfigManager config = com.zerog.neoessentials.config.ConfigManager.getInstance();
        com.zerog.neoessentials.moderation.JailManager jailManager = com.zerog.neoessentials.moderation.JailManager.getInstance();
        if (config.isPreventJailEscapeEnabled() && jailManager.isPlayerJailed(player.getUUID())) {
            context.getSource().sendFailure(com.zerog.neoessentials.util.MessageUtil.error("commands.neoessentials.jail.prevent_escape"));
            return 0;
        }
        TeleportLocation location = warpManager.getPlayerWarp(player, warpName);
        if (location == null) {
            context.getSource().sendFailure(MessageUtil.error("commands.neoessentials.teleport.warp.not_found", warpName));
            return 0;
        }
        // Teleport
        warpManager.teleportToWarp(player, warpName); // Reuse teleport logic (may need adjustment)
        return 1;
    }

    private static int executeSetPwarp(CommandContext<CommandSourceStack> context) {
        ServerPlayer player = (ServerPlayer) context.getSource().getEntity();
        String warpName = StringArgumentType.getString(context, "name");
        WarpManager warpManager = WarpManager.getInstance();
        TeleportLocation location = new TeleportLocation(player);
        if (warpManager.createPlayerWarp(player, warpName, location)) {
            return 1;
        }
        return 0;
    }

    private static int executeDelPwarp(CommandContext<CommandSourceStack> context) {
        ServerPlayer player = (ServerPlayer) context.getSource().getEntity();
        String warpName = StringArgumentType.getString(context, "name");
        WarpManager warpManager = WarpManager.getInstance();
        if (warpManager.deletePlayerWarp(player, warpName)) {
            return 1;
        }
        return 0;
    }

    private static int executePwarps(CommandContext<CommandSourceStack> context) {
        ServerPlayer player = (ServerPlayer) context.getSource().getEntity();
        WarpManager warpManager = WarpManager.getInstance();
        var names = warpManager.getPlayerWarpNames(player);
        if (names.isEmpty()) {
            // coloredText(), not component(localize(...)) — localize() already returns fully
            // resolved text; component() would treat that resolved text as a translation KEY
            // and re-run it through localize() again, corrupting it via the humanizeKey()
            // fallback (same anti-pattern found and fixed across the moderation commands).
            player.sendSystemMessage(MessageUtil.coloredText(MessageUtil.localize("commands.neoessentials.teleport.warp.playerwarps_list_empty")));
        } else {
            StringBuilder builder = new StringBuilder();
            // Template is "{0}'s Player Warps ({1}/{2})" — needs the player's OWN name as {0},
            // not just count/max — previously only 2 args were passed for 3 placeholders, so
            // {0} showed the warp COUNT (not the player's name), {1} showed MAX (not count),
            // and {2} was left as a literal unresolved "{2}".
            builder.append(MessageUtil.localize("commands.neoessentials.teleport.warp.playerwarps_list_header",
                player.getName().getString(), names.size(), warpManager.getMaxPlayerWarps()));
            names.stream().sorted().forEach(name -> builder.append("\n").append(name));
            player.sendSystemMessage(MessageUtil.coloredText(builder.toString()));
        }
        return 1;
    }
}
