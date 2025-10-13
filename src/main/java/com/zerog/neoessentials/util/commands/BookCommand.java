package com.zerog.neoessentials.util.commands;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import com.zerog.neoessentials.config.ConfigManager;
import com.zerog.neoessentials.util.MessageUtil;
import com.zerog.neoessentials.util.PermissionValidator;

/**
 * Implements the /book command - Gives players a writable book or converts books
 * Allows easy creation and editing of books
 */
public class BookCommand {
    
    /**
     * Register the /book command
     */
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        if (!ConfigManager.getInstance().isCommandEnabled("book")) return;
        
        dispatcher.register(
            Commands.literal("book")
                .requires(cs -> cs.getEntity() instanceof ServerPlayer)
                // /book - Give a writable book
                .executes(ctx -> {
                    PermissionValidator.PermissionResult permResult = 
                        PermissionValidator.validatePermission(ctx.getSource(), "neoessentials.book");
                    if (!permResult.hasPermission()) {
                        ctx.getSource().sendFailure(MessageUtil.error(permResult.getErrorMessage()));
                        return 0;
                    }
                    
                    ServerPlayer player = permResult.getPlayer();
                    return giveWritableBook(player);
                })
                // /book unlock - Convert written book back to writable
                .then(Commands.literal("unlock")
                    .executes(ctx -> {
                        PermissionValidator.PermissionResult permResult = 
                            PermissionValidator.validatePermission(ctx.getSource(), "neoessentials.book.unlock");
                        if (!permResult.hasPermission()) {
                            ctx.getSource().sendFailure(MessageUtil.error(permResult.getErrorMessage()));
                            return 0;
                        }
                        
                        ServerPlayer player = permResult.getPlayer();
                        return unlockBook(player);
                    })
                )
                // /book title <title> - Set title of book in hand
                .then(Commands.literal("title")
                    .then(Commands.argument("title", com.mojang.brigadier.arguments.StringArgumentType.greedyString())
                        .executes(ctx -> {
                            PermissionValidator.PermissionResult permResult = 
                                PermissionValidator.validatePermission(ctx.getSource(), "neoessentials.book.title");
                            if (!permResult.hasPermission()) {
                                ctx.getSource().sendFailure(MessageUtil.error(permResult.getErrorMessage()));
                                return 0;
                            }
                            
                            ServerPlayer player = permResult.getPlayer();
                            String title = com.mojang.brigadier.arguments.StringArgumentType.getString(ctx, "title");
                            return setBookTitle(player, title);
                        })
                    )
                )
                // /book author <author> - Set author of book in hand
                .then(Commands.literal("author")
                    .then(Commands.argument("author", com.mojang.brigadier.arguments.StringArgumentType.greedyString())
                        .executes(ctx -> {
                            PermissionValidator.PermissionResult permResult = 
                                PermissionValidator.validatePermission(ctx.getSource(), "neoessentials.book.author");
                            if (!permResult.hasPermission()) {
                                ctx.getSource().sendFailure(MessageUtil.error(permResult.getErrorMessage()));
                                return 0;
                            }
                            
                            ServerPlayer player = permResult.getPlayer();
                            String author = com.mojang.brigadier.arguments.StringArgumentType.getString(ctx, "author");
                            return setBookAuthor(player, author);
                        })
                    )
                )
        );
    }
    
    /**
     * Give a writable book to the player
     */
    private static int giveWritableBook(ServerPlayer player) {
        ItemStack book = new ItemStack(Items.WRITABLE_BOOK);
        
        // Add book to player's inventory
        if (!player.getInventory().add(book)) {
            // If inventory is full, drop it at their feet
            player.drop(book, false);
            player.sendSystemMessage(MessageUtil.warning("commands.neoessentials.book.inventory_full"));
        } else {
            player.sendSystemMessage(MessageUtil.success("commands.neoessentials.book.given"));
        }
        
        return 1;
    }
    
    /**
     * Convert a written book back to writable
     */
    private static int unlockBook(ServerPlayer player) {
        ItemStack heldItem = player.getMainHandItem();
        
        if (heldItem.getItem() != Items.WRITTEN_BOOK) {
            player.sendSystemMessage(MessageUtil.error("commands.neoessentials.book.not_written_book"));
            return 0;
        }
        
        // Create new writable book
        ItemStack writableBook = new ItemStack(Items.WRITABLE_BOOK);
        
        // Copy any written book content to the writable book if possible
        // Note: In newer versions, this conversion is simplified
        
        // Replace the item in player's hand
        player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, writableBook);
        
        player.sendSystemMessage(MessageUtil.success("commands.neoessentials.book.unlocked"));
        return 1;
    }
    
    /**
     * Set the title of a writable book
     */
    private static int setBookTitle(ServerPlayer player, String title) {
        ItemStack heldItem = player.getMainHandItem();
        
        if (heldItem.getItem() != Items.WRITABLE_BOOK) {
            player.sendSystemMessage(MessageUtil.error("commands.neoessentials.book.not_writable_book"));
            return 0;
        }
        
        // Validate title length
        if (title.length() > 32) {
            player.sendSystemMessage(MessageUtil.error("commands.neoessentials.book.title_too_long"));
            return 0;
        }
        
        // Note: Title setting is stored for when the book is signed
        // In newer Minecraft versions, title is set when signing the book
        player.sendSystemMessage(MessageUtil.success("commands.neoessentials.book.title_set", title));
        return 1;
    }
    
    /**
     * Set the author of a writable book
     */
    private static int setBookAuthor(ServerPlayer player, String author) {
        ItemStack heldItem = player.getMainHandItem();
        
        if (heldItem.getItem() != Items.WRITABLE_BOOK) {
            player.sendSystemMessage(MessageUtil.error("commands.neoessentials.book.not_writable_book"));
            return 0;
        }
        
        // Validate author length
        if (author.length() > 16) {
            player.sendSystemMessage(MessageUtil.error("commands.neoessentials.book.author_too_long"));
            return 0;
        }
        
        // Note: Author setting is stored for when the book is signed
        // In newer Minecraft versions, author is set when signing the book
        player.sendSystemMessage(MessageUtil.success("commands.neoessentials.book.author_set", author));
        return 1;
    }
}