package com.zerog.neoessentials.commands;    /**
     * Registers all sign editing commands with the dispatcher.
     * 
     * @param dispatcher The command dispatcher to register with    /**
     * Checks if a player can edit a specific sign.
     * This can be extended to include plot protection, region protection, etc.
     *
     * @param player The player
     * @param signEntity The sign block entity
     * @return true if the player can edit the sign
     */
    private static boolean canEditSign(ServerPlayer player, SignBlockEntity signEntity) {
        // Check if player has admin permission
        if (CommandManager.hasPermission(player.createCommandSourceStack(), "neoessentials.editsign.admin")) {
            return true;
        }
        
        // Check if player has basic permission
        if (!CommandManager.hasPermission(player.createCommandSourceStack(), "neoessentials.editsign")) {blic static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        registerEditSignCommand(dispatcher);
        registerSignCommand(dispatcher);
    }t com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.zerog.neoessentials.utils.TextUtil;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Implements sign editing commands for NeoEssentials.
 * <p>
 * This system allows players to edit signs in the world with proper permissions.
 * Commands support:
 * <ul>
 *   <li>Editing specific lines of signs</li>
 *   <li>Replacing entire sign content</li>
 *   <li>Color code support</li>
 *   <li>Permission-based access control</li>
 * </ul>
 * </p>
 * 
 * @author ZeroG
 * @since 1.0.3
 */
public class SignEditCommands {

    /**
     * Registers all sign editing commands with the dispatcher.
     *
     * @param dispatcher The command dispatcher
     */
    public void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        registerEditSignCommand(dispatcher);
        registerSignCommand(dispatcher);
    }

    /**
     * Registers the editsign command.
     * Usage: /editsign <line> <text>
     *
     * @param dispatcher The command dispatcher
     */
    private static void registerEditSignCommand(CommandDispatcher<CommandSourceStack> dispatcher) {
        LiteralArgumentBuilder<CommandSourceStack> editSignCommand = Commands.literal("editsign")
                .requires(source -> CommandManager.hasPermission(source, "neoessentials.editsign"))
                .then(Commands.argument("line", IntegerArgumentType.integer(1, 4))
                        .then(Commands.argument("text", StringArgumentType.greedyString())
                                .executes(SignEditCommands::executeEditSignLine)))
                .executes(SignEditCommands::executeEditSignInfo);

        dispatcher.register(editSignCommand);
    }

    /**
     * Registers the sign command (alias for editsign).
     * Usage: /sign <line> <text>
     *
     * @param dispatcher The command dispatcher
     */
    private static void registerSignCommand(CommandDispatcher<CommandSourceStack> dispatcher) {
        LiteralArgumentBuilder<CommandSourceStack> signCommand = Commands.literal("sign")
                .requires(source -> CommandManager.hasPermission(source, "neoessentials.sign"))
                .then(Commands.argument("line", IntegerArgumentType.integer(1, 4))
                        .then(Commands.argument("text", StringArgumentType.greedyString())
                                .executes(SignEditCommands::executeEditSignLine)))
                .executes(SignEditCommands::executeEditSignInfo);

        dispatcher.register(signCommand);
    }

    /**
     * Executes the edit sign command for a specific line.
     *
     * @param context The command context
     * @return 1 if successful, 0 otherwise
     */
    private static int executeEditSignLine(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        ServerPlayer player = source.getPlayerOrException();
        ServerLevel level = player.serverLevel();
        
        int lineNumber = IntegerArgumentType.getInteger(context, "line");
        String newText = StringArgumentType.getString(context, "text");
        
        // Find the sign the player is looking at
        SignBlockEntity signEntity = getTargetSign(player, level);
        if (signEntity == null) {
            source.sendFailure(Component.literal("§cNo sign found. Please look at a sign to edit it."));
            return 0;
        }
        
        // Check if player has permission to edit this sign
        if (!canEditSign(player, signEntity)) {
            source.sendFailure(Component.literal("§cYou don't have permission to edit this sign."));
            return 0;
        }
        
        // Edit the specified line
        try {
            editSignLine(signEntity, lineNumber - 1, newText);
            source.sendSuccess(() -> Component.literal("§aSign line " + lineNumber + " has been updated."), false);
            return 1;
        } catch (Exception e) {
            source.sendFailure(Component.literal("§cFailed to edit sign: " + e.getMessage()));
            return 0;
        }
    }

    /**
     * Executes the edit sign info command (shows usage).
     *
     * @param context The command context
     * @return 1 if successful, 0 otherwise
     */
    private static int executeEditSignInfo(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        
        source.sendSuccess(() -> Component.literal("§6=== Sign Editing Commands ==="), false);
        source.sendSuccess(() -> Component.literal("§e/editsign <line> <text> §7- Edit a specific line of a sign"), false);
        source.sendSuccess(() -> Component.literal("§e/sign <line> <text> §7- Alias for editsign"), false);
        source.sendSuccess(() -> Component.literal("§7Line numbers are 1-4 (top to bottom)"), false);
        source.sendSuccess(() -> Component.literal("§7Look at a sign and use the command to edit it"), false);
        source.sendSuccess(() -> Component.literal("§7Color codes are supported using § symbol"), false);
        source.sendSuccess(() -> Component.literal("§7Examples:"), false);
        source.sendSuccess(() -> Component.literal("§7  /editsign 1 §bWelcome to our server!"), false);
        source.sendSuccess(() -> Component.literal("§7  /sign 2 §4[DANGER]"), false);
        
        return 1;
    }

    /**
     * Gets the sign block entity the player is looking at.
     *
     * @param player The player
     * @param level The server level
     * @return The sign block entity, or null if not found
     */
    private SignBlockEntity getTargetSign(ServerPlayer player, ServerLevel level) {
        // Calculate look position (max distance 10 blocks for sign editing)
        Vec3 eyePosition = player.getEyePosition();
        Vec3 viewVector = player.getViewVector(1.0F);
        Vec3 targetVector = eyePosition.add(viewVector.x * 10, viewVector.y * 10, viewVector.z * 10);
        
        // Perform ray trace
        ClipContext clipContext = new ClipContext(
            eyePosition, 
            targetVector, 
            ClipContext.Block.OUTLINE, 
            ClipContext.Fluid.NONE, 
            player
        );
        
        BlockHitResult hitResult = level.clip(clipContext);
        
        if (hitResult.getType() != HitResult.Type.BLOCK) {
            return null;
        }
        
        BlockPos blockPos = hitResult.getBlockPos();
        BlockEntity blockEntity = level.getBlockEntity(blockPos);
        
        if (blockEntity instanceof SignBlockEntity signEntity) {
            return signEntity;
        }
        
        return null;
    }

    /**
     * Checks if a player can edit a specific sign.
     * This can be extended to include plot protection, region protection, etc.
     *
     * @param player The player
     * @param signEntity The sign block entity
     * @return true if the player can edit the sign
     */
    private boolean canEditSign(ServerPlayer player, SignBlockEntity signEntity) {
        // Check if player has admin permission
        if (CommandManager.hasPermission(player, "neoessentials.editsign.admin")) {
            return true;
        }
        
        // Check if player has basic edit permission
        if (!CommandManager.hasPermission(player, "neoessentials.editsign")) {
            return false;
        }
        
        // Additional checks can be added here:
        // - Check if player owns the land/plot
        // - Check if player is in a protected region
        // - Check if sign is in a protected area
        
        // For now, allow editing if player has the basic permission
        return true;
    }

    /**
     * Edits a specific line of a sign.
     *
     * @param signEntity The sign block entity
     * @param lineIndex The line index (0-3)
     * @param newText The new text for the line
     */
    private void editSignLine(SignBlockEntity signEntity, int lineIndex, String newText) {
        if (lineIndex < 0 || lineIndex > 3) {
            throw new IllegalArgumentException("Line index must be between 0 and 3");
        }
        
        // Process color codes in the text
        String processedText = TextUtil.colorize(newText);
        
        // Create component from processed text
        Component textComponent = Component.literal(processedText);
        
        // Get the current sign text
        var signText = signEntity.getFrontText();
        
        // Update the specific line
        signText = signText.setMessage(lineIndex, textComponent);
        
        // Set the updated text back to the sign
        signEntity.setText(signText, true);
        
        // Mark the block for update
        signEntity.setChanged();
        
        // Update clients
        BlockPos pos = signEntity.getBlockPos();
        ServerLevel level = (ServerLevel) signEntity.getLevel();
        if (level != null) {
            BlockState state = level.getBlockState(pos);
            level.sendBlockUpdated(pos, state, state, 3);
        }
    }
}
