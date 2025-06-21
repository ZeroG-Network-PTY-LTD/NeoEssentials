# Client-Server Synchronization Fix

## Issue

When connecting to a server with NeoEssentials installed, clients were disconnecting with the following error:

```
[Render thread/WARN] [net.minecraft.client.multiplayer.ClientCommonPacketListenerImpl/]: Client disconnected with reason: The server send registries with unknown keys: ResourceKey[minecraft:command_argument_type / neoessentials:string_to_boolean]
```

This error occurs because the server registers custom command argument types that the client doesn't know about, causing a registry mismatch.

## Root Cause

Although NeoEssentials is primarily a server-side mod, certain registries (like command argument types) must be synchronized between the server and all connecting clients. When these registrations aren't properly set up for synchronization, clients without the information will disconnect.

The issue was in the registration and synchronization of the `StringToBooleanArgumentType`:

1. The mod was registering the command argument type using `DeferredRegister`
2. However, it wasn't properly ensuring that clients would receive and recognize this type
3. This led to a registry mismatch when clients connected to the server

## Fix Applied

1. **Improved Command Argument Type Registration:**
   - Updated `ModArgumentTypes.java` to register the argument type in a client-compatible way
   - Used both `DeferredRegister` and explicit `ArgumentTypeInfos.registerByClass()` in the common setup phase

2. **Enhanced Client-Server Synchronization:**
   - Added proper CommonSetup event handling to ensure synchronization
   - Made sure the class-to-info mapping is registered correctly for network serialization

3. **Explicit Documentation:**
   - Added clear documentation about the client-server synchronization requirements
   - Updated the code comments to explain the dual registration approach

## Technical Details

For custom argument types to work across client and server:

1. The type must be registered with `DeferredRegister` for the registry itself
2. The mapping between the argument type class and its info class must be registered with `ArgumentTypeInfos.registerByClass()`
3. This registration must happen during the common setup phase, which is shared by both client and server

```java
private static void onCommonSetup(FMLCommonSetupEvent event) {
    event.enqueueWork(() -> {
        ArgumentTypeInfo<StringToBooleanArgumentType, ?> info = STRING_TO_BOOLEAN.get();
        ArgumentTypeInfos.registerByClass(StringToBooleanArgumentType.class, info);
    });
}
```

## Testing

After applying this fix, clients should be able to connect to servers running NeoEssentials without disconnection errors related to unknown registries.

## Best Practices for Registry Synchronization

When adding any custom registry entries that need client-server synchronization:

1. **Use DeferredRegister** for the core registry entry
2. **Ensure proper serialization** is set up for network communication
3. **Test in a client-server environment** to verify synchronization
4. **Remember that even server-side mods** may need to register some data on clients
5. **Document synchronization requirements** for future maintenance
