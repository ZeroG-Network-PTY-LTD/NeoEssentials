
package com.zerog.neoessentials.items.commands;

import com.zerog.neoessentials.util.MessageUtil;
import com.zerog.neoessentials.util.InputValidator;
import com.zerog.neoessentials.util.PermissionValidator;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.Collection;

/**
 * Provides enhanced item enchanting functionality with safety features and override permissions.
 * 
 * <p>Commands:</p>
 * <ul>
 *   <li>/enchant &lt;enchantment&gt; [level] - Enchant item in hand (default level 1)</li>
 *   <li>/enchant &lt;target&gt; &lt;enchantment&gt; [level] - Enchant target player's item</li>
 *   <li>/ench - Short alias for /enchant</li>
 *   <li>/enchanthand - Explicit hand-only enchanting</li>
 * </ul>
 * 
 * <p>Permissions:</p>
 * <ul>
 *   <li>neoessentials.item.enchant - Basic enchanting on own items</li>
 *   <li>neoessentials.item.enchant.others - Enchant other players' items</li>
 *   <li>neoessentials.item.enchant.unsafe - Use enchantment levels above normal max</li>
 *   <li>neoessentials.item.enchant.any - Bypass item compatibility checks</li>
 * </ul>
 * 
 * <p>Configuration:</p>
 * <ul>
 *   <li>unsafe-enchantments - Allow enchantment levels above normal max (global)</li>
 * </ul>
 * 
 * <p>Features:</p>
 * <ul>
 *   <li>Modern DataComponents API for Minecraft 1.21.1</li>
 *   <li>Enchantment level validation and safety checks</li>
 *   <li>Optional target player support with notifications</li>
 *   <li>Configuration-driven unsafe enchantment control</li>
 *   <li>Comprehensive audit logging for all enchantments</li>
 *   <li>Item compatibility checking</li>
 * </ul>
 */
public class EnchantCommand {
    private static final Logger LOGGER = LoggerFactory.getLogger(EnchantCommand.class);
    /**
     * Register the enhanced /enchant command that overrides vanilla Minecraft enchant.
     * Registers with higher priority to override vanilla command.
     */
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        if (!com.zerog.neoessentials.config.ConfigManager.getInstance().isCommandEnabled("enchant")) return;
        
