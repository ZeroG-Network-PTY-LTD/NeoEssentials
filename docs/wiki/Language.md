# Language System

NeoEssentials features a robust multi-language system that provides comprehensive internationalization support. Players can seamlessly switch between languages in-game, and server administrators can easily add custom translations and manage language preferences.

## 🌍 Supported Languages

### Built-in Language Support
NeoEssentials includes complete translations for major languages:

#### **Fully Supported Languages** ✅
- **English (en_US)** - Default language, 100% complete
- **German (de_DE)** - Complete translation with all features ✅
- **Spanish (es_ES)** - Complete translation
- **French (fr_FR)** - Complete translation  
- **Portuguese (pt_BR)** - Brazilian Portuguese
- **Russian (ru_RU)** - Complete translation
- **Chinese Simplified (zh_CN)** - Complete translation
- **Japanese (ja_JP)** - Complete translation

#### **Partially Supported Languages** 🔄
Languages with basic support that can be extended:
- **Chinese Traditional (zh_TW)**
- **Korean (ko_KR)**
- **Dutch (nl_NL)**
- **Italian (it_IT)**

### Language File Formats

NeoEssentials supports two language file formats:

#### **Properties Format** (`.properties`)
```properties
# Example: en_US.properties
general.prefix=&6[NeoEssentials]&r
general.no_permission=&cYou don't have permission to do that!
command.language.changed=&aLanguage changed to {LANGUAGE}!
home.set=&aHome '{HOME}' set at your current location!
```

#### **JSON Format** (`.json`)
```json
{
  "_comment": "NeoEssentials German Language File",
  "neoessentials.commands.error.no_permission": "Du hast keine Berechtigung, diesen Befehl zu verwenden!",
  "neoessentials.home.set": "Heimatort '%s' gesetzt!",
  "neoessentials.spawn.teleported": "Zum Spawn teleportiert!"
}
```

## 🎯 Language Management Commands

### Player Language Commands

#### `/language` - Show Language Information
```bash
/language                       # Show current language and available options
```
**Output Example:**
```
=== Language Information ===
Current Language: English (en_US)
Available Languages: 8
Use '/language set <language>' to change your language
Use '/language list' to see all available languages
```

#### `/language list` - List Available Languages
```bash
/language list                  # List all available languages
```
**Output Example:**
```
=== Available Languages ===
► English (en_US) <- Current
- Deutsch (de_DE)
- Español (es_ES)
- Français (fr_FR)
- Português (pt_BR)
- Русский (ru_RU)
- 中文 (zh_CN)
- 日本語 (ja_JP)
```

#### `/language set <code>` - Change Language
```bash
/language set <language_code>   # Set your language
/lang <language_code>           # Alias for language set
```

**Examples:**
```bash
/language set de_de             # Switch to German  
/lang es_es                     # Switch to Spanish
/language set fr_fr             # Switch to French
/language set zh_cn             # Switch to Chinese Simplified
/lang en_us                     # Switch back to English
```

#### `/language info` - Detailed Language Information
```bash
/language info                  # Show detailed language system information
```
**Output Example:**
```
=== Language System Information ===
Available Languages: 8
Default Language: en_US
Players with Custom Locales: 25
Current Locale: de_DE (Deutsch)
```

### Admin Language Commands

#### `/language reload` - Reload Language Files
```bash
/language reload                # Reload all language files from disk
```
**Permission**: `neoessentials.language.reload`

#### `/language test <key>` - Test Language Keys
```bash
/language test <message_key>    # Test a specific language key across languages
```
**Examples:**
```bash
/language test general.no_permission
/language test home.set
/language test command.language.changed
```

**Output Example:**
```
Testing language key: general.no_permission
Your language (de_DE): Du hast keine Berechtigung, das zu tun!
Default (en_US): You don't have permission to do that!
Other languages:
- es_ES: ¡No tienes permiso para hacer eso!
- fr_FR: Vous n'avez pas la permission de faire cela!
```
/language admin                 # Open language admin panel
/language reload                # Reload language files
/language stats                 # Language usage statistics
/language validate              # Validate language files
/language export <lang>         # Export language file
/language import <file>         # Import language file
```

## 🔧 Language Configuration

### Server Language Settings
Configure in `config/neoessentials/language.toml`:

```toml
[language]
# Default server language
defaultLanguage = "en_US"

# Allow players to change their language
allowPlayerLanguageChange = true

# Auto-detect language from client locale
autoDetectLanguage = true

# Fallback language if translation missing
fallbackLanguage = "en_US"

[regional]
# Default timezone
defaultTimezone = "UTC"

# Default date format
dateFormat = "yyyy-MM-dd"

# Default time format  
timeFormat = "HH:mm:ss"

# Default number format
numberFormat = "#,##0.00"

