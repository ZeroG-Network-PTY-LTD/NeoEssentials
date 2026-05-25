# Custom Languages — Quick Reference

> Full documentation: [LocalizationSystem Wiki](Wiki/LocalizationSystem.md)

NeoEssentials ships 9 built-in languages. Switch languages with a single config line — no file renaming or copying needed.

## Setting the Server Language

In `config/neoessentials/main.json` (or `config.json` in monolithic mode):

```json
"localization": {
  "language": "fr_fr"
}
```

Then reload in-game: `/neoessentials reload`

## Built-in Language Codes

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

## File Location

Language files live at: `neoessentials/languages/custom/<code>.json`

The active language file is auto-deployed from the JAR on first use. Any missing keys fall back to English automatically.

## Overriding Individual Messages

```
/language override set neoessentials.server.welcome §aWelcome to MyServer!
```

Overrides are stored in `neoessentials/languages/overrides.json` and survive mod updates.

## Creating a New Language

```
/language template xx_xx          — generates a template
# translate neoessentials/languages/templates/xx_xx_template.json
# copy to neoessentials/languages/custom/xx_xx.json
# set "language": "xx_xx" in config
/neoessentials reload
/language validate xx_xx           — check coverage
```

## Useful Commands

| Command | Description |
|---------|-------------|
| `/language list` | List loaded custom language files |
| `/language validate <code>` | Check translation coverage vs English |
| `/language regenerate <code>` | Re-deploy language file from JAR (preserves edits) |
| `/language reload` | Reload all language files from disk |
| `/language stats` | Show language system statistics |

---

*See [LocalizationSystem.md](Wiki/LocalizationSystem.md) for the complete developer and admin guide.*

