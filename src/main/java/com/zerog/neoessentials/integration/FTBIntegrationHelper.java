package com.zerog.neoessentials.integration;

import net.neoforged.fml.ModList;

public class FTBIntegrationHelper {
    public static boolean isFTBTeamsLoaded() {
        return ModList.get().isLoaded("ftbteams");
    }

    public static boolean isFTBRanksLoaded() {
        return ModList.get().isLoaded("ftbranks");
    }

    public static boolean isFTBLibraryLoaded() {
        return ModList.get().isLoaded("ftblibrary");
    }

    // Example usage: wrap FTB API calls in these checks
    public static void safeTeamInfo(net.minecraft.server.level.ServerPlayer player) {
        if (isFTBTeamsLoaded()) {
            // FTBTeams API usage here (reflection or try/catch recommended)
            player.sendSystemMessage(net.minecraft.network.chat.Component.literal("FTBTeams is loaded. Team info would be shown here."));
        } else {
            player.sendSystemMessage(net.minecraft.network.chat.Component.literal("FTBTeams is not installed."));
        }
    }

    public static void safeRankInfo(net.minecraft.server.level.ServerPlayer player) {
        if (isFTBRanksLoaded()) {
            // FTBRanks API usage here (reflection or try/catch recommended)
            player.sendSystemMessage(net.minecraft.network.chat.Component.literal("FTBRanks is loaded. Rank info would be shown here."));
        } else {
            player.sendSystemMessage(net.minecraft.network.chat.Component.literal("FTBRanks is not installed."));
        }
    }
}
