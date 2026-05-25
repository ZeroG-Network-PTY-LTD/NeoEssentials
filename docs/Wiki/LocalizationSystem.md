# NeoEssentials — Localization System

> **Added / overhauled in build.62 · Language config setting added in build.187**

---

## Overview

NeoEssentials ships a fully featured, server-side localization system. Every in-game message is driven by translation keys stored in language JSON files. Server admins can:

- **Set the active language** with one line in `config.json` — no file renaming needed.
- **Override individual messages** without editing any bundled file.
- **Add or swap entire languages** by dropping a `.json` file in the custom language directory.
- **Validate coverage** of any language file against the English base.
- **Regenerate** a language file from the JAR while preserving user edits.

All tooling is exposed through the `/language` in-game command (OP level 4).

---

## Choosing the Active Language

Set the server language in `config.json` under the `localization` section:

```json
"localization": {
  "language": "fr_fr"
}
```

Reload in-game: `/neoessentials reload`

**Available built-in language codes:**

| Code | Language |
|------|----------|
| `en_us` | English (US) — default |
| `fr_fr` | French |
| `de_de` | German |
| `es_es` | Spanish |
| `pt_br` | Portuguese (Brazil) |
| `zh_cn` | Chinese (Simplified) |
| `nl_nl` | Dutch |
| `pl_pl` | Polish |
| `ru_ru` | Russian |

> **Fallback:** Any key not present in the configured language file automatically falls back to `en_us`. You will never see a raw translation key in-game.

---

## File Locations

| File | Purpose |
|------|---------|
| `neoessentials/languages/custom/<language>.json` | Active language file — auto-deployed from JAR when first selected, auto-merged on updates (e.g. `fr_fr.json` when `"language": "fr_fr"`) |
| `neoessentials/languages/custom/en_us.json` | Fallback English file (always present) |
| `neoessentials/languages/custom/<code>.json` | Any additional custom / community language files |
| `neoessentials/languages/overrides.json` | Admin message overrides — take top priority over everything else |
| `neoessentials/languages/templates/<code>_template.json` | Auto-generated templates for translators |

---

## How the System Works

### Startup / Boot

1. `ConfigManager.getServerLanguage()` reads `localization.language` from `config.json` (default: `en_us`).
2. `MessageUtil.loadTranslations()` runs and resolves the configured language code.
3. The language file is looked for at `neoessentials/languages/custom/<code>.json`.
4. If not found (first run), it is deployed from the JAR's bundled `<code>.json`.
5. If the `fr_fr` (or other) JAR file does not cover 100% of keys, missing keys are filled from `en_us` as fallback.
6. If the deployed file's `_langVersion` is **older** than the current JAR version, **new keys are merged in** without overwriting existing values.
7. `overrides.json` is loaded. Overrides take top priority over all files.

### Key Resolution Order (highest → lowest priority)

```
1. Admin overrides  (overrides.json)
2. Configured language file  (e.g. fr_fr.json when "language": "fr_fr")
3. en_us fallback  (for keys not covered by the configured language)
4. Human-readable fallback  (generated from the key name)
```

### Human-Readable Fallback

If a key is not found in any file, instead of showing the raw key string (`commands.neoessentials.home.not_found`), the system strips common prefixes and converts dots/underscores to spaces:

```
commands.neoessentials.home.not_found → "Home not found"
neoessentials.moderation.player_only_command → "Moderation player only command"
```

This ensures players always see readable English even if a key is missing.

---

## Version Tracking

Each language file contains a `_langVersion` metadata key:

```json
{
  "_langVersion": "16",
  ...
}
```

`MessageUtil.java` maintains a constant `CURRENT_LANG_VERSION`. On startup, if the deployed file's version is lower, new keys are merged automatically. The file version is then bumped to the current value and saved.

**Current version: 16** (as of build.187)

When you add new translation keys to `en_us.json`, increment `_langVersion` by 1 in both:
- `src/main/resources/data/lang/en_us.json` (the `_langVersion` value)
- `MessageUtil.java` → `CURRENT_LANG_VERSION` constant

---

## `/language` Command Reference

All subcommands require OP level 4.

### Information & Management

| Command | Description |
|---------|-------------|
| `/language list` | List all custom language files loaded |
| `/language reload` | Reload all language files and overrides from disk |
| `/language stats` | Show statistics (loaded languages, missing keys tracked, overrides) |
| `/language info` | Show full command reference |

### Templates & Missing Keys

| Command | Description |
|---------|-------------|
| `/language template <code>` | Generate a translator-ready template for language `<code>` |
| `/language exportmissing` | Export all runtime-tracked missing keys to a JSON template file |
| `/language clearmissing` | Reset the missing-keys tracker |

### Validation & Regeneration

| Command | Description |
|---------|-------------|
| `/language validate <code>` | Compare `<code>.json` against `en_us.json`; shows coverage %, missing keys, and extra keys |
| `/language regenerate <code>` | Re-deploy `<code>.json` from JAR, merging user translations, auto-backup to `.bak` |

