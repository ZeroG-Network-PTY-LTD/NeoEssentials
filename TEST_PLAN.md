# NeoEssentials Mod Test Plan

## Overview
This document outlines the test plan for the NeoEssentials mod for Minecraft 1.21.1 NeoForge server. The testing is focused on validating the functionality of all implemented features and commands.

## Teleport Commands

### `/back` Command
- Join server and record position
- Teleport to another location using `/home` or `/warp`
- Use `/back` to return to original position
- Test that multiple teleports maintain the correct history stack
- Verify teleport cooldown works

### `/tpa` and Related Commands
- Test `/tpa <player>` to request teleportation to another player
- Test `/tpahere <player>` to request another player to teleport to you
- Verify target player receives notification
- Test `/tpaccept` to accept teleport request
- Test `/tpdeny` to deny teleport request
- Verify requests timeout after some time

### `/spawn` Command
- Test `/spawn` to teleport to spawn point
- Test `/setspawn` to set the spawn point at current location
- Verify the spawn point persists after server restart

## Home Commands

### `/home` Commands
- Test `/sethome` to set default home
- Test `/sethome <name>` to set named home
- Test `/home` to teleport to default home
- Test `/home <name>` to teleport to named home
- Test `/delhome <name>` to delete a home
- Test `/homes` to list all homes
- Verify home limit enforcement
- Verify homes persist after server restart

## Warp Commands

### `/warp` Commands
- Test `/setwarp <name>` to create a public warp {Not working}
- Test `/warp <name>` to teleport to a warp {Not working}
- Test `/delwarp <name>` to delete a warp {Not working}
- Test `/warps` to list all warps
- Test `/warpplayer <player> <warp>` to teleport another player to a warp {Not working}
- Verify warps persist after server restart

## Kit Commands

### `/kit` Commands
- Test `/createkit <name> <cooldown>` to create a kit from inventory
- Test `/kit <name>` to claim a kit  {Not working perms required though no perms mod in atm}
- Test `/kits` to list available kits
- Test `/deletekit <name>` to delete a kit
- Test `/givekit <player> <kit>` to give a kit to another player  {Not working}
- Verify kit cooldowns work properly
- Verify kits persist after server restart

## Time and Weather Commands

### Time Commands
- Test `/day` to set time to day {Not working}
- Test `/night` to set time to night {Not working}
<<<<<<< HEAD
<<<<<<< HEAD
- Test `/time set <value>` to set specific time {Not working}
- Test `/time add <value>` to add time {Not working}
=======
- Test `/time set <value>` to set specific time
- Test `/time add <value>` to add time
>>>>>>> eab9ffa (feat: Implement core event handling for NeoEssentials mod)
=======
- Test `/time set <value>` to set specific time {Not working}
- Test `/time add <value>` to add time {Not working}
>>>>>>> 796dc37 (refactor: Update warp command permissions and storage handling)

### Weather Commands {Not working}
- Test `/weather clear` to clear weather
- Test `/weather rain` to set rainy weather
- Test `/weather thunder` to set thunderstorm
- Test weather duration parameter

## Player/Economy Commands

### Economy Commands
- Test `/balance` (and aliases `/bal`, `/money`) to check own balance
- Test `/balance <player>` to check another player's balance
- Test `/pay <player> <amount>` to transfer money to another player
- Test `/pay <player>` with insufficient funds to verify error handling
- Test `/baltop` to check top richest players list
- Test `/baltop <page>` to check pagination of top players

#### Admin Economy Commands
- Test `/eco give <player> <amount>` to add money to a player's balance
- Test `/eco take <player> <amount>` to remove money from a player's balance
  - Verify behavior when trying to take more than the player has
- Test `/eco set <player> <amount>` to set a player's balance to a specific amount
  - Test with various amounts including zero
- Verify all economy changes are properly announced to affected players
- Verify economy data persists after server restart

## Performance Testing
- Test server performance with multiple players
- Monitor memory usage
- Check for any lag when using teleport commands

## Permission Testing
- Verify permission checks work correctly for all commands
- Test with both OP and regular players

## Error Handling
- Test appropriate error messages when commands are misused
- Verify teleport safety checks
- Test invalid command arguments handling

## Data Persistence
- Verify all data (homes, warps, kits, etc.) persists after server restart
- Check for any data corruption issues

## Database Integration Testing

### JSON Storage (Default)
- Verify that all data is correctly saved in JSON format
- Test that JSON files are created in the correct directories
- Test data loading on server startup
- Test data saving on server shutdown

### SQLite Integration
- Configure mod to use SQLite storage in config file
- Verify that SQLite database file is created
- Test all commands and verify data is properly stored in the database
- Test data persistence after server restart
- Check that database tables are correctly created
- Verify proper error handling if database file is corrupted

### MySQL Integration
- Configure mod to use MySQL storage in config file
- Set up a MySQL server for testing
- Test connection to MySQL database
- Verify all tables are created correctly
- Test all commands and verify data is properly stored in the database
- Test data persistence after server restart
- Test MySQL connection loss and reconnection behavior

## Permissions Integration Testing

### Default Permissions
- Verify that operators have access to all commands
- Test permission configuration in config file

### LuckPerms Integration
- Install LuckPerms on test server
- Set up different permission groups and users
- Test permissions for various commands with different user groups
- Verify permission inheritance works correctly

### FTB Ranks Integration
- Install FTB Ranks on test server
- Configure different ranks with varying permissions
- Test permissions for various commands with users of different ranks
- Verify rank-based permission system works correctly

## Notes
- Track any bugs or issues found during testing
- Document any suggested improvements or additional features
