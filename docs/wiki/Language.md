# Language System

NeoEssentials includes comprehensive multi-language support, allowing server administrators to provide localized experiences for players from different regions. The language system supports dynamic switching, custom translations, and regional formatting.

## 🌍 Supported Languages

### Built-in Language Support
NeoEssentials comes with built-in support for major languages:

<<<<<<< HEAD
#### **Fully Supported Languages** ✅
- **English (en_US)** - Default language, 100% complete ✅
- **Spanish (es_ES)** - Complete translation ✅
- **French (fr_FR)** - Complete translation ✅
- **German (de_DE)** - Complete translation ✅
- **Italian (it_IT)** - Complete translation ✅
- **Portuguese (pt_BR)** - Brazilian Portuguese ✅
- **Russian (ru_RU)** - Complete translation ✅
- **Japanese (ja_JP)** - Complete translation ✅
- **Korean (ko_KR)** - Complete translation ✅
- **Chinese Simplified (zh_CN)** - Complete translation ✅
- **Chinese Traditional (zh_TW)** - Complete translation ✅
- **Dutch (nl_NL)** - Complete translation ✅

### Language File Format

NeoEssentials uses Properties file format for language files:

#### **Properties Format** (`.properties`)
```properties
# Example: en_US.properties
general.prefix=&6[NeoEssentials]&r
general.no_permission=&cYou don't have permission to do that!
command.language.changed=&aLanguage changed to {LANGUAGE}!
home.set=&aHome '{HOME}' set at your current location!
command.heal.self=&aYou have been healed!
command.feed.self=&aYou have been fed!
```
=======
- **English (en_US)** - Default language, fully supported
- **Spanish (es_ES)** - Complete translation
- **French (fr_FR)** - Complete translation  
- **German (de_DE)** - Complete translation
- **Italian (it_IT)** - Complete translation
- **Portuguese (pt_BR)** - Brazilian Portuguese
- **Russian (ru_RU)** - Complete translation
- **Chinese Simplified (zh_CN)** - Complete translation
- **Chinese Traditional (zh_TW)** - Complete translation
- **Japanese (ja_JP)** - Complete translation
- **Korean (ko_KR)** - Complete translation
- **Dutch (nl_NL)** - Complete translation

### Regional Variants
Some languages include regional variants:

- **English**: en_US (American), en_GB (British), en_AU (Australian)
- **Spanish**: es_ES (Spain), es_MX (Mexico), es_AR (Argentina)
- **Portuguese**: pt_BR (Brazil), pt_PT (Portugal)
- **Chinese**: zh_CN (Simplified), zh_TW (Traditional)
>>>>>>> parent of 482ed14 (Implement SignShopData class for persistent storage of sign shop data, including serialization to/from JSON. Added BlockPosData and ItemStackData inner classes for handling position and item stack information.)

## 🎯 Language Management Commands

### Player Language Commands
```bash
/language                       # Show current language
/language list                  # List available languages
/language set <code>            # Set your language
/lang <code>                    # Alias for language set
/language info                  # Show language information
/language help                  # Language system help
```

**Examples:**
```bash
/language set es_ES             # Switch to Spanish
/lang fr_FR                     # Switch to French
/language set zh_CN             # Switch to Chinese Simplified
```

### Admin Language Commands
```bash
<<<<<<< HEAD
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

#### `/language stats` - Language Usage Statistics
```bash
/language stats                 # Show language system statistics
=======
/language admin                 # Open language admin panel
/language reload                # Reload language files
/language stats                 # Language usage statistics
/language validate              # Validate language files
/language export <lang>         # Export language file
/language import <file>         # Import language file
>>>>>>> parent of 482ed14 (Implement SignShopData class for persistent storage of sign shop data, including serialization to/from JSON. Added BlockPosData and ItemStackData inner classes for handling position and item stack information.)
```
**Permission**: `neoessentials.language.admin`

## 🔧 Language Configuration

### Language File Structure
Language files are stored in `config/neoessentials/languages/`:

```
config/neoessentials/languages/
├── en_US.properties            # English (Default)
├── es_ES.properties            # Spanish
├── fr_FR.properties            # French
├── de_DE.properties            # German
├── it_IT.properties            # Italian
├── pt_BR.properties            # Portuguese (Brazil)
├── ru_RU.properties            # Russian
├── ja_JP.properties            # Japanese
├── ko_KR.properties            # Korean
├── zh_CN.properties            # Chinese (Simplified)
├── zh_TW.properties            # Chinese (Traditional)
└── nl_NL.properties            # Dutch
```

## 📝 Translation Files

### Base Translation Structure
Example from `en_US.properties`:

```properties
# General Messages
general.prefix=&8[&6NeoEssentials&8]&r
general.no_permission=&cYou don't have permission to use this command.
general.player_not_found=&cPlayer '{player}' not found.
general.invalid_number=&cInvalid number: {input}
general.command_disabled=&cThis command is currently disabled.

