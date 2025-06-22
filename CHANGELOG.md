# NeoEssentials - Changelog

## Version 1.0.1 (Planned - In Development)

### Tablist System Overhaul
- **Enhanced Animation System**
  - New animation types: scrolling text, fade effects, rainbow colors, typewriter, and blinking text
  - Customizable animation speeds and transitions
  - Per-group animation settings

- **Advanced Configuration**
  - Complete migration from legacy JSON to TOML configuration
  - Expanded configuration options with detailed comments
  - Group-specific header and footer settings

- **Improved Placeholders**
  - Expanded placeholder library for server and player information
  - Dynamic placeholder processing with conditional support
  - Custom placeholder API for addon development

- **Visual Improvements**
  - Advanced text formatting with rank-based styling
  - Custom icons and symbols in the tablist
  - Colored ping indicators and status icons

- **Player Grouping and Sorting**
  - Group players by rank/permissions
  - Multiple sorting methods (name, rank, playtime)
  - Group headers within the tablist

- **Performance Optimizations**
  - Batch update system to reduce network traffic
  - Smart update scheduling based on content changes
  - Optimized placeholder processing and caching

## Version 1.0.0.1 (June 22, 2025)

### Initial Release for Minecraft NeoForge 1.21.1

#### Major Features
- **Complete Teleportation System**
  - Home system with multi-home support and permissions
  - Server warps with permission-based access
  - TPA request system for player-to-player teleportation
  - Tracking of previous locations with `/back` command

- **Economy System**
  - Balance tracking and management
  - Player-to-player payments
  - Admin economy commands for server management
  - Support for shop integration

- **Admin Tools**
  - Comprehensive admin panel for server management
  - Professional moderation tools (ban, kick, mute)
  - Server performance monitoring utilities
  - Maintenance mode with permission-based bypass

- **Player Utilities**
  - Kit system with cooldowns and permission controls
  - Offline mail messaging system
  - Jail system for rule enforcement
  - Vanish functionality for staff
  - PowerTools for binding commands to items

- **General Features**
  - AFK detection and notification system
  - Chat formatting and management
  - Time and weather control commands
  - Tab completion with smart suggestions
  - Integration with permission systems

#### Technical Improvements
- **True Server-Side Implementation**
  - Zero client-side requirements
  - Compatible with vanilla and modded clients
  - No client disconnects in modded environments
  - Only uses vanilla-compatible command argument types
  
- **Performance Optimizations**
  - Efficient data storage and retrieval
  - Optimized command processing
  - Minimal impact on server resources
  
- **Bug Fixes**
  - Fixed command argument registry synchronization
  - Resolved client-server connection issues
  - Enhanced compatibility with other mods
  - Improved error handling and feedback

#### Storage System
- Implemented three storage backend options:
  - JSON file-based storage (default)
  - SQLite database support
  - MySQL integration for multi-server setups

#### Documentation
- In-game command help and suggestions
- Detailed configuration file comments
- Wiki documentation for all features

---

## Known Issues
- None reported yet. Please submit any issues to our [GitHub Issue Tracker](https://github.com/zerog/neoessentials/issues).

---

## Future Plans
- Additional economy features
- More administrative tools
- Extended customization options
- Expanded API for mod integration
