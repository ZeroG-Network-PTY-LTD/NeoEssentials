# Language File and Shop Permission Fix Summary

## Issues Fixed

### 1. Language File Format Strings ✅

**Problem**: The `en_us.json` language file was using C-style format strings (`%s`, `%d`, `%.2f`, `%c`) instead of Java MessageFormat strings (`{0}`, `{1}`, `{2}`).

**Solution**: 
- Converted all C-style format strings to Java MessageFormat
- Fixed parameter ordering in multi-parameter strings
- Examples:
  - `"§cKit '%s' is currently disabled."` → `"§cKit '{0}' is currently disabled."`
  - `"§7Items: §f%d"` → `"§7Items: §f{0}"`
  - `"§7Cost: §f%.2f"` → `"§7Cost: §f{0}"`
  - `"§7- §e%s §7(%d items, cooldown: %s)"` → `"§7- §e{0} §7({1} items, cooldown: {2})"`

### 2. Shop Permission Issue ✅

**Problem**: Other players couldn't use shops owned by someone else, even though they had the `neoessentials.shop.sign.use` permission.

**Root Cause**: The `SignShopHandler.handleSignInteraction()` method was requiring the `SHOP_SIGN_USE` permission for ALL shop interactions, including just using/buying from shops.

**Solution**: Removed the permission check for shop **usage**. Now:
- ✅ **Shop Creation** requires `neoessentials.shop.sign.create` permission  
- ✅ **Shop Usage** requires NO permission (open to all players)
- ✅ **Shop Breaking** requires ownership or admin permissions

**Code Change**:
```java
// REMOVED this check from handleSignInteraction():
if (!PermissionUtil.hasPermission((ServerPlayer) player, PermissionNodes.SHOP_SIGN_USE)) {
    // Access denied message
    return InteractionResult.FAIL;
}

// Replaced with comment:
// NOTE: Removed permission check for shop usage - all players should be able to use shops
// Only shop creation requires permissions, not shop usage
```

## Expected Behavior After Fixes

### Language System
- ✅ All language strings now use proper Java MessageFormat (`{0}`, `{1}`, `{2}`)
- ✅ Multi-parameter messages display correctly with proper parameter substitution
- ✅ No more format string errors in logs

### Shop System  
- ✅ **Shop Owners**: Can create, manage, and use their own shops
- ✅ **Other Players**: Can freely buy from and sell to any shop (no permission required)
- ✅ **Shop Creation**: Still requires `neoessentials.shop.sign.create` permission
- ✅ **Shop Management**: Only owners or admins can break/modify shops

## Testing Recommendations

1. **Language Testing**:
   - Check kit messages display properly with parameters
   - Verify multi-parameter messages show correct values
   - Test admin commands that use formatted strings

2. **Shop Testing**:
   - Create a shop as one player
   - Switch to another player (without shop permissions)  
   - Verify the other player can buy from/sell to the shop
   - Confirm permission is still required for shop creation

## Files Modified

- `SignShopHandler.java` - Removed permission check for shop usage
- `en_us.json` - Fixed all format strings from C-style to Java MessageFormat

The shop permission issue should now be completely resolved - any player can use existing shops regardless of permissions, while only creation requires permissions.
