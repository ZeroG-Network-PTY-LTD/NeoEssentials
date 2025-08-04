package com.zerog.neoessentials.commands.player;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.zerog.neoessentials.player.AchievementSystem;
import com.zerog.neoessentials.player.PlayerData;
import com.zerog.neoessentials.player.PlayerDataManager;
import com.zerog.neoessentials.util.MessageUtils;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Command for viewing and managing achievements
 */
public class AchievementsCommand {
    private static final Logger LOGGER = LoggerFactory.getLogger(AchievementsCommand.class);
    
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("achievements")
            .executes(AchievementsCommand::showOwnAchievements)
            .then(Commands.argument("player", EntityArgument.player())
                .requires(source -> source.hasPermission(1))
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
            
            Component header = MessageUtils.format("&6&l=== Achievement Categories ===");
            context.getSource().sendSuccess(() -> header, false);
            
            for (AchievementSystem.AchievementCategory category : AchievementSystem.AchievementCategory.values()) {
                List<AchievementSystem.Achievement> categoryAchievements = achievementSystem.getAchievementsByCategory(category);
                
                Component categoryInfo = MessageUtils.format(
                    "&e" + category.getDisplayName() + " &7- &a" + categoryAchievements.size() + " achievements"
                );
                context.getSource().sendSuccess(() -> categoryInfo, false);
            }
            
            return 1;
        } catch (Exception e) {
            LOGGER.error("Error showing achievement categories", e);
            Component error = MessageUtils.format("&cError retrieving achievement categories.");
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
            
            Component header = MessageUtils.format("&6&l=== All Achievements ===");
            context.getSource().sendSuccess(() -> header, false);
            
            for (AchievementSystem.Achievement achievement : allAchievementsCollection) {
                long progress = achievements.getOrDefault(achievement.getId(), 0L);
                boolean completed = progress >= achievement.getRequiredProgress();
                
                String status = completed ? "&a✓" : "&7○";
                String progressText = completed ? "Completed" : progress + "/" + achievement.getRequiredProgress();
                
                Component achievementInfo = MessageUtils.format(
                    status + " &e" + achievement.getName() + " &7- &f" + achievement.getDescription() +
                    " &7(" + progressText + ")"
                );
                context.getSource().sendSuccess(() -> achievementInfo, false);
            }
            
            return 1;
        } catch (Exception e) {
            LOGGER.error("Error showing all achievements for player " + player.getName().getString(), e);
            Component error = MessageUtils.format("&cError retrieving achievements.");
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
            
            Component header = MessageUtils.format("&6&l=== Achievement Statistics ===");
            context.getSource().sendSuccess(() -> header, false);
            
            Component completedInfo = MessageUtils.format("&eCompleted: &a" + completed + "&7/&a" + allAchievementsCollection.size());
            context.getSource().sendSuccess(() -> completedInfo, false);
            
            Component progressInfo = MessageUtils.format("&eIn Progress: &6" + inProgress);
            context.getSource().sendSuccess(() -> progressInfo, false);
            
            Component scoreInfo = MessageUtils.format("&eTotal Score: &b" + totalScore + " points");
            context.getSource().sendSuccess(() -> scoreInfo, false);
            
            double completionRate = (double) completed / allAchievementsCollection.size() * 100;
            Component rateInfo = MessageUtils.format("&eCompletion Rate: &d" + String.format("%.1f", completionRate) + "%");
            context.getSource().sendSuccess(() -> rateInfo, false);
            
            return 1;
        } catch (Exception e) {
            LOGGER.error("Error showing achievement stats for player " + player.getName().getString(), e);
            Component error = MessageUtils.format("&cError retrieving achievement statistics.");
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
            
            String targetName = isOwnAchievements ? "Your" : targetPlayer.getName().getString() + "'s";
            
            Component header = MessageUtils.format("&6&l=== " + targetName + " Achievements ===");
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
            
            Component summary = MessageUtils.format("&eCompleted: &a" + completed + "&7/&a" + allAchievementsCollection.size() + 
                " &7| Score: &b" + totalScore + " points");
            source.sendSuccess(() -> summary, false);
            
            // Show recently completed achievements (last 5)
            Component recentHeader = MessageUtils.format("&6Recent Progress:");
            source.sendSuccess(() -> recentHeader, false);
            
            int shown = 0;
            for (AchievementSystem.Achievement achievement : allAchievementsCollection) {
                if (shown >= 5) break;
                
                long progress = achievements.getOrDefault(achievement.getId(), 0L);
                if (progress > 0) {
                    boolean completedAchievement = progress >= achievement.getRequiredProgress();
                    String status = completedAchievement ? "&a✓" : "&6⏳";
                    String progressText = completedAchievement ? "Completed" : progress + "/" + achievement.getRequiredProgress();
                    
                    Component achievementInfo = MessageUtils.format(
                        status + " &e" + achievement.getName() + " &7(" + progressText + ")"
                    );
                    source.sendSuccess(() -> achievementInfo, false);
                    shown++;
                }
            }
            
            if (shown == 0) {
                Component noProgress = MessageUtils.format("&7No achievements in progress yet.");
                source.sendSuccess(() -> noProgress, false);
            }
            
            Component footerInfo = MessageUtils.format("&7Use &e/achievements list &7or &e/achievements stats &7for more details.");
            source.sendSuccess(() -> footerInfo, false);
            
            return 1;
        } catch (Exception e) {
            LOGGER.error("Error showing achievements for player " + targetPlayer.getName().getString(), e);
            Component error = MessageUtils.format("&cError retrieving achievement data.");
            source.sendSuccess(() -> error, false);
            return 0;
        }
    }
}