**`/language validate` example output:**
```
═══ Language Validation: fr_fr ═══
Total base keys: 1058
Translated keys: 987
Coverage: §e93%
Missing keys: 71
Extra keys (not in base): 3
First 10 missing key(s):
  - commands.neoessentials.teleport.request.sent
  - commands.neoessentials.teleport.request.received
  ...
Run /language regenerate fr_fr to update the file from JAR.
```

### Admin Overrides

Override any message key permanently. Overrides are saved to `overrides.json` and survive reloads.

| Command | Description |
|---------|-------------|
| `/language override set <key> <value>` | Set a message override |
| `/language override get <key>` | Show the current override for a key |
| `/language override remove <key>` | Remove a single override |
| `/language override list` | List all active overrides |
| `/language override clear` | Remove all overrides |
| `/language override reload` | Reload overrides from disk |

**Example — customize the server's welcome message:**
```
/language override set neoessentials.server.welcome §aWelcome to MyServer! Type /help to get started.
```

---

## Creating a Custom Language

1. Generate a template:
   ```
   /language template zh_tw
   ```
   This creates `neoessentials/languages/templates/zh_tw_template.json`.

2. Translate all values in the template (replace `[TRANSLATE] English text` with your translation). Keep `{0}`, `{1}` placeholders exactly as-is — they are substituted at runtime with player names, counts, etc.

3. Add metadata at the top of your file:
   ```json
   {
     "_nativeName": "繁體中文",
     "_englishName": "Traditional Chinese",
     "_languageCode": "zh_tw",
     "_author": "YourName",
     "_version": "1.0",
     ...
   }
   ```

4. Save the file as `neoessentials/languages/custom/zh_tw.json`.

5. Set it active in `config.json`:
   ```json
   "localization": {
     "language": "zh_tw"
   }
   ```

6. Run `/neoessentials reload`.

7. Verify coverage with `/language validate zh_tw`.

---

## Bundled Language Files

The following languages are bundled in the JAR and can be activated via `config.json`:

| Code | Language |
|------|----------|
| `en_us` | English (base) |
| `fr_fr` | French |
| `de_de` | German |
| `es_es` | Spanish |
| `pt_br` | Portuguese (Brazil) |
| `zh_cn` | Chinese (Simplified) |
| `nl_nl` | Dutch |
| `pl_pl` | Polish |
| `ru_ru` | Russian |

---

## Developer Notes

### Adding a New Translation Key

1. Add the key + English value to `src/main/resources/data/lang/en_us.json`.
2. Use the key via `MessageUtil.localize("your.new.key", arg0, arg1...)` in Java.
3. Increment `_langVersion` in `en_us.json` and `CURRENT_LANG_VERSION` in `MessageUtil.java`.
4. On the next server start, the new key is automatically merged into all deployed language files.

### Key Naming Convention

```
commands.neoessentials.<category>.<action>    (command feedback messages)
neoessentials.<category>.<action>             (system / non-command messages)
```

Examples:
- `commands.neoessentials.teleport.home.not_found`
- `commands.neoessentials.moderation.ban.success`
- `neoessentials.moderation.ban_message`
- `neoessentials.error.no_server`

### `MessageUtil` API

```java
// Simple lookup with args
String msg = MessageUtil.localize("commands.neoessentials.home.not_found", homeName);

// With explicit English fallback (use when you're certain of the English text)
String msg = MessageUtil.localize("commands.neoessentials.home.not_found", "Home ''{0}'' not found.", homeName);

// As a Component (for sendSuccess / sendFailure)
Component comp = MessageUtil.component("commands.neoessentials.home.not_found", homeName);

// Convenience coloured components
Component success = MessageUtil.success("commands.neoessentials.home.set", name);
Component error   = MessageUtil.error("commands.neoessentials.home.not_found", name);
Component warning = MessageUtil.warning("commands.neoessentials.home.limit_reached", max);
Component info    = MessageUtil.info("commands.neoessentials.home.list_header", count);
```

---

## Troubleshooting

| Problem | Fix |
|---------|-----|
| Language not changing after config edit | Run `/neoessentials reload` — language is re-read on every reload. |
| Message shows raw key (e.g. `commands.neoessentials.xyz`) | The key is missing from `en_us.json`. Add it and bump `_langVersion`. |
| Message shows human-readable fallback instead of proper text | Same as above — key is missing. The humanizer is a safety net, not the final output. |
| Bundled language file not updating | Run `/language regenerate <code>`. |
| Override not showing in-game | Run `/language override reload` or `/language reload` after editing `overrides.json` manually. |
| Language file corrupted | Delete it and run `/language regenerate <code>` to redeploy from JAR. |
| Language falls back to English for some messages | That key isn't translated in the language file yet. Run `/language validate <code>` to see missing keys. |

---

*Last updated: build.190 — 2026-05-25*
