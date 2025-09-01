# FTB Integration Examples

This document provides examples of how to use the FTB Teams, Ranks, and Library integration within NeoEssentials.

## Available FTB Placeholders

### Team Placeholders
- `%ftb_team_name%` - The team's internal name
- `%ftb_team_display_name%` - The team's display name
- `%ftb_team_role%` - Player's role in the team (Owner, Moderator, Member)
- `%ftb_team_members%` - Number of team members
- `%ftb_team_prefix%` - Team prefix
- `%ftb_team_suffix%` - Team suffix
- `%ftb_team_color%` - Team color code

### Rank Placeholders
- `%ftb_rank_name%` - The rank's internal name
- `%ftb_rank_display_name%` - The rank's display name
- `%ftb_rank_prefix%` - Rank prefix
- `%ftb_rank_suffix%` - Rank suffix
- `%ftb_rank_color%` - Rank color code
- `%ftb_rank_weight%` - Rank weight/priority
- `%ftb_rank_permissions%` - Number of permissions in rank

### Combined Placeholders
- `%ftb_combined_prefix%` - Best prefix from rank and team combined
- `%ftb_combined_suffix%` - Best suffix from rank and team combined

### Status Placeholders
- `%ftb_has_team%` - "true" if player has a team, "false" otherwise
- `%ftb_has_rank%` - "true" if player has a rank, "false" otherwise

### Legacy Aliases (for compatibility)
- `%team_name%` - Same as `%ftb_team_name%`
- `%rank_name%` - Same as `%ftb_rank_name%`
- `%team_role%` - Same as `%ftb_team_role%`

## Example Tablist Configuration with FTB Integration

Here's an enhanced tablist configuration that uses FTB Teams and Ranks:

```json
{
  "enableTablist": true,
  "tablistFormat": "{ftb_combined_prefix}[{team_name}] {player_name}{ftb_combined_suffix} | {ftb_team_role}",
  "enableScoreboard": true,
  "scoreboardFormat": "Team: {team_name} | Rank: {rank_name} | Player: {player_name}",
  "enableBossbar": true,
  "bossbarFormat": "{ftb_rank_prefix}{player_name}{ftb_rank_suffix} | Team: {team_name}",
  "tablistLayouts": [
    {
      "priority": 200,
      "conditionType": "placeholder",
      "condition": "ftb_team_role:Owner",
      "header": [
        "&6&l═══════════════════════════",
        "&e&lTEAM OWNER &7| &f{player_name}",
        "&7Team: &b{ftb_team_display_name}",
        "&7Rank: &a{ftb_rank_display_name}",
        "&6&l═══════════════════════════"
      ],
      "footer": [
        "&7Team Members: &e{ftb_team_members}",
        "&7Your Role: &c&lOWNER",
        "&7Rank Weight: &e{ftb_rank_weight}"
      ]
    },
    {
      "priority": 150,
      "conditionType": "placeholder",
      "condition": "ftb_team_role:Moderator",
      "header": [
        "&9&l═══════════════════════════",
        "&b&lTEAM MODERATOR &7| &f{player_name}",
        "&7Team: &b{ftb_team_display_name}",
        "&7Rank: &a{ftb_rank_display_name}",
        "&9&l═══════════════════════════"
      ],
      "footer": [
        "&7Team Members: &e{ftb_team_members}",
        "&7Your Role: &9&lMODERATOR",
        "&7Rank Weight: &e{ftb_rank_weight}"
      ]
    },
    {
      "priority": 100,
      "conditionType": "placeholder",
      "condition": "ftb_has_team:true",
      "header": [
        "&a&l═══════════════════════════",
        "&2&lTEAM MEMBER &7| &f{player_name}",
        "&7Team: &b{ftb_team_display_name}",
        "&7Rank: &a{ftb_rank_display_name}",
        "&a&l═══════════════════════════"
      ],
      "footer": [
        "&7Team Members: &e{ftb_team_members}",
        "&7Your Role: &a&lMEMBER",
        "&7Rank Weight: &e{ftb_rank_weight}"
      ]
    },
    {
      "priority": 50,
      "conditionType": "placeholder",
      "condition": "ftb_has_rank:true",
      "header": [
        "&f&l═══════════════════════════",
        "&7&lRANKED PLAYER &7| &f{player_name}",
        "&7Rank: &a{ftb_rank_display_name}",
        "&f&l═══════════════════════════"
      ],
      "footer": [
        "&7Your Rank: &a{ftb_rank_display_name}",
        "&7Rank Weight: &e{ftb_rank_weight}",
        "&7Permissions: &e{ftb_rank_permissions}"
      ]
    },
    {
      "priority": 1,
      "conditionType": "default",
      "header": [
        "&7&l═══════════════════════════",
        "&8&lDEFAULT PLAYER &7| &f{player_name}",
        "&7&l═══════════════════════════"
      ],
      "footer": [
        "&7No team or rank assigned",
        "&7Join a team to get started!"
      ]
    }
  ],
  "playerOrder": [
    { "placeholder": "ftb_rank_weight", "direction": "desc", "asNumber": true },
    { "placeholder": "ftb_team_role", "direction": "desc" },
    { "placeholder": "ping", "direction": "asc", "asNumber": true }
  ]
}
```

## Name Format Examples

The NameFormatManager automatically integrates FTB prefixes and suffixes:

### Chat Format
```
{ftb_combined_prefix}[{team_name}] {player_name}{ftb_combined_suffix}: {message}
```

### Display Name Format
```
{ftb_rank_color}{ftb_combined_prefix}{player_name}{ftb_combined_suffix}
```

## Usage in Commands

You can use these placeholders in any NeoEssentials command that supports placeholders:

```
/broadcast Welcome {ftb_combined_prefix}{player_name}{ftb_combined_suffix} from team {ftb_team_display_name}!
```

## Automatic Integration

The FTB integration works automatically when FTB Teams, Ranks, or Library mods are detected. The integration includes:

1. **Automatic Prefix/Suffix Combination**: The system intelligently combines FTB rank and team prefixes/suffixes
2. **Performance Caching**: Team and rank data is cached for 30 seconds to avoid API overhead
3. **Safe Fallback**: If FTB mods are not available, placeholders return empty strings
4. **Permission Integration**: Works alongside NeoEssentials' existing permission system

## Configuration Notes

- Use `placeholder` condition type to check for specific FTB values
- The `ftb_has_team` and `ftb_has_rank` placeholders are useful for conditional layouts
- Team roles are automatically detected (Owner, Moderator, Member)
- Rank weights are used for player ordering in tablist
- All FTB placeholders return empty strings if the player has no team/rank
