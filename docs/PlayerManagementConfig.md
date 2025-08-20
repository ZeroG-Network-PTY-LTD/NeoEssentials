# PlayerManagementConfig Options

This document lists all player management-related configuration options available in `main.json` for NeoEssentials. Each option can be set to `true` (enabled) or `false` (disabled).

## Options

- **list**: Enable `/list` command (list all online players)
- **whois**: Enable `/whois` command (show detailed info about a player)
- **seen**: Enable `/seen` command (show when a player was last online)
- **realname**: Enable `/realname` command (show a player's real Minecraft name)

- **nick**: Enable `/nick` command (set a nickname)
- **delnick**: Enable `/delnick` command (remove a nickname)

- **afk**: Enable `/afk` command (set yourself as AFK)
- **afkcheck**: Enable `/afkcheck` command (check if a player is AFK)

- **playerdata**: Enable `/playerdata` command (view or edit player data)
- **exp**: Enable `/exp` command (view or modify experience)
- **skull**: Enable `/skull` command (get a player's head item)

## How to Use
Edit the relevant options in your `main.json` config file under the `playerManagement` section. Example:

```json
"playerManagement": {
  "list": true,
  "whois": true,
  "seen": true,
  "realname": true,
  "nick": true,
  "delnick": true,
  "afk": true,
  "afkcheck": true,
  "playerdata": true,
  "exp": true,
  "skull": true
}
```

Refer to this document for descriptions of each option.
