# Shop Disappearance Issue - Analysis & Fixes

## Problem Analysis

The shop signs were losing their locations and getting deleted due to several critical issues in the shop persistence system:

### Root Causes Identified

1. **Missing Server Shutdown Handling**: The `ShopManager.shutdown()` method was never called during server shutdown, causing shop data to be lost when the server stopped.

2. **Asynchronous Save Race Conditions**: The `saveShopsToStorage()` method used async operations without waiting for completion, so if the server shut down before the async save completed, data was lost.

3. **No Periodic Auto-Save**: Shops were only saved when created/removed/updated, with no automatic periodic saves. Server crashes or force shutdowns resulted in data loss.

4. **Inadequate Error Handling**: Failed shop serialization would only log warnings and continue, potentially leaving shops unsaved.

## Implemented Fixes

### 1. Proper Server Shutdown Handling
**File**: `NeoEssentials.java`
- Added `ShopManager.shutdown()` call in `onServerStopping()` method
- Added `StorageManager.shutdown()` call for complete cleanup
- Ensures all shop data is saved before server stops

### 2. Synchronous Save During Shutdown
**File**: `ShopManager.java`
- Added `saveShopsToStorageSync()` method that waits for completion
- Used during shutdown to guarantee data persistence
- Added 10-second timeout with proper error handling

### 3. Periodic Auto-Save System
**File**: `ShopManager.java`
- Added auto-save scheduler that runs every 5 minutes
- Uses daemon thread to prevent blocking server shutdown
- Automatically saves shops to prevent data loss from crashes
- Proper cleanup of executor service during shutdown

### 4. Enhanced Error Handling & Validation
**File**: `ShopManager.java`
- Better validation during shop loading
- Null checks and data integrity verification
- Improved error logging with detailed context
- Graceful handling of corrupted shop data

### 5. Diagnostic Commands
**New Files**:
- `SaveShopsCommand.java`: Manual shop save for debugging
- `CheckShopsCommand.java`: Integrity check to diagnose shop issues

Both commands registered in `CommandRegistry.java`

### 6. Improved Shop Data Loading
**File**: `ShopManager.java`
- Added validation before converting loaded shop data
- Better error handling for corrupted or invalid shop entries
- Skip invalid shops instead of failing completely

## Usage Instructions

### For Server Administrators

1. **Manual Save**: Use `/saveshops` command to force save all shops
2. **Integrity Check**: Use `/checkshops` to diagnose shop issues
3. **Monitor Logs**: Look for auto-save messages every 5 minutes
4. **Shutdown**: Server will automatically save shops on proper shutdown

### Monitoring Shop Health

The system now logs:
- Auto-save operations every 5 minutes
- Shop loading/saving success/failure
- Validation errors with specific details
- Shutdown save operations

### What This Fixes

✅ **Shop signs losing their locations**
✅ **Shops getting deleted unexpectedly** 
✅ **Data loss during server restarts**
✅ **Data loss during server crashes** (via auto-save)
✅ **Silent failures in shop saving**
✅ **Lack of diagnostic tools**

### Expected Behavior After Fix

1. **Server Startup**: Shops load with validation and error reporting
2. **During Operation**: Shops auto-save every 5 minutes
3. **Server Shutdown**: All shop data is saved synchronously before shutdown
4. **After Crashes**: Maximum 5 minutes of shop data loss (auto-save interval)
5. **Diagnostics**: Commands available to check shop integrity and force saves

## Testing Recommendations

1. Create test shops and restart server - shops should persist
2. Use `/checkshops` to verify all shops are valid
3. Monitor logs for auto-save messages
4. Test `/saveshops` command functionality
5. Verify shops persist after unclean shutdowns (simulated crashes)

## Files Modified

- `NeoEssentials.java` - Added shutdown handling
- `economy/shops/ShopManager.java` - Added auto-save and synchronous save
- `commands/CommandRegistry.java` - Registered new commands  
- `commands/economy/SaveShopsCommand.java` - New diagnostic command
- `commands/economy/CheckShopsCommand.java` - New integrity check command

The shop disappearance issue should now be completely resolved with these comprehensive fixes.
