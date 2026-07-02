package com.zerog.neoessentials.util.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.zerog.neoessentials.config.ConfigManager;
import com.zerog.neoessentials.util.PermissionValidator;
import com.zerog.neoessentials.util.MessageUtil;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SignCommand {
    private static final Logger LOGGER = LoggerFactory.getLogger(SignCommand.class);

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        if (!ConfigManager.getInstance().isCommandEnabled("sign")) return;
        
        dispatcher.register(Commands.literal("sign")
            .requires(source -> {
                PermissionValidator.PermissionResult result = PermissionValidator.validatePermission(source, "neoessentials.sign");
                return result.hasPermission();
            })
            .executes(SignCommand::editLookingAtSign)
            .then(Commands.argument("line", IntegerArgumentType.integer(1, 4))
                .then(Commands.argument("text", StringArgumentType.greedyString())
                    .executes(SignCommand::editLookingAtSignWithText)
                )
            )
            .then(Commands.literal("clear")
                .executes(SignCommand::clearLookingAtSign)
                .then(Commands.argument("line", IntegerArgumentType.integer(1, 4))
                    .executes(SignCommand::clearLookingAtSignLine)
                )
            )
        );
    }

    private static int editLookingAtSign(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        SignBlockEntity signBlockEntity = getTargetedSign(player);
        
        if (signBlockEntity == null) {
            context.getSource().sendFailure(MessageUtil.error("commands.neoessentials.sign.not_looking_at_sign"));
            return 0;
        }

        // Show current sign contents and usage
        context.getSource().sendSuccess(() -> MessageUtil.info("commands.neoessentials.sign.current_contents"), false);
        
        for (int i = 0; i < 4; i++) {
            Component lineText = signBlockEntity.getFrontText().getMessage(i, false);
            String textContent = lineText.getString();
            if (textContent.isEmpty()) {
                textContent = "§7(empty)";
            }
            
            MutableComponent lineComponent = Component.literal("§6Line " + (i + 1) + ": §f" + textContent);
            context.getSource().sendSuccess(() -> lineComponent, false);
        }
        
        context.getSource().sendSuccess(() -> MessageUtil.info("commands.neoessentials.sign.usage"), false);
        return 1;
    }

    private static int editLookingAtSignWithText(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        int line = IntegerArgumentType.getInteger(context, "line") - 1; // Convert to 0-based
        String originalText = StringArgumentType.getString(context, "text");
        
        // Process color codes if player has permission
        PermissionValidator.PermissionResult colorResult = PermissionValidator.validatePermission(context.getSource(), "neoessentials.sign.colors");
        String processedText;
        if (colorResult.hasPermission()) {
            processedText = originalText.replace("&", "§");
        } else {
            // Remove any color codes for regular players
            processedText = originalText.replaceAll("§[0-9a-fk-or]", "").replaceAll("&[0-9a-fk-or]", "");
        }
        
        // Limit text length (signs have limited space)
        String finalText;
        if (processedText.length() > 15) {
            finalText = processedText.substring(0, 15);
            context.getSource().sendSuccess(() -> MessageUtil.warning("commands.neoessentials.sign.text_truncated"), false);
        } else {
            finalText = processedText;
        }

        SignBlockEntity signBlockEntity = getTargetedSign(player);
        
        if (signBlockEntity == null) {
            context.getSource().sendFailure(MessageUtil.error("commands.neoessentials.sign.not_looking_at_sign"));
            return 0;
        }

        BlockPos pos = signBlockEntity.getBlockPos();
        
        // Update the sign text
        updateSignLine(signBlockEntity, line, finalText, com.zerog.neoessentials.util.LevelCompat.of(player));
        
        LOGGER.info("Player {} edited sign at {} line {} to: {}", 
            player.getName().getString(), pos, line + 1, finalText);
        
        context.getSource().sendSuccess(() -> MessageUtil.success("commands.neoessentials.sign.updated", 
            line + 1, finalText.isEmpty() ? "§7(empty)" : finalText), false);
        return 1;
    }

    private static int clearLookingAtSign(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        SignBlockEntity signBlockEntity = getTargetedSign(player);
        
        if (signBlockEntity == null) {
            context.getSource().sendFailure(MessageUtil.error("commands.neoessentials.sign.not_looking_at_sign"));
            return 0;
        }

        BlockPos pos = signBlockEntity.getBlockPos();
        
        // Clear all lines
        for (int i = 0; i < 4; i++) {
            updateSignLine(signBlockEntity, i, "", com.zerog.neoessentials.util.LevelCompat.of(player));
        }
        
        LOGGER.info("Player {} cleared all lines on sign at {}", player.getName().getString(), pos);
        
        context.getSource().sendSuccess(() -> MessageUtil.success("commands.neoessentials.sign.cleared_all"), false);
        return 1;
    }

    private static int clearLookingAtSignLine(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        int line = IntegerArgumentType.getInteger(context, "line") - 1; // Convert to 0-based
        
        SignBlockEntity signBlockEntity = getTargetedSign(player);
        
        if (signBlockEntity == null) {
            context.getSource().sendFailure(MessageUtil.error("commands.neoessentials.sign.not_looking_at_sign"));
            return 0;
        }

        BlockPos pos = signBlockEntity.getBlockPos();
        
        // Clear the specific line
        updateSignLine(signBlockEntity, line, "", com.zerog.neoessentials.util.LevelCompat.of(player));
        
        LOGGER.info("Player {} cleared line {} on sign at {}", player.getName().getString(), line + 1, pos);
        
        context.getSource().sendSuccess(() -> MessageUtil.success("commands.neoessentials.sign.cleared_line", line + 1), false);
        return 1;
    }

    private static SignBlockEntity getTargetedSign(ServerPlayer player) {
        // Get what the player is looking at
        HitResult hitResult = player.pick(5.0D, 0.0F, false);
        
        if (hitResult.getType() != HitResult.Type.BLOCK) {
            return null;
        }
        
        BlockHitResult blockHitResult = (BlockHitResult) hitResult;
        BlockPos pos = blockHitResult.getBlockPos();
        ServerLevel level = com.zerog.neoessentials.util.LevelCompat.of(player);
        
        BlockEntity blockEntity = level.getBlockEntity(pos);
        
        if (blockEntity instanceof SignBlockEntity) {
            return (SignBlockEntity) blockEntity;
        }
        
        return null;
    }

    private static void updateSignLine(SignBlockEntity signBlockEntity, int line, String text, ServerLevel level) {
    // Create a new component with the text (with color codes and hex support)
    Component textComponent = com.zerog.neoessentials.util.MessageUtil.coloredText(text);
        
        // Get current front text and update the specific line
        var currentText = signBlockEntity.getFrontText();
        var newText = currentText.setMessage(line, textComponent);
        
        // Update the sign
        signBlockEntity.updateText(signBlockEntity_ -> newText, true);
        
        // Mark the block entity as changed and sync to clients
        signBlockEntity.setChanged();
        BlockState blockState = level.getBlockState(signBlockEntity.getBlockPos());
        level.sendBlockUpdated(signBlockEntity.getBlockPos(), blockState, blockState, 3);
    }
}