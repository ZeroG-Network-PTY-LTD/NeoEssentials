import net.minecraft.nbt.CompoundTag;
package com.zerog.neoessentials.data;

// ...existing code...
import net.minecraft.world.item.ItemStack;

public class PowertoolCommandComponent {
    // ...existing code...


    // Registration should be done using DeferredRegister in your mod's main class or a ModDataComponents class.
    // Example (in ModDataComponents.java):
    // public static final DeferredRegister<DataComponentType<?>> DATA_COMPONENT_TYPES = DeferredRegister.create(BuiltInRegistries.DATA_COMPONENT_TYPE.key(), "neoessentials");
    // public static final RegistryObject<DataComponentType<String>> POWERTOOL_COMMAND = DATA_COMPONENT_TYPES.register("powertool_command", () -> DataComponentType.<String>builder().build());

    public static void set(ItemStack stack, String command) {
        CompoundTag tag = stack.getTag();
        if (tag == null) {
            tag = new CompoundTag();
            stack.setTag(tag);
        }
        tag.putString("NeoEssentialsPowertoolCommand", command);
    }

    public static String get(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        return tag != null && tag.contains("NeoEssentialsPowertoolCommand") ? tag.getString("NeoEssentialsPowertoolCommand") : "";
    }
}
// PowertoolCommandComponent removed: all powertool data is now server-side only for server-only compatibility.
