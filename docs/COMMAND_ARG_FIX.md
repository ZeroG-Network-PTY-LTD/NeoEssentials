# Command Argument Registry Fix

## Issue
The mod was encountering a connection loss issue with the following error:
```
Connection lost 
The server send registeries with unknown keys:
ResourceKey[minecraft:command_argument_type / neoessentials:String_to_boolean]
```

## Root Cause
<<<<<<< HEAD
<<<<<<< HEAD
=======
>>>>>>> 6929d4c (feat: Update build number and document command argument type registry fix to resolve connection issues)
Two issues were identified:

1. First issue (connection loss):
   - The server was looking for a registry key with capitalization `"String_to_boolean"`
   - The mod was registering it as `"string_to_boolean"` (all lowercase)

2. Second issue (startup crash):
   - Using uppercase characters in ResourceLocation paths is not allowed 
   - ResourceLocations (registry names) can only contain lowercase letters, numbers, and specific symbols [a-z0-9/._-]
<<<<<<< HEAD

## Fix Applied
1. Ensured the command argument type registration key in `ModArgumentTypes.java` is properly lowercase as `"string_to_boolean"` (Minecraft resource locations only allow lowercase a-z, numbers 0-9, and a few special characters like /._-)
=======
The issue was related to a capitalization mismatch in the command argument type registration:
1. The server was looking for a registry key with capitalization `"String_to_boolean"`
2. The mod was registering it as `"string_to_boolean"` (all lowercase)

## Fix Applied
1. Changed the command argument type registration key in `ModArgumentTypes.java` from `"string_to_boolean"` to `"StringToBoolean"`
>>>>>>> f49502a (fix: Update build process and fix command argument registration issues to prevent connection loss)
=======

## Fix Applied
1. Ensured the command argument type registration key in `ModArgumentTypes.java` is properly lowercase as `"string_to_boolean"` (Minecraft resource locations only allow lowercase a-z, numbers 0-9, and a few special characters like /._-)
>>>>>>> 6929d4c (feat: Update build number and document command argument type registry fix to resolve connection issues)
2. Removed the redundant direct registration via `ArgumentTypeInfos.registerByClass()` in the common setup method, which could have been causing conflicting registrations
3. Fixed a formatting issue in `PlayerCommands.java` related to the command registration

## Technical Details
Command argument types need to be registered consistently between client and server. By using only the `DeferredRegister` method for registration and ensuring consistent naming conventions, we avoid conflicts that can cause connection issues.

This update ensures that only a single, consistent registration mechanism is used for custom command argument types, preventing connection and registry mismatch issues.
