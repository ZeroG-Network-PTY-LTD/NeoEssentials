package com.zerog.neoessentials.commands.player;

import com.zerog.neoessentials.util.PermissionUtil;
import com.zerog.neoessentials.permissions.PermissionNodes;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.zerog.neoessentials.player.AchievementSystem;
import com.zerog.neoessentials.player.PlayerData;
import com.zerog.neoessentials.player.PlayerDataManager;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Command for viewing and managing achievements
 */
@SuppressWarnings("deprecation")
public class AchievementsCommand {
    private static final Logger LOGGER = LoggerFactory.getLogger(AchievementsCommand.class);
    
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("achievements")
            .executes(AchievementsCommand::showOwnAchievements)
            .then(Commands.argument("player", EntityArgument.player())
                .requires(source -> PermissionUtil.hasPermissionOrOp(source, PermissionNodes.ESSENTIALS_USE))
                .executes(AchievementsCommand::showPlayerAchievements))
            .then(Commands.literal("categories")
                .executes(AchievementsCommand::showCategories))
            .then(Commands.literal("list")
                .executes(AchievementsCommand::showAllAchievements))
            .then(Commands.literal("stats")
                .executes(AchievementsCommand::showAchievementStats))
        );
    }
    
    private static int showOwnAchievements(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        return showAchievements(context.getSource(), player);
    }
    
    private static int showPlayerAchievements(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer targetPlayer = EntityArgument.getPlayer(context, "player");
        return showAchievements(context.getSource(), targetPlayer);
    }
    
    private static int showCategories(CommandContext<CommandSourceStack> context) {
        try {
            AchievementSystem achievementSystem = AchievementSystem.getInstance();
            
            Component header = Component.literal(com.zerog.neoessentials.localization.LanguageManager.getInstance().getMessage("en", "achievements.categories.header"));
            context.getSource().sendSuccess(() -> header, false);
            for (AchievementSystem.AchievementCategory category : AchievementSystem.AchievementCategory.values()) {
                List<AchievementSystem.Achievement> categoryAchievements = achievementSystem.getAchievementsByCategory(category);
                Component categoryInfo = Component.literal(com.zerog.neoessentials.localization.LanguageManager.getInstance().getMessage("en", "achievements.categories.entry", category.getDisplayName(), categoryAchievements.size()));
                context.getSource().sendSuccess(() -> categoryInfo, false);
            }
            
            return 1;
        } catch (Exception e) {
            LOGGER.error("Error showing achievement categories", e);
            Component error = Component.literal(com.zerog.neoessentials.localization.LanguageManager.getInstance().getMessage("en", "achievements.categories.error"));
            context.getSource().sendSuccess(() -> error, false);
            return 0;
        }
    }
    
    private static int showAllAchievements(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        
        try {
            PlayerData playerData = PlayerDataManager.getInstance().getPlayerData(player.getUUID());
            Map<String, Long> achievements = playerData.getAchievements();
            Collection<AchievementSystem.Achievement> allAchievementsCollection = AchievementSystem.getInstance().getAllAchievements();
            
            Component header = Component.literal(com.zerog.neoessentials.localization.LanguageManager.getInstance().getMessage("en", "achievements.list.header"));
            context.getSource().sendSuccess(() -> header, false);
            for (AchievementSystem.Achievement achievement : allAchievementsCollection) {
                long progress = achievements.getOrDefault(achievement.getId(), 0L);
                boolean completed = progress >= achievement.getRequiredProgress();
                String status = completed ? com.zerog.neoessentials.localization.LanguageManager.getInstance().getMessage("en", "achievements.list.completed") : com.zerog.neoessentials.localization.LanguageManager.getInstance().getMessage("en", "achievements.list.incomplete");
                String progressText = completed ? com.zerog.neoessentials.localization.LanguageManager.getInstance().getMessage("en", "achievements.list.progress.completed") : com.zerog.neoessentials.localization.LanguageManager.getInstance().getMessage("en", "achievements.list.progress.incomplete", progress, achievement.getRequiredProgress());
                Component achievementInfo = Component.literal(com.zerog.neoessentials.localization.LanguageManager.getInstance().getMessage("en", "achievements.list.entry", status, achievement.getName(), achievement.getDescription(), progressText));
                context.getSource().sendSuccess(() -> achievementInfo, false);
            }
            
            return 1;
        } catch (Exception e) {
            LOGGER.error("Error showing all achievements for player " + player.getName().getString(), e);
            Component error = Component.literal(com.zerog.neoessentials.localization.LanguageManager.getInstance().getMessage("en", "achievements.list.error"));
            context.getSource().sendSuccess(() -> error, false);
            return 0;
        }
    }
    
    private static int showAchievementStats(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        
        try {
            PlayerData playerData = PlayerDataManager.getInstance().getPlayerData(player.getUUID());
            Map<String, Long> achievements = playerData.getAchievements();
            Collection<AchievementSystem.Achievement> allAchievementsCollection = AchievementSystem.getInstance().getAllAchievements();
            
            long completed = 0;
            long inProgress = 0;
            int totalScore = 0;
            
            for (AchievementSystem.Achievement achievement : allAchievementsCollection) {
                long progress = achievements.getOrDefault(achievement.getId(), 0L);
                if (progress >= achievement.getRequiredProgress()) {
                    completed++;
                    totalScore += achievement.getPoints();
                } else if (progress > 0) {
                    inProgress++;
                }
            }
            
            Component header = Component.literal(com.zerog.neoessentials.localization.LanguageManager.getInstance().getMessage("en", "achievements.stats.header"));
            context.getSource().sendSuccess(() -> header, false);
            Component completedInfo = Component.literal(com.zerog.neoessentials.localization.LanguageManager.getInstance().getMessage("en", "achievements.stats.completed", completed, allAchievementsCollection.size()));
            context.getSource().sendSuccess(() -> completedInfo, false);
            Component progressInfo = Component.literal(com.zerog.neoessentials.localization.LanguageManager.getInstance().getMessage("en", "achievements.stats.in_progress", inProgress));
            context.getSource().sendSuccess(() -> progressInfo, false);
            Component scoreInfo = Component.literal(com.zerog.neoessentials.localization.LanguageManager.getInstance().getMessage("en", "achievements.stats.score", totalScore));
            context.getSource().sendSuccess(() -> scoreInfo, false);
            double completionRate = (double) completed / allAchievementsCollection.size() * 100;
            Component rateInfo = Component.literal(com.zerog.neoessentials.localization.LanguageManager.getInstance().getMessage("en", "achievements.stats.completion_rate", String.format("%.1f", completionRate)));
            context.getSource().sendSuccess(() -> rateInfo, false);
            return 1;
        } catch (Exception e) {
            LOGGER.error("Error showing achievement stats for player " + player.getName().getString(), e);
            Component error = Component.literal(com.zerog.neoessentials.localization.LanguageManager.getInstance().getMessage("en", "achievements.stats.error"));
            context.getSource().sendSuccess(() -> error, false);
            return 0;
        }
    }
    
    private static int showAchievements(CommandSourceStack source, ServerPlayer targetPlayer) {
        try {
            PlayerData playerData = PlayerDataManager.getInstance().getPlayerData(targetPlayer.getUUID());
            Map<String, Long> achievements = playerData.getAchievements();
            Collection<AchievementSystem.Achievement> allAchievementsCollection = AchievementSystem.getInstance().getAllAchievements();
            
            boolean isOwnAchievements = source.getEntity() instanceof ServerPlayer player && 
                player.getUUID().equals(targetPlayer.getUUID());
            
            String targetName = isOwnAchievements ? com.zerog.neoessentials.localization.LanguageManager.getInstance().getMessage(targetPlayer, "achievements.your") : targetPlayer.getName().getString();
            Component header = Component.literal(com.zerog.neoessentials.localization.LanguageManager.getInstance().getMessage(targetPlayer, "achievements.header", targetName));
            source.sendSuccess(() -> header, false);
            long completed = 0;
            int totalScore = 0;
            for (AchievementSystem.Achievement achievement : allAchievementsCollection) {
                long progress = achievements.getOrDefault(achievement.getId(), 0L);
                if (progress >= achievement.getRequiredProgress()) {
                    completed++;
                    totalScore += achievement.getPoints();
                }
            }
            Component summary = Component.literal(com.zerog.neoessentials.localization.LanguageManager.getInstance().getMessage(targetPlayer, "achievements.summary", completed, allAchievementsCollection.size(), totalScore));
            source.sendSuccess(() -> summary, false);
            Component recentHeader = Component.literal(com.zerog.neoessentials.localization.LanguageManager.getInstance().getMessage(targetPlayer, "achievements.recent_header"));
            source.sendSuccess(() -> recentHeader, false);
            int shown = 0;
            for (AchievementSystem.Achievement achievement : allAchievementsCollection) {
                if (shown >= 5) break;
                long progress = achievements.getOrDefault(achievement.getId(), 0L);
                if (progress > 0) {
                    boolean completedAchievement = progress >= achievement.getRequiredProgress();
                    String status = completedAchievement ? com.zerog.neoessentials.localization.LanguageManager.getInstance().getMessage(targetPlayer, "achievements.list.completed") : com.zerog.neoessentials.localization.LanguageManager.getInstance().getMessage(targetPlayer, "achievements.list.incomplete");
                    String progressText = completedAchievement ? com.zerog.neoessentials.localization.LanguageManager.getInstance().getMessage(targetPlayer, "achievements.list.progress.completed") : com.zerog.neoessentials.localization.LanguageManager.getInstance().getMessage(targetPlayer, "achievements.list.progress.incomplete", progress, achievement.getRequiredProgress());
                    Component achievementInfo = Component.literal(com.zerog.neoessentials.localization.LanguageManager.getInstance().getMessage(targetPlayer, "achievements.recent_entry", status, achievement.getName(), progressText));
                    source.sendSuccess(() -> achievementInfo, false);
                    shown++;
                }
            }
            if (shown == 0) {
                Component noProgress = Component.literal(com.zerog.neoessentials.localization.LanguageManager.getInstance().getMessage(targetPlayer, "achievements.no_progress"));
                source.sendSuccess(() -> noProgress, false);
            }
            Component footerInfo = Component.literal(com.zerog.neoessentials.localization.LanguageManager.getInstance().getMessage(targetPlayer, "achievements.footer"));
            source.sendSuccess(() -> footerInfo, false);
            return 1;
        } catch (Exception e) {
            LOGGER.error("Error showing achievements for player " + targetPlayer.getName().getString(), e);
            Component error = Component.literal(com.zerog.neoessentials.localization.LanguageManager.getInstance().getMessage(targetPlayer, "achievements.error"));
            source.sendSuccess(() -> error, false);
            return 0;
        }
    }
}