# Default currency format
currencyFormat = "$#,##0.00"
```

### Language File Structure
Language files are stored in `config/neoessentials/language/`:

```
language/
├── en_US.yml                   # English (Default)
├── es_ES.yml                   # Spanish
├── fr_FR.yml                   # French
├── de_DE.yml                   # German
├── custom/                     # Custom translations
│   ├── custom_en.yml
│   └── custom_es.yml
└── overrides/                  # Override translations
    ├── server_en.yml
    └── server_es.yml
```

## 📝 Translation Files

### Base Translation Structure
Example from `en_US.yml`:

```yaml
# General Messages
general:
  prefix: "&8[&6NeoEssentials&8]&r"
  no_permission: "&cYou don't have permission to use this command."
  player_not_found: "&cPlayer '{player}' not found."
  invalid_number: "&cInvalid number: {input}"
  command_disabled: "&cThis command is currently disabled."

# Command Messages
commands:
  heal:
    self: "&aYou have been healed!"
    other: "&a{player} has been healed!"
    broadcast: "&7{healer} healed {player}"
  
  feed:
    self: "&aYou have been fed!"
    other: "&a{player} has been fed!"
    
  teleport:
    success: "&aTeleported to {location}"
    unsafe: "&cUnsafe teleportation location!"
    cooldown: "&cYou must wait {time} before teleporting again."

# GUI Messages
gui:
  shop:
    title: "Server Shop"
    purchase_success: "&aYou purchased {item} for {price}!"
    insufficient_funds: "&cInsufficient funds! Need {amount} more."
    
  kits:
    title: "Available Kits"
    claimed: "&aYou claimed the {kit} kit!"
    cooldown: "&cYou must wait {time} before claiming this kit again."

# Time and Date Formats
time:
  formats:
    short: "h:mm a"
    long: "h:mm:ss a z"
    date: "MMM d, yyyy"
    datetime: "MMM d, yyyy h:mm a"
    
  units:
    second: "second"
    seconds: "seconds"
    minute: "minute"
    minutes: "minutes"
    hour: "hour"
    hours: "hours"
    day: "day"
    days: "days"
```

### Custom Translation Creation
Create custom translations by copying and modifying base files:

```yaml
# custom/server_en.yml - Server-specific customizations
commands:
  heal:
    self: "&6✨ You feel refreshed and renewed!"
    broadcast: "&7⚡ {healer} channeled healing energy to {player}"

gui:
  shop:
    title: "&6🏪 Awesome Server Shop"
    welcome: "&eWelcome to our amazing shop, {player}!"
```

## 🎨 Regional Formatting

### Number and Currency Formatting
Different regions have different formatting conventions:

```yaml
# English (US)
regional:
  number_format: "#,##0.00"
  currency_format: "$#,##0.00"
  decimal_separator: "."
  thousands_separator: ","

# German
regional:
  number_format: "#.##0,00"
  currency_format: "#.##0,00 €"
  decimal_separator: ","
  thousands_separator: "."

# French
regional:
  number_format: "# ##0,00"
  currency_format: "# ##0,00 €"
  decimal_separator: ","
  thousands_separator: " "
```

### Date and Time Formatting
Regional date and time format variations:

```yaml
# US Format
time:
  date_format: "MM/dd/yyyy"
  time_format: "h:mm a"
  datetime_format: "MM/dd/yyyy h:mm a"

# European Format  
time:
  date_format: "dd/MM/yyyy"
  time_format: "HH:mm"
  datetime_format: "dd/MM/yyyy HH:mm"

# ISO Format
time:
  date_format: "yyyy-MM-dd"
  time_format: "HH:mm:ss"
  datetime_format: "yyyy-MM-dd HH:mm:ss"
```

## 🔧 Advanced Language Features

### Placeholder Translation
Placeholders can be automatically translated based on player language:

```yaml
placeholders:
  health_status:
    en_US: "Health: {health}/{max_health}"
    es_ES: "Salud: {health}/{max_health}"
    fr_FR: "Santé: {health}/{max_health}"
    de_DE: "Gesundheit: {health}/{max_health}"
```

### Conditional Messages
Messages can vary based on context:

```yaml
messages:
  welcome:
    first_time:
      en_US: "Welcome to the server, {player}! This is your first visit."
      es_ES: "¡Bienvenido al servidor, {player}! Esta es tu primera visita."
    returning:
      en_US: "Welcome back, {player}! You were last here {last_seen}."
      es_ES: "¡Bienvenido de vuelta, {player}! Estuviste aquí por última vez {last_seen}."
```

### Pluralization Support
Handle singular and plural forms correctly:

```yaml
plurals:
  item_count:
    en_US:
      zero: "no items"
      one: "1 item"
      other: "{count} items"
    es_ES:
      zero: "ningún artículo"
      one: "1 artículo"
      other: "{count} artículos"
