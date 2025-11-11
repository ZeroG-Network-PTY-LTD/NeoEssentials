## Utility Systems

### Features
- **AFK System**: Automatic AFK detection, broadcasts, tablist indicator, activity tracking, AFK kick, and custom messages. Configurable via `afk` section in config.
- **Nicknames**: Change player display names with `/nick` and `/realname` commands.
- **Mail System**: (If implemented) Send/receive in-game mail/messages.
- **MOTD**: Display server message of the day with `/motd` command.
- **Near**: Show nearby players with `/near` command.
- **Ping**: Check player/server latency with `/ping` command.
- **Depth**: Show current depth with `/depth` command.
- **GetPos**: Show player coordinates with `/getpos` command.
- **List**: List online players with `/list` command.
- **Seen**: Show last seen info for players with `/seen` command.
- **HelpOp**: Send help requests to staff with `/helpop` command.
- **Suicide**: Self-elimination with `/suicide` command.
- **Rules**: Display server rules with `/rules` command.

### Key Config Options (`config.json`)
- `afk`: Enable/disable AFK system, timeouts, messages, tablist indicator, activity tracking, excluded commands, auto-save interval.
- Other utility features are enabled/disabled via the `commands` section (e.g., `nick`, `motd`, `ping`, etc.).

### Commands
- `/afk`, `/ignore`, `/unignore`, `/mute`, `/unmute`, `/msgtoggle`, `/socialspy`, `/mutelist`
- `/nick`, `/realname`
- `/motd`, `/near`, `/ping`, `/depth`, `/getpos`, `/list`, `/seen`, `/helpop`, `/suicide`, `/rules`

### Permissions
- Permissions for utility commands are typically `neoessentials.command.<command>` (e.g., `neoessentials.command.nick`).
- AFK system may use `neoessentials.afk.*` for advanced features.

### Notes
- Utility systems are highly configurable and cover quality-of-life features for players and staff.
- Some features (e.g., mail) may require additional config or database support if implemented.
