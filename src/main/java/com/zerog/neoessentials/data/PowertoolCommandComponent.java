package com.zerog.neoessentials.data;

import com.mojang.serialization.Codec;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

public class PowertoolCommandComponent {
    public static final ResourceLocation ID = new ResourceLocation("neoessentials", "powertool_command");
    public static final DataComponentType<String> TYPE =
        DataComponentType.builder(Codec.STRING).build();

    public static void register() {
        if (!BuiltInRegistries.DATA_COMPONENT_TYPE.containsKey(ID)) {
            BuiltInRegistries.DATA_COMPONENT_TYPE.register(ID, TYPE);
        }
    }

    public static void set(ItemStack stack, String command) {
        stack.set(TYPE, command);
    }

    public static String get(ItemStack stack) {
        return stack.get(TYPE);
    }
}
