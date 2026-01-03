# Issues That Were Discovered
 - ✅ **FIXED - Duplicate Event Handlers Causing Log Spam**: EnhancedAfkActivityHandler and AfkActivityHandler were both registered as event subscribers, causing duplicate event processing and thousands of WARN messages per minute for suspicious activity patterns. (Solution: Deleted old AfkActivityHandler.java and replaced it with the Enhanced version under the standard name. Consolidated to single handler with smart pattern detection. Fixed all debug logging to use DebugLogger.log() to respect logging.enableDebugLogging config. Eliminated log spam - now only logs when debug is enabled. See ENHANCED_HANDLER_CONSOLIDATION.md for complete details.)
- ✅ **FIXED - Debug Logging Consolidation**: Duplicate debug config options (modules.debugMode and logging.enableDebugLogging) caused inconsistent behavior. (Solution: Removed modules.debugMode from config.json. Made isDebugModeEnabled() delegate to isDebugLoggingEnabled(). Audited AFK system (AfkManager.java, AfkTablistHandler.java, AfkActivityHandler.java) and fixed 6 debug logs to use DebugLogger.log(). All debug logging now respects single logging.enableDebugLogging config. See DEBUG_LOGGING_CONSOLIDATION_AFK.md for audit results.)
# Additional Added Features

- **More detailed kit permissions**: Allow kits to have more specific permission nodes for access.
- **Chat formatting options**: More options for customizing chat format.
- **Negative Permissions**: Allow negative permissions to be set for more granular control.