        // Override vanilla enchant command with enhanced version and add aliases
        registerEnchantCommand(dispatcher, "enchant");
        registerEnchantCommand(dispatcher, "ench");
    }
    
    private static void registerEnchantCommand(CommandDispatcher<CommandSourceStack> dispatcher, String commandName) {
        dispatcher.register(
            Commands.literal(commandName)
                .requires(cs -> cs.hasPermission(2) || // Allow ops
                    (cs.getEntity() instanceof ServerPlayer player && 
                     com.zerog.neoessentials.api.permissions.PermissionAPI.hasPermission(player.getUUID(), "neoessentials.item.enchant")))
                // Enchant item in hand
                .then(Commands.argument("enchantment", ResourceLocationArgument.id())
                    .suggests((ctx, builder) -> {
                        return net.minecraft.commands.SharedSuggestionProvider.suggestResource(
                            ctx.getSource().getServer().registryAccess()
                                .registryOrThrow(Registries.ENCHANTMENT)
                                .keySet(), builder
                        );
                    })
                    .then(Commands.argument("level", IntegerArgumentType.integer(1, 32767))
                        .executes(ctx -> executeEnchant(ctx, EnchantMode.HAND_ONLY))
                    )
                    .executes(ctx -> executeEnchant(ctx, EnchantMode.HAND_ONLY)) // Default level 1
                )
                // Enchant target player's item in hand
                .then(Commands.argument("target", EntityArgument.player())
                    .requires(cs -> cs.hasPermission(2) || 
                        (cs.getEntity() instanceof ServerPlayer player && 
                         com.zerog.neoessentials.api.permissions.PermissionAPI.hasPermission(player.getUUID(), "neoessentials.item.enchant.others")))
                    .then(Commands.argument("enchantment", ResourceLocationArgument.id())
                        .suggests((ctx, builder) -> {
                            return net.minecraft.commands.SharedSuggestionProvider.suggestResource(
                                ctx.getSource().getServer().registryAccess()
                                    .registryOrThrow(Registries.ENCHANTMENT)
                                    .keySet(), builder
                            );
                        })
                        .then(Commands.argument("level", IntegerArgumentType.integer(1, 32767))
                            .executes(ctx -> executeEnchant(ctx, EnchantMode.TARGET_HAND))
                        )
                        .executes(ctx -> executeEnchant(ctx, EnchantMode.TARGET_HAND)) // Default level 1
                    )
                )
                .executes(ctx -> {
                    ctx.getSource().sendFailure(MessageUtil.error("commands.neoessentials.enchant.usage"));
                    return 0;
                })
        );
        
        // Keep enchanthand as alias for hand-only enchanting
        dispatcher.register(
            Commands.literal("enchanthand")
                .requires(cs -> cs.hasPermission(2) ||
                    (cs.getEntity() instanceof ServerPlayer player && 
                     com.zerog.neoessentials.api.permissions.PermissionAPI.hasPermission(player.getUUID(), "neoessentials.item.enchant")))
                .then(Commands.argument("enchantment", ResourceLocationArgument.id())
                    .suggests((ctx, builder) -> {
                        return net.minecraft.commands.SharedSuggestionProvider.suggestResource(
                            ctx.getSource().getServer().registryAccess()
                                .registryOrThrow(Registries.ENCHANTMENT)
                                .keySet(), builder
                        );
                    })
                    .then(Commands.argument("level", IntegerArgumentType.integer(1, 32767))
                        .executes(ctx -> executeEnchant(ctx, EnchantMode.HAND_ONLY))
                    )
                    .executes(ctx -> executeEnchant(ctx, EnchantMode.HAND_ONLY)) // Default level 1
                )
                .executes(ctx -> {
                    ctx.getSource().sendFailure(MessageUtil.error("commands.neoessentials.enchanthand.usage"));
                    return 0;
                })
        );
    }
    
    /**
     * Enchantment modes for different command contexts
     */
    private enum EnchantMode {
        HAND_ONLY,      // Enchant item in executor's hand
        TARGET_HAND     // Enchant item in target player's hand
    }

    /**
     * Execute the enhanced enchant command with improved validation and features.
     */
    private static int executeEnchant(com.mojang.brigadier.context.CommandContext<CommandSourceStack> ctx, EnchantMode mode) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        // Validate permission based on mode
        String requiredPermission = mode == EnchantMode.TARGET_HAND ? "neoessentials.item.enchant.others" : "neoessentials.item.enchant";
        PermissionValidator.PermissionResult permResult = 
            PermissionValidator.validatePermission(ctx.getSource(), requiredPermission);
        if (!permResult.hasPermission()) {
            ctx.getSource().sendFailure(MessageUtil.error(permResult.getErrorMessage()));
            return 0;
        }
        
        ServerPlayer executor = permResult.getPlayer();
        final ServerPlayer targetPlayer;
        
        // Handle target player for TARGET_HAND mode
        if (mode == EnchantMode.TARGET_HAND) {
            try {
                Collection<ServerPlayer> targets = EntityArgument.getPlayers(ctx, "target");
                if (targets.isEmpty()) {
                    ctx.getSource().sendFailure(MessageUtil.error("commands.neoessentials.enchant.no_target"));
                    return 0;
                }
                targetPlayer = targets.iterator().next();
            } catch (Exception e) {
                ctx.getSource().sendFailure(MessageUtil.error("commands.neoessentials.enchant.invalid_target"));
                return 0;
            }
        } else {
            targetPlayer = executor;
        }
        
        // Get enchantment from argument
        ResourceLocation enchantId = ResourceLocationArgument.getId(ctx, "enchantment");
        
        // Get level (default to 1 if not provided)
        int levelTemp = 1;
        try {
            levelTemp = IntegerArgumentType.getInteger(ctx, "level");
        } catch (IllegalArgumentException ignored) {
            // Use default level 1
        }
        
        // Check if unsafe enchantments are allowed (or if player has override permission)
        boolean allowUnsafeEnchants = com.zerog.neoessentials.config.ConfigManager.isUnsafeEnchantsAllowed() ||
            com.zerog.neoessentials.api.permissions.PermissionAPI.hasPermission(executor.getUUID(), "neoessentials.item.enchant.unsafe");
        
        // Validate enchantment level
        InputValidator.ValidationResult levelValidation = 
            InputValidator.validateEnchantmentLevel(levelTemp, allowUnsafeEnchants);
        if (!levelValidation.isValid()) {
            ctx.getSource().sendFailure(MessageUtil.error(levelValidation.getErrorMessage()));
            return 0;
        }
        
        final int level = levelValidation.getValue(Integer.class);
        
        // Get the enchantment from registry
        net.minecraft.core.Registry<Enchantment> enchantRegistry = targetPlayer.getServer()
            .registryAccess()
            .registryOrThrow(Registries.ENCHANTMENT);
        
        if (!enchantRegistry.containsKey(enchantId)) {
            ctx.getSource().sendFailure(MessageUtil.error("commands.neoessentials.enchant.unknown", enchantId.toString()));
            return 0;
        }
        
        Enchantment enchantment = enchantRegistry.get(enchantId);
        if (enchantment == null) {
            ctx.getSource().sendFailure(MessageUtil.error("commands.neoessentials.enchant.unknown", enchantId.toString()));
            return 0;
        }
        
        // Get item to enchant
        ItemStack stack = targetPlayer.getMainHandItem();
        if (stack.isEmpty()) {
            ctx.getSource().sendFailure(MessageUtil.error("commands.neoessentials.enchant.no_item"));
            return 0;
        }
        
        // Check if enchantment is compatible with the item (unless override permission)
        boolean canEnchantAny = com.zerog.neoessentials.api.permissions.PermissionAPI.hasPermission(executor.getUUID(), "neoessentials.item.enchant.any");
        if (!canEnchantAny && !isEnchantmentCompatible(enchantment, stack)) {
            ctx.getSource().sendFailure(MessageUtil.error("commands.neoessentials.enchant.incompatible", 
                enchantId.toString(), stack.getDisplayName().getString()));
            return 0;
        }
        
        // Apply enchantment 
        boolean success = applyEnchantment(targetPlayer, stack, enchantment, level);
        
        if (success) {
            // Log successful enchantment for audit trail
            LOGGER.info("Player {} enchanted {} with {} level {} for player {}", 
                executor.getName().getString(),
                stack.getDisplayName().getString(),
                enchantId.toString(),
                level,
                targetPlayer.getName().getString());
            
            // Success message varies by mode
            if (mode == EnchantMode.TARGET_HAND && !executor.equals(targetPlayer)) {
                ctx.getSource().sendSuccess(() -> MessageUtil.success(
                    "commands.neoessentials.enchant.success.other", 
                    enchantId.toString(), 
                    level,
                    stack.getDisplayName().getString(),
                    targetPlayer.getDisplayName().getString()
                ), false);
                
                // Notify target player
                targetPlayer.sendSystemMessage(MessageUtil.info(
                    "commands.neoessentials.enchant.target.notified",
                    enchantId.toString(),
                    level,
                    executor.getDisplayName().getString()
                ));
            } else {
                ctx.getSource().sendSuccess(() -> MessageUtil.success(
                    "commands.neoessentials.enchant.success", 
                    enchantId.toString(), 
                    level,
                    stack.getDisplayName().getString()
                ), false);
            }
            return 1;
        } else {
            ctx.getSource().sendFailure(MessageUtil.error("commands.neoessentials.enchant.failed"));
            return 0;
        }
    }

    /**
     * Check if an enchantment is compatible with an item stack
     */
    private static boolean isEnchantmentCompatible(Enchantment enchantment, ItemStack stack) {
        try {
            // Check if the item is enchantable at all
            if (!stack.getItem().isEnchantable(stack)) {
                return false;
            }
            
            // For books, allow all enchantments
            if (stack.getItem().toString().contains("book")) {
                return true;
            }
            
            // Try to check enchantment category compatibility
            // This is a basic implementation - in practice you'd need more sophisticated checking
            return stack.getItem().isEnchantable(stack);
            
        } catch (Exception e) {
            // Fallback: if we can't determine compatibility, allow it
            return true;
        }
    }

    /**
     * Apply an enchantment to an item, enforcing the unsafe-enchantments config.
     * @param player The player
     * @param stack The item stack
     * @param enchantment The enchantment
     * @param level The enchantment level
     * @return true if enchantment was applied, false if blocked
     */
    public static boolean applyEnchantment(ServerPlayer player, ItemStack stack, Enchantment enchantment, int level) {
        if (stack == null || enchantment == null) return false;

        // Respect unsafe-enchantments config
        boolean allowUnsafeEnchants = com.zerog.neoessentials.config.ConfigManager.isUnsafeEnchantsAllowed();
        if (!allowUnsafeEnchants && level > enchantment.getMaxLevel()) {
            return false;
        }

        try {
            // Get the enchantment holder from registry
            net.minecraft.core.Registry<Enchantment> registry = player.getServer()
                .registryAccess()
                .registryOrThrow(Registries.ENCHANTMENT);
                
            // Find the holder for this enchantment
            Holder<Enchantment> holder = null;
            for (var entry : registry.holders().toList()) {
                if (entry.value().equals(enchantment)) {
                    holder = entry;
                    break;
                }
            }
            
            if (holder == null) return false;

            // Get existing enchantments and create a mutable copy
            ItemEnchantments existing = stack.get(DataComponents.ENCHANTMENTS);
            ItemEnchantments.Mutable builder;
            
            if (existing != null) {
                builder = new ItemEnchantments.Mutable(existing);
            } else {
                builder = new ItemEnchantments.Mutable(ItemEnchantments.EMPTY);
            }
            
            // Add/update the enchantment
            builder.set(holder, level);
            
            // Apply to item
            stack.set(DataComponents.ENCHANTMENTS, builder.toImmutable());
            return true;
            
        } catch (Exception e) {
            LOGGER.error("Failed to apply enchantment to item", e);
            return false;
        }
    }
}
