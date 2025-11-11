## API & Placeholder System

### Features
- **PlaceholderAPI Integration**: Centralized system for registering and resolving placeholders in chat, join/quit messages, and other text formatting contexts.
- **Custom Placeholders**: Mods and server owners can register custom placeholders using `PlaceholderAPI.registerPlaceholder`.
- **Placeholder Expansions**: Support for registering multiple related placeholders via expansions.
- **Configurable Placeholders**: Many config options and messages support placeholders, including those from PlaceholderAPI.
- **Web Dashboard API**: RESTful endpoints for server status, player info, logs, config, events, statistics, and more (see `dashboard.js`).

### Key Config Options (`config.json`)
- Placeholders are referenced in chat format, join/quit messages, MOTD, and other configurable messages.
- Permissions: `neoessentials.integration.placeholderapi` for accessing PlaceholderAPI features.

### Key Classes
- `PlaceholderManager`: Central manager for registration, resolution, and management of placeholders.
- `PlaceholderAPI`: Interface for registering, resolving, and managing placeholders and expansions.
- `PlaceholderExpansion`: Abstract class for registering multiple related placeholders.

### API Endpoints (Web Dashboard)
- `/api/auth/*` - Authentication endpoints
- `/api/player/online` - Get online players
- `/api/server/status` - Get server status
- `/api/server/profile` - Get server profile
- `/api/server/statistics` - Get server statistics
- `/api/server/performance` - Get server performance
- `/api/server/worlds` - Get worlds info
- `/api/game/events` - Get game events
- `/api/game/statistics` - Get game statistics

### Permissions
- `neoessentials.integration.placeholderapi` - Access PlaceholderAPI features

### Notes
- Placeholders can be used in most configurable messages and formats.
- API endpoints are documented in `dashboard.js` and used by the web dashboard for real-time server management.
