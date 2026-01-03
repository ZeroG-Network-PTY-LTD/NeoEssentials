# Issues That Were Discovered and Fixed

## ✅ Fixed in v1.0.2.3 (January 4, 2026) - Build #596

### Critical Fixes
- ✅ **FIXED - Duplicate Event Handlers Causing Log Spam**: EnhancedAfkActivityHandler and AfkActivityHandler were both registered as event subscribers, causing duplicate event processing and thousands of WARN messages per minute for suspicious activity patterns. (Solution: Deleted old AfkActivityHandler.java and replaced it with the Enhanced version under the standard name. Consolidated to single handler with smart pattern detection. Fixed all debug logging to use DebugLogger.log() to respect `logging.enableDebugLogging` config. Eliminated log spam - now only logs when debug is enabled. 50% reduction in event processing overhead. See ENHANCED_HANDLER_CONSOLIDATION.md for complete details.)

- ✅ **FIXED - Debug Logging Consolidation**: Duplicate debug config options (`modules.debugMode` and `logging.enableDebugLogging`) caused inconsistent behavior. (Solution: Removed `modules.debugMode` from config.json. Made `isDebugModeEnabled()` delegate to `isDebugLoggingEnabled()`. Audited AFK system (AfkManager.java, AfkTablistHandler.java, AfkActivityHandler.java) and fixed 6 debug logs to use DebugLogger.log(). All debug logging now respects single `logging.enableDebugLogging` config. See DEBUG_LOGGING_CONSOLIDATION_AFK.md for audit results.)

- ✅ **FIXED - Kit System Crash on Minecraft 1.21.4**: Server crashed with NoSuchMethodError when loading kits due to Minecraft 1.21.4 Registry API changes. (Solution: Changed Kit.fromJson() to use `getOptional()` instead of `get()` for registry lookups. Line 183 in Kit.java updated from `BuiltInRegistries.ITEM.get(itemId)` to `BuiltInRegistries.ITEM.getOptional(itemId).orElse(null)` with null checking. Unknown items are now gracefully skipped. Server now starts successfully and kits load correctly.)

- ✅ **FIXED - /tpahere Command NullPointerException**: The `/tpahere` command crashed with NullPointerException when players tried to send teleport requests. (Solution: Fixed TeleportRequestManager.sendTeleportRequest() which was attempting to insert null values into ConcurrentHashMap (line 107). Changed from `sentRequests.putIfAbsent(requesterId, null)` to `sentRequests.containsKey(requesterId)` check. Removed null cleanup calls. ConcurrentHashMap does not allow null keys or values. All teleport request commands (/tpa, /tpahere, /tpaccept, /tpdeny) now work correctly.)

### New Features
- ✅ **IMPLEMENTED - /tpo Command**: `/tpo` command had implementation in DirectTeleportManager but was never registered in DirectTeleportCommands. (Solution: Added command registration for `/tpo` (teleport to offline player's last known location). Created registerTpoCommand() and teleportToOfflinePlayer() methods. Updated misleading comment about `/back` command (which was already implemented). Permission: `neoessentials.teleport.admin.tpo`. See TELEPORTATION_INVESTIGATION.md for complete teleport system analysis.)

- ✅ **IMPLEMENTED - LuckPerms Permission Sync**: NeoEssentials permissions were not visible in LuckPerms autocomplete or web editor. (Solution: Added automatic permission synchronization with LuckPerms. Created `syncWithLuckPerms()` method in PermissionRegistry and `registerPermissions()` method in LuckPermsAdapter. All 85+ permissions now automatically sync on startup. Permissions appear in LuckPerms autocomplete (`/lp user <player> permission set neoessentials.<TAB>`) and web editor. Added `exportForLuckPerms()` method for YAML export. See LUCKPERMS_PERMISSION_SYNC.md for integration guide.)

### Permission System
- ✅ **FIXED - Missing Permission Nodes**: 15 utility commands (stonecutter, anvil, crafting, fletching, smithing, realname, whois, seen, sign, rules, suicide, ping, etc.) were missing permission node registrations in PermissionRegistry.java. (Solution: Added all 15 missing permission nodes to PermissionRegistry.registerAllPermissions(). All permissions properly categorized as MISC or ADMIN with appropriate defaults. Full LuckPerms integration now available. See MISSING_PERMISSION_NODES_FIXED.md for complete list.)

- ✅ **FIXED - External Permissions Priority**: When `useExternalPermissions` was set to true, ops were bypassing LuckPerms entirely. (Solution: Reordered permission checking in PermissionAPI.hasPermission() to prioritize external adapter (LuckPerms) FIRST before ops bypass. Now: LuckPerms → Ops (internal only) → Internal. This ensures LuckPerms permissions are always respected when enabled.)

### Configuration & Dashboard
- ✅ **FIXED - Dashboard Port Configuration Not Working**: Dashboard port and bind address changes in config.json were ignored - server always used default port 8080 instead of configured port. (Solution: Changed DashboardAPI from caching port in singleton to reading port dynamically from config on each start(). Removed immutable port/bindAddress fields, added dynamic getPort() and getBindAddress() methods that read from ConfigManager. Port and bind address now properly updated when config changes. Requires restart or `/dashboard restart` to apply changes.)

### Code Quality
- ✅ **FIXED - Code Analysis Warnings**: Fixed 27 code analysis warnings across 7 files (Kit.java, PermissionAPI.java, TeleportRequestManager.java, PermissionRegistry.java, AfkManager.java, AfkTablistHandler.java, AfkActivityHandler.java). (Solution: Added @SuppressWarnings("unused") to 20+ public API methods, made 5 fields final, removed unused metadata field, updated to pattern matching instanceof, converted old-style switch to enhanced switch expression, added null safety checks. Zero compilation warnings. See PERMISSION_REGISTRY_WARNINGS_FIXED.md for details.)

### Verification
- ✅ **VERIFIED - TPA/TPAHERE Logic Correct**: Investigation confirmed TPA and TPAHERE commands are working as designed. TPA teleports requester TO target. TPAHERE teleports target TO requester. Logic is correct. (See TPA_TPAHERE_IMPLEMENTATION.md for detailed flow diagrams and implementation explanation.)

---

## 📚 Documentation Created (v1.0.2.3)

1. `DEBUG_LOGGING_CONSOLIDATION_AFK.md` - AFK system audit results
2. `ENHANCED_HANDLER_CONSOLIDATION.md` - Duplicate handler fix details
3. `TELEPORTATION_INVESTIGATION.md` - Complete teleport system analysis
4. `TPA_TPAHERE_IMPLEMENTATION.md` - TPA/TPAHERE flow diagrams
5. `LUCKPERMS_PERMISSION_SYNC.md` - LuckPerms integration guide
6. `PERMISSION_REGISTRY_WARNINGS_FIXED.md` - Code quality improvements
7. `MISSING_PERMISSION_NODES_FIXED.md` - Permission node documentation
8. `SESSION_SUMMARY_v1.0.2.3.md` - Complete session summary

---

## 🎯 Additional Features

- **More detailed kit permissions**: Allow kits to have more specific permission nodes for access.
- **Chat formatting options**: More options for customizing chat format.
- **Negative Permissions**: Allow negative permissions to be set for more granular control.

