package com.zerog.neoessentials.auctionhouse.gui;
import com.zerog.neoessentials.auctionhouse.AuctionItem;
import com.zerog.neoessentials.util.MessageUtil;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemLore;
import java.util.List;
/** Shared helpers for building ItemStacks used by Auction House GUI menus. */
public final class AuctionGuiHelper {
    private AuctionGuiHelper() {}
    public static ItemStack buildListingStack(AuctionItem ai) {
        ItemStack base = ai.getItemStack().copy();
        if (base.isEmpty()) return ItemStack.EMPTY;
        base.set(net.minecraft.core.component.DataComponents.LORE, new ItemLore(List.of(
            Component.literal(MessageUtil.localize("commands.neoessentials.ah.gui.seller", ai.getOwner())).withStyle(ChatFormatting.GRAY),
            Component.literal(MessageUtil.localize("commands.neoessentials.ah.gui.price", String.format("%.2f", ai.getPrice()))).withStyle(ChatFormatting.GOLD),
            Component.literal(MessageUtil.localize("commands.neoessentials.ah.gui.time", ai.getTimeLeft())).withStyle(ChatFormatting.DARK_PURPLE),
            Component.empty(),
            Component.literal(MessageUtil.localize("commands.neoessentials.ah.gui.click_to_buy")).withStyle(ChatFormatting.YELLOW)
        )));
        return base;
    }
    public static ItemStack buildPersonalListingStack(AuctionItem ai) {
        ItemStack base = ai.getItemStack().copy();
        if (base.isEmpty()) return ItemStack.EMPTY;
        base.set(net.minecraft.core.component.DataComponents.LORE, new ItemLore(List.of(
            Component.literal(MessageUtil.localize("commands.neoessentials.ah.gui.price", String.format("%.2f", ai.getPrice()))).withStyle(ChatFormatting.GOLD),
            Component.literal(MessageUtil.localize("commands.neoessentials.ah.gui.time", ai.getTimeLeft())).withStyle(ChatFormatting.DARK_PURPLE),
            Component.empty(),
            Component.literal(MessageUtil.localize("commands.neoessentials.ah.gui.click_to_manage")).withStyle(ChatFormatting.YELLOW)
        )));
        return base;
    }
    public static ItemStack buildExpiredStack(AuctionItem ai) {
        ItemStack base = ai.getItemStack().copy();
        if (base.isEmpty()) return ItemStack.EMPTY;
        base.set(net.minecraft.core.component.DataComponents.LORE, new ItemLore(List.of(
            Component.literal(MessageUtil.localize("commands.neoessentials.ah.gui.original_price", String.format("%.2f", ai.getPrice()))).withStyle(ChatFormatting.GOLD),
            Component.empty(),
            Component.literal(MessageUtil.localize("commands.neoessentials.ah.gui.click_to_collect")).withStyle(ChatFormatting.GREEN)
        )));
        return base;
    }
    public static ItemStack prevPageItem() {
        return named(headWithTexture(HeadTextures.GUI_PREVIOUS_PAGE),
                Component.translatable("spectatorMenu.previous_page").withStyle(ChatFormatting.WHITE));
    }
    public static ItemStack prevPageBlockedItem() {
        return named(headWithTexture(HeadTextures.GUI_PREVIOUS_PAGE_BLOCKED),
                Component.translatable("spectatorMenu.previous_page").withStyle(ChatFormatting.DARK_GRAY));
    }
    public static ItemStack nextPageItem() {
        return named(headWithTexture(HeadTextures.GUI_NEXT_PAGE),
                Component.translatable("spectatorMenu.next_page").withStyle(ChatFormatting.WHITE));
    }
    public static ItemStack nextPageBlockedItem() {
        return named(headWithTexture(HeadTextures.GUI_NEXT_PAGE_BLOCKED),
                Component.translatable("spectatorMenu.next_page").withStyle(ChatFormatting.DARK_GRAY));
    }
    public static ItemStack closeItem() {
        return named(new ItemStack(Items.BARRIER),
                Component.translatable("spectatorMenu.close").withStyle(ChatFormatting.RED));
    }
    public static ItemStack fillerItem() {
        return named(new ItemStack(Items.STAINED_GLASS_PANE.pick(net.minecraft.world.item.DyeColor.LIGHT_GRAY)), Component.literal(" "));
    }
    public static ItemStack backItem(String label) {
        return named(new ItemStack(Items.CONCRETE.pick(net.minecraft.world.item.DyeColor.RED)), Component.literal(label).withStyle(ChatFormatting.RED));
    }
    public static ItemStack myListingsItem() {
        return named(new ItemStack(Items.CHEST), Component.literal(MessageUtil.localize("commands.neoessentials.ah.gui.my_listings")).withStyle(ChatFormatting.GREEN));
    }
    public static ItemStack expiredItemsButton() {
        return named(new ItemStack(Items.HOPPER), Component.literal(MessageUtil.localize("commands.neoessentials.ah.gui.expired_items_button")).withStyle(ChatFormatting.RED));
    }
    public static ItemStack named(ItemStack stack, Component name) {
        stack.set(net.minecraft.core.component.DataComponents.CUSTOM_NAME, name);
        return stack;
    }
    private static ItemStack headWithTexture(String texture) {
        ItemStack head = new ItemStack(Items.PLAYER_HEAD);
        com.mojang.authlib.properties.PropertyMap properties = new com.mojang.authlib.properties.PropertyMap(
            com.google.common.collect.ImmutableMultimap.of("textures",
                new com.mojang.authlib.properties.Property("textures", texture)));
        com.mojang.authlib.GameProfile profile =
            new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(), "", properties);
        head.set(net.minecraft.core.component.DataComponents.PROFILE,
                net.minecraft.world.item.component.ResolvableProfile.createResolved(profile));
        return head;
    }
}