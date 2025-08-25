package com.zerog.neoessentials.commands;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;

public class CleanupTeamsCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
            net.minecraft.commands.Commands.literal("neessentials")
                .then(net.minecraft.commands.Commands.literal("cleanupteams")
                    .requires(source -> source.hasPermission(2))
                    .executes(ctx -> {
                        // Remove all NeoEssentials teams and objectives from the server scoreboard
                        var server = ctx.getSource().getServer();
                        var scoreboard = server.getScoreboard();
                        int removedTeams = 0;
                        int removedObjectives = 0;
                        // Remove teams with prefix "neo_"
                        for (var team : scoreboard.getPlayerTeams()) {
                            if (team.getName().startsWith("neo_")) {
                                scoreboard.removePlayerTeam(team);
                                removedTeams++;
                            }
                        }
                        // Remove objectives with prefix "neoess_sidebar_"
                        for (var objective : scoreboard.getObjectives()) {
                            if (objective.getName().startsWith("neoess_sidebar_")) {
                                scoreboard.removeObjective(objective);
                                removedObjectives++;
                            }
                        }
                        final String resultMsg = "NeoEssentials teams cleaned: " + removedTeams + ", objectives cleaned: " + removedObjectives;
                        ctx.getSource().sendSuccess(() -> Component.literal(resultMsg), true);
                        ctx.getSource().sendSuccess(() -> Component.literal("NeoEssentials teams and scoreboards cleaned up."), true);
                        return 1;
                    })
                )
        );
    }
}
