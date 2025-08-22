package com.zerog.neoessentials.commands;

import net.minecraft.server.level.ServerPlayer;
import com.zerog.neoessentials.permissions.PermissionManager;
import java.util.Arrays;
import java.util.List;

public class RoleCommand implements ICommand {
    @Override
    public void execute(ServerPlayer player, String[] args) {
        if (args.length < 2) {
            player.sendSystemMessage(net.minecraft.network.chat.Component.literal("Usage: /role <player> <role>"));
            return;
        }
        // Example: Find target player by name (stub)
        // In real code, use server API to get player by name
        ServerPlayer target = player; // Replace with actual lookup
        String role = args[1];
        PermissionManager.assignPlayerRole(target.getUUID(), role);
        player.sendSystemMessage(net.minecraft.network.chat.Component.literal("Assigned role '" + role + "' to " + target.getName().getString()));
    }

    @Override
    public List<String> tabComplete(ServerPlayer player, String[] args) {
        if (args.length == 2) {
            // Suggest available roles
            return Arrays.asList("admin", "moderator", "player");
        }
        return Arrays.asList();
    }

    @Override
    public List<String> getAliases() {
        return Arrays.asList();
    }
}