```

## 🎮 GUI Language Integration

### Multi-language GUI Support
GUI elements automatically adapt to player language:

```json
{
  "gui": {
    "shop": {
      "title": {
        "en_US": "§6🏪 Server Shop",
        "es_ES": "§6🏪 Tienda del Servidor",
        "fr_FR": "§6🏪 Boutique du Serveur",
        "de_DE": "§6🏪 Server Shop"
      },
      "buttons": {
        "buy": {
          "en_US": "§aClick to Buy",
          "es_ES": "§aHaz clic para Comprar",
          "fr_FR": "§aCliquez pour Acheter",
          "de_DE": "§aKlicken zum Kaufen"
        }
      }
    }
  }
}
```

### Dynamic Language Switching
Players can change language and see immediate updates:

```bash
/language set fr_FR             # Switch to French
# GUI immediately updates to French text
/shop                           # Shop opens in French
```

## 📊 Language Analytics

### Usage Statistics
Track language usage across your server:

```bash
/language stats                 # Overall language statistics
/language usage <timeframe>     # Language usage over time
/language players <language>    # Players using specific language
```

**Statistics Include:**
- **Language Distribution** - Percentage of players per language
- **Geographic Distribution** - Languages by player location
- **Usage Trends** - Language adoption over time
- **Popular Translations** - Most requested language features

### Translation Quality
Monitor translation completeness and quality:

```bash
/language completeness          # Translation completeness report
/language missing <language>    # Missing translations for language
/language validate <language>   # Validate language file syntax
```

## 🛠️ Translation Management

### Translation Workflow
1. **Base Translation** - Start with English template
2. **Community Translation** - Allow community contributions
3. **Quality Review** - Review and approve translations
4. **Testing** - Test translations in-game
5. **Deployment** - Deploy to production server

### Translation Tools
```bash
/language extract               # Extract new strings for translation
/language merge <file>          # Merge translation updates
/language diff <lang1> <lang2>  # Compare language files
/language template <language>   # Generate translation template
```

### Community Contributions
Enable community members to contribute translations:

```toml
[language.community]
# Allow players to suggest translations
allowSuggestions = true

# Minimum permission level for suggestions
suggestionPermission = "vip"

# Review suggestions before applying
requireReview = true

# Discord webhook for translation notifications
discordWebhook = "your_webhook_url"
```

## 🔒 Permission Integration

### Language Permissions
Control language access with permissions:

```yaml
# Basic language permissions
neoessentials.language.use          # Use language commands
neoessentials.language.change       # Change own language
neoessentials.language.list         # List available languages

# Advanced permissions
neoessentials.language.admin        # Language administration
neoessentials.language.reload       # Reload language files
neoessentials.language.validate     # Validate translations

# Language-specific permissions
neoessentials.language.en_US        # Allow English
neoessentials.language.es_ES        # Allow Spanish
neoessentials.language.premium.*    # Allow premium languages
```

### Group-based Language Access
Restrict certain languages to specific groups:

```yaml
language_restrictions:
  vip_languages: ["en_US", "es_ES", "fr_FR"]
  premium_languages: ["ja_JP", "ko_KR", "zh_CN"]
  staff_languages: ["*"]  # All languages
```

## 🌐 Client Integration

### Automatic Language Detection
Automatically detect player language from client:

```toml
[language.detection]
# Use client locale as default
useClientLocale = true

# Fallback detection methods
fallbackMethods = ["ip_geolocation", "username_patterns"]

# Override with saved preference
respectSavedPreference = true
```

### Resource Pack Integration
Integrate with resource packs for complete localization:

```json
{
  "resource_pack": {
    "language_packs": {
      "en_US": "https://example.com/packs/en_US.zip",
      "es_ES": "https://example.com/packs/es_ES.zip"
    },
    "auto_download": true,
    "force_pack": false
  }
}
```

## 🔧 Troubleshooting

### Common Language Issues

#### Language Not Loading
- Check language file syntax (YAML format)
- Verify file encoding (UTF-8)
- Check file permissions
- Reload language system

#### Missing Translations
- Check for translation keys in base language
- Verify placeholder syntax
- Use fallback language for missing keys
- Update translation files

#### Character Display Issues
- Ensure UTF-8 encoding
- Check client font support
- Verify resource pack compatibility
- Test with different clients

### Debug Commands
```bash
/language debug <player>        # Debug player language settings
/language test <key> <language> # Test specific translation
/language encoding <file>       # Check file encoding
/language keys missing          # List missing translation keys
```

---

## 📚 Related Documentation

- **[Configuration](Configuration.md)** - Language system configuration
- **[Placeholders](Placeholders.md)** - Placeholder localization
- **[GUI System](GUI-System.md)** - Multi-language GUI setup
- **[Notifications](Notifications.md)** - Localized notifications

*Last Updated: August 6, 2025*
