# Discord Role ID Setup Guide

## Overview
NeoEssentials now uses Discord Role IDs instead of role names for more reliable permission linking. This guide will help you configure your Discord integration properly.

## Why Use Role IDs?
- **Reliability**: Role IDs never change, even if you rename the role
- **Precision**: No confusion with similar role names
- **Stability**: System continues working even if roles are renamed

## How to Get Discord Role IDs

### Method 1: Discord Developer Mode (Recommended)
1. Open Discord and go to **User Settings** (gear icon)
2. Go to **Advanced** settings
3. Enable **Developer Mode**
4. Right-click on any role mention or in the server settings
5. Click **Copy ID**

### Method 2: Using Discord Bot Commands
If you have a Discord bot with role management, you can use commands like:
```
!role info @RoleName
```

### Method 3: Discord API/Browser Inspector
1. Open Discord in a web browser
2. Press F12 to open Developer Tools
3. Go to Network tab
4. Navigate to Server Settings → Roles
5. Look for API calls containing role information

## Configuration Steps

### 1. Update tablist.json
Replace the example role IDs in your `config/neoessentials/tablist.json` file:

```json
{
  "discordIntegration": {
    "roleSync": {
      "enabled": true,
      "syncOnJoin": true,
      "syncInterval": 300,
      "bidirectional": true,
      "roleMappings": {
        "YOUR_OWNER_ROLE_ID": {
          "_roleName": "Owner",
          "minecraftPermission": "neoessentials.admin",
          "priority": 1000,
          "tablistPrefix": "&4[OWNER]&r",
          "scoreboardTitle": "&4&lOWNER"
        },
        "YOUR_ADMIN_ROLE_ID": {
          "_roleName": "Admin", 
          "minecraftPermission": "neoessentials.moderator",
          "priority": 800,
          "tablistPrefix": "&c[ADMIN]&r",
          "scoreboardTitle": "&c&lADMIN"
        }
        // ... add more roles as needed
      }
    }
  }
}
```

### 2. Example Role ID Configuration
Here's a real example with actual role IDs (replace with your own):

```json
"roleMappings": {
  "987654321098765432": {
    "_roleName": "Server Owner",
    "minecraftPermission": "neoessentials.admin",
    "priority": 1000,
    "tablistPrefix": "&4[OWNER]&r",
    "scoreboardTitle": "&4&lOWNER"
  },
  "876543210987654321": {
    "_roleName": "Administrator", 
    "minecraftPermission": "neoessentials.moderator",
    "priority": 800,
    "tablistPrefix": "&c[ADMIN]&r",
    "scoreboardTitle": "&c&lADMIN"
  },
  "765432109876543210": {
    "_roleName": "Moderator",
    "minecraftPermission": "neoessentials.helper", 
    "priority": 600,
    "tablistPrefix": "&6[MOD]&r",
    "scoreboardTitle": "&6&lMODERATOR"
  },
  "654321098765432109": {
    "_roleName": "VIP Member",
    "minecraftPermission": "neoessentials.vip",
    "priority": 400,
    "tablistPrefix": "&d[VIP]&r",
    "scoreboardTitle": "&d&lVIP"
  },
  "543210987654321098": {
    "_roleName": "Member",
    "minecraftPermission": "neoessentials.member", 
    "priority": 200,
    "tablistPrefix": "&a[MEMBER]&r",
    "scoreboardTitle": "&a&lMEMBER"
  },
  "432109876543210987": {
    "_roleName": "Verified",
    "minecraftPermission": "neoessentials.verified",
    "priority": 100,
    "tablistPrefix": "&7[VERIFIED]&r", 
    "scoreboardTitle": "&7&lVERIFIED"
  }
}
```

## Important Notes

### Role ID Format
- Discord role IDs are 18-19 digit numbers
- Always use quotes around role IDs in JSON: `"123456789012345678"`
- Do not include special characters or spaces

### Priority System
- Higher priority numbers = higher rank
- Players get permissions from their highest priority role
- Fallback role is used if no Discord roles match

### Testing Your Configuration
1. Save your configuration file
2. Restart your server or reload the mod
3. Join the server with a Discord-linked account
4. Check the console logs for role synchronization messages
5. Verify that your tablist and permissions are correct

## Troubleshooting

### Common Issues
1. **Role ID not working**: Verify the ID is correct and the bot has permission to see the role
2. **Permissions not syncing**: Check that the Discord link integration is enabled
3. **Player not getting role**: Ensure the player's Discord account is properly linked

### Debug Information
Enable debug logging in your configuration:
```json
"errorHandling": {
  "logErrors": true,
  "retryAttempts": 3,
  "retryDelay": 5000
}
```

### Log Messages to Look For
- `[SimpleDiscordLinkIntegration] Synced Discord role ID 'XXXXX' (RoleName) to permission 'permission.name'`
- `[SimpleDiscordLinkIntegration] No permission mapping found for Discord role ID: XXXXX`

## Migration from Role Names

If you're upgrading from a role name-based system:

1. **Backup your current configuration**
2. **Get all your Discord role IDs** using the methods above
3. **Replace role names with role IDs** in the configuration
4. **Test thoroughly** before deploying to production
5. **Update any documentation** that references role names

## Support

If you need help with Discord role ID setup:
1. Check the console logs for error messages
2. Verify your Discord bot permissions
3. Test with a single role first before configuring all roles
4. Join our Discord server for support

---

**Note**: The `_roleName` field in the configuration is optional and only used for human readability. The actual role matching is done by the Discord role ID (the key).