# Command Messages
command.heal.self=&aYou have been healed!
command.heal.other=&a{player} has been healed!
command.heal.broadcast=&7{healer} healed {player}

command.feed.self=&aYou have been fed!
command.feed.other=&a{player} has been fed!

command.teleport.success=&aTeleported to {location}
command.teleport.unsafe=&cUnsafe teleportation location!
command.teleport.cooldown=&cYou must wait {time} before teleporting again.

# Home System
home.set=&aHome '{home}' set at your current location!
home.deleted=&aHome '{home}' has been deleted.
home.teleported=&aTeleported to home '{home}'.
home.not_found=&cHome '{home}' not found.
home.max_homes=&cYou have reached the maximum number of homes ({max}).

# Economy Messages
economy.balance=&eYour balance: &a${balance}
economy.paid=&aYou paid &e{player} &a${amount}.
economy.received=&aYou received &a${amount} &efrom {player}.
economy.insufficient_funds=&cInsufficient funds! You need ${amount} more.
```

### Custom Translation Creation
Create custom translations by copying and modifying base files:

```properties
# custom_en_US.properties - Server-specific customizations
command.heal.self=&6✨ You feel refreshed and renewed!
command.heal.broadcast=&7⚡ {healer} channeled healing energy to {player}

general.welcome=&eWelcome to our amazing server, {player}!
```

## 🛠️ Translation Management

### Adding New Languages
1. **Create Language File** - Copy `en_US.properties` to new language code
2. **Translate Messages** - Replace English text with translated versions
3. **Test Translations** - Use `/language test <key>` to verify translations
4. **Reload Languages** - Use `/language reload` to load new files

### Translation Workflow
1. **Base Translation** - Start with English template
2. **Community Translation** - Allow community contributions
3. **Quality Review** - Review and approve translations
4. **Testing** - Test translations in-game
5. **Deployment** - Deploy to production server

## 🔒 Permission Integration

### Language Permissions
Control language access with permissions:

```yaml
# Basic language permissions
neoessentials.language.use          # Use language commands
neoessentials.language.change       # Change own language
neoessentials.language.list         # List available languages

# Admin permissions
neoessentials.language.admin        # Language administration
neoessentials.language.reload       # Reload language files
neoessentials.language.test         # Test language keys
```

## 🔧 Troubleshooting

### Common Language Issues

#### Language Not Loading
- Check language file syntax (Properties format)
- Verify file encoding (UTF-8)
- Check file permissions
- Use `/language reload` to reload language system

#### Missing Translations
- Check for translation keys in base language file
- Verify placeholder syntax `{PLACEHOLDER}`
- Use fallback language (en_US) for missing keys
- Update translation files with missing keys

#### Character Display Issues
- Ensure UTF-8 encoding for language files
- Check client font support for special characters
- Test with different clients
- Verify color codes are properly formatted

### Debug Commands
```bash
/language info                  # Show language system information
/language test <key>            # Test specific translation key
/language stats                 # Show language usage statistics
/language reload                # Reload all language files
```

---

## 📚 Related Documentation

- **[Configuration](Configuration.md)** - General configuration setup
- **[Essential Commands](Essential-Commands.md)** - Available commands  
- **[Placeholders](Placeholders.md)** - Placeholder system
- **[Permissions](Permissions.md)** - Permission system setup

*Last Updated: August 9, 2025*
