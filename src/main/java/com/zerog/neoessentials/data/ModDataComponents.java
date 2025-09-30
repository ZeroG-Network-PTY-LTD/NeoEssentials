package com.zerog.neoessentials.data;

import net.minecraft.core.component.DataComponentType;
import net.minecraft.resources.ResourceLocation;
import com.mojang.serialization.Codec;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;

public class ModDataComponents {
    public static final ResourceLocation POWERTOOL_COMMAND_ID = ResourceLocation.parse("neoessentials:powertool_command");
    public static final DataComponentType<String> POWERTOOL_COMMAND = DataComponentType.<String>builder()
        .persistent(Codec.STRING)
        .build();

    public static final ResourceLocation POWERTOOL_TOGGLE_ID = ResourceLocation.parse("neoessentials:powertool_toggle");
    public static final DataComponentType<Boolean> POWERTOOL_TOGGLE = DataComponentType.<Boolean>builder()
        .persistent(Codec.BOOL)
        .build();

    public static void register() {
        Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE, POWERTOOL_COMMAND_ID, POWERTOOL_COMMAND);
        Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE, POWERTOOL_TOGGLE_ID, POWERTOOL_TOGGLE);
    }
}
