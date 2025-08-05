package com.zerog.neoessentials.commands.test;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.zerog.neoessentials.test.GuiTest;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Test command for GUI functionality
 * 
 * @author ZeroG
 * @since 2.0.0
 */
public class GuiTestCommand {
    private static final Logger LOGGER = LoggerFactory.getLogger(GuiTestCommand.class);
    
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("testgui")
            .requires(source -> source.hasPermission(3)) // Admin only
            .then(Commands.argument("type", StringArgumentType.string())
                .suggests((context, builder) -> {
                    builder.suggest("shop");
                    builder.suggest("stats");
                    builder.suggest("kits");
                    builder.suggest("all");
                    return builder.buildFuture();
                })
                .executes(GuiTestCommand::executeTestGui))
        );
    }
    
    private static int executeTestGui(CommandContext<CommandSourceStack> context) {
        try {
            ServerPlayer player = context.getSource().getPlayerOrException();
            String type = StringArgumentType.getString(context, "type").toLowerCase();
            
            switch (type) {
                case "shop" -> {
                    GuiTest.testShopGui(player);
                    player.sendSystemMessage(Component.literal("§aShop GUI test initiated"));
                }
                case "stats" -> {
                    GuiTest.testStatsGui(player);
                    player.sendSystemMessage(Component.literal("§aStats GUI test initiated"));
                }
                case "kits" -> {
                    GuiTest.testKitGui(player);
                    player.sendSystemMessage(Component.literal("§aKit GUI test initiated"));
                }
                case "all" -> {
                    GuiTest.testAllGuis(player);
                    player.sendSystemMessage(Component.literal("§aAll GUI tests initiated"));
                }
                default -> {
                    player.sendSystemMessage(Component.literal("§cUnknown test type. Use: shop, stats, kits, or all"));
                    return 0;
                }
            }
            
            return 1;
            
        } catch (Exception e) {
            LOGGER.error("Error executing GUI test command", e);
            context.getSource().sendFailure(Component.literal("§cFailed to execute GUI test: " + e.getMessage()));
            return 0;
        }
    }
}
