# Language System

NeoEssentials includes comprehensive multi-language support, allowing server administrators to provide localized experiences for players from different regions. The language system supports dynamic switching, player-specific locale detection, placeholder replacement, and hot-reloading of language files.

## 🌍 Supported Languages

### Built-in Language Support
NeoEssentials comes with built-in JSON language files located in `src/main/resources/assets/neoessentials/lang/`:

#### **Fully Supported Languages** ✅
- **English (en_us)** - Default language, 100% complete ✅
- **Spanish (es_es)** - Complete translation ✅  
- **French (fr_fr)** - Complete translation ✅
- **German (de_de)** - Complete translation ✅
- **Italian (it_it)** - Complete translation ✅
- **Portuguese (pt_br)** - Brazilian Portuguese ✅
- **Russian (ru_ru)** - Complete translation ✅
- **Japanese (ja_jp)** - Complete translation ✅  
- **Chinese Simplified (zh_cn)** - Complete translation ✅

### Language File Structure

NeoEssentials uses JSON format for language files with automatic conversion to Properties format:

#### **JSON Format** (`.json`) - Primary Format
```json
{
  "neoessentials.language.info.header": "&6=== Language Information ===",
  "general.no_permission": "You don't have permission to use this command!",
  "command.language.changed": "Language changed to {0}!",
  "neoessentials.home.set": "Home '{0}' set at your current location!",
  "command.heal.self": "You have been healed!",
  "command.feed.self": "You have been fed!",
  "neoessentials.economy.balance": "Your balance: ${0}",
  "neoessentials.warp.no_permission_use": "You do not have permission to use this warp."
}

## 🎯 Language Management Commands

### Player Language Commands
```bash
/language                       # Show current language information and available options
/language set <code>            # Set your language to specified language code
/language list                  # List all available languages with current indicator
/language info                  # Show detailed language system information
```

**Examples:**
```bash
/language                       # Show current language: "English (US) (en_US)"
/language set es_ES             # Switch to Spanish (Spain)
/language set pt_BR             # Switch to Portuguese (Brazil)
/language set zh_CN             # Switch to Chinese Simplified
/language list                  # Show all available languages with ► indicator for current
```

### Administrative Language Commands
```bash
/language reload                # Reload all language files from disk
/language test <key>            # Test a specific language key across available languages
```

**Examples:**
```bash
/language reload                                    # Reload all language files
/language test general.no_permission               # Test permission message across languages
/language test neoessentials.economy.balance       # Test economy balance message
/language test command.language.changed            # Test language change confirmation
```

**Permission Requirements:**
- Basic commands: `neoessentials.moderation.basic` (or OP)
- Administrative commands: `neoessentials.admin.basic` (or OP)
- Reload functionality: `neoessentials.language.reload`

---

## 🏗️ Technical Architecture

### LanguageManager System
The language system is powered by a comprehensive `LanguageManager` singleton with the following features:

#### **Multi-Source Loading**
- **Resource Languages:** Built-in JSON files from `assets/neoessentials/lang/`
- **Custom Overrides:** Optional `.properties` files in `config/neoessentials/languages/`
- **Fallback System:** Automatic fallback to English (en_US) for missing translations
- **Hot Reloading:** Dynamic reloading without server restart

#### **Player-Specific Localization**
- **Automatic Detection:** Attempts to detect player's client language settings
- **Manual Override:** Players can set their preferred language via commands
- **Persistent Storage:** Language preferences cached per-player UUID
- **Real-time Switching:** Immediate language changes without reconnection required

#### **Advanced Features**
- **Placeholder Replacement:** Supports both indexed (`{0}`, `{1}`) and named (`{PLAYER}`, `{AMOUNT}`) placeholders
- **Fallback Generation:** Intelligent fallback message generation for missing keys
- **Missing Key Tracking:** Tracks requested keys that don't exist for admin review
- **Statistics & Analytics:** Comprehensive language usage statistics
- **Thread-Safe Operations:** Concurrent access safe for high-performance servers

### File Structure
```
config/neoessentials/languages/     # Custom language overrides (optional)
├── en_US.properties                # English override example
├── es_ES.properties                # Spanish override example
└── custom_lang.properties          # Custom language files

src/main/resources/assets/neoessentials/lang/    # Built-in languages (JSON format)
├── en_us.json                      # English (Default)
├── es_es.json                      # Spanish
├── fr_fr.json                      # French
├── de_de.json                      # German
├── it_it.json                      # Italian
├── pt_br.json                      # Portuguese (Brazil)
├── ru_ru.json                      # Russian
├── ja_jp.json                      # Japanese
└── zh_cn.json                      # Chinese (Simplified)
```

---

## 📝 Translation Files

### Base Translation Structure
Example from actual `en_us.json`:

```json
{
  "neoessentials.language.info.header": "&6=== Language Information ===",
  "general.no_permission": "You don't have permission to use this command!",
  "neoessentials.warp.no_permission_use": "You do not have permission to use this warp.",
  "neoessentials.playtime.session": "Session Playtime: {0}",
  "neoessentials.playtime.top_entry": "#{0}: {1} - {2}",
  "neoessentials.economy.balance": "Your balance: ${0}",
  "command.heal.self": "You have been healed!",
  "command.feed.self": "You have been fed!",
  "neoessentials.language.set.success_fallback": "&aLanguage set to {0}.",
  "neoessentials.language.list.header": "&6=== Available Languages ==="
}
```

### Custom Language Override Example
Create custom `.properties` files in `config/neoessentials/languages/`:

```properties
# custom_en_US.properties - Server-specific customizations
general.no_permission=&c⛔ Access denied! You lack the required permissions.
command.heal.self=&a✨ Your wounds have been miraculously healed!
neoessentials.economy.balance=&e💰 Your wallet contains: &a${0}
```

### Placeholder System
The language system supports comprehensive placeholder replacement:

#### **Indexed Placeholders**
```json
{
  "message.with.placeholders": "Player {0} paid {1} to {2}",
  "usage.example": "Usage: {0} <required> [optional]"
}
```

#### **Named Placeholders** (when using key-value pairs)
```json
{
  "player.welcome": "Welcome {PLAYER}! Your balance is {BALANCE}",
  "teleport.success": "Teleported to {LOCATION} in {WORLD}"
}
```

## 🛠️ Translation Management

### Adding New Languages
1. **Create JSON Language File** - Add new `.json` file in `src/main/resources/assets/neoessentials/lang/`
2. **Follow Naming Convention** - Use lowercase with underscores (e.g., `ko_kr.json`)
3. **Copy English Template** - Start with `en_us.json` as base template
4. **Translate Messages** - Replace English text with target language translations
5. **Test Translations** - Use `/language test <key>` to verify translations work
6. **Reload System** - Use `/language reload` to load new language files

### Custom Override System
1. **Create Override Directory** - Files in `config/neoessentials/languages/` override built-in translations
2. **Use Properties Format** - Override files use `.properties` format for easy editing
3. **Partial Overrides** - Only include keys you want to customize
4. **Priority System** - Config overrides take priority over built-in resources
5. **Hot Reload** - Changes take effect immediately with `/language reload`

### Translation Workflow
1. **Base Translation** - Start with English template from resources
2. **Community Contributions** - Allow community members to contribute translations
3. **Quality Review** - Review and approve community translations
4. **Testing Phase** - Test translations in-game with `/language test`
5. **Production Deployment** - Deploy approved translations to server

---

## 🔒 Permission Integration

### Language System Permissions
Control language access with specific permission nodes:

```yaml
# Basic Language Permissions
neoessentials.moderation.basic     # Access to basic /language commands
neoessentials.language.set         # Set own language preference  
neoessentials.language.list        # List available languages

# Administrative Permissions  
neoessentials.admin.basic          # Advanced language administration
neoessentials.language.reload      # Reload language files from disk
neoessentials.language.*           # All language system permissions
```

### Permission Requirements by Command
- `/language` - `neoessentials.moderation.basic` (or OP)
- `/language set <lang>` - `neoessentials.moderation.basic` (or OP)
- `/language list` - `neoessentials.moderation.basic` (or OP)  
- `/language info` - `neoessentials.moderation.basic` (or OP)
- `/language reload` - `neoessentials.admin.basic` (or OP)
- `/language test <key>` - `neoessentials.admin.basic` (or OP)

---

## 💡 Usage Examples

### Player Language Management
```bash
# Check current language settings
/language
# Output: "Current Language: English (US) (en_US)"

# Switch to Spanish
/language set es_ES  
# Output: "Idioma cambiado a Español (España)!"

# List all available languages
/language list
# Output: Shows all languages with ► indicator for current language
```

### Administrative Operations  
```bash
# Test a specific translation key
/language test neoessentials.economy.balance
# Output: Shows the message in your language and default language

# Reload all language files after making changes
/language reload
# Output: "Reloaded X language files"

# View detailed language system information
/language info  
# Output: Shows language statistics, player locale counts, etc.
```

## 🔧 Troubleshooting

### Common Language Issues

#### **Language Not Loading**
- **Check file format:** Ensure JSON files are valid (resources) or Properties files are properly formatted (config)
- **Verify file encoding:** All language files should use UTF-8 encoding
- **Check file permissions:** Ensure server has read access to language files
- **Use reload command:** Execute `/language reload` to refresh language system
- **Review console logs:** Check for LanguageManager errors in server console

#### **Missing Translations**
- **Fallback system active:** Missing keys automatically fall back to English (en_US)
- **Check key spelling:** Verify exact key names using `/language test <key>`
- **Review placeholder syntax:** Ensure placeholders use correct format `{0}`, `{1}`, etc.
- **Update language files:** Add missing keys to appropriate language files
- **Missing key tracking:** System tracks requested keys that don't exist for admin review

#### **Character Display Issues**
- **UTF-8 encoding required:** Ensure all language files use UTF-8 encoding
- **Client font support:** Verify player clients support special characters and Unicode
- **Color code formatting:** Check that Minecraft color codes (`&a`, `&c`, etc.) are properly formatted
- **Test with different clients:** Some clients may display characters differently

#### **Player Language Issues**
- **Language detection:** System attempts automatic client language detection (currently defaults to English)
- **Manual override:** Players can manually set language with `/language set <code>`
- **Persistent storage:** Language preferences are cached per-player UUID
- **Cache clearing:** Server restart clears language preference cache

### Debug Information
```bash
# Language system information
/language info                    # Show system stats and available languages
/language test <key>              # Test specific translation key across languages  
/language reload                  # Reload all language files and show count
```

### Performance Considerations
- **Resource loading:** Built-in JSON files loaded once at startup
- **Hot reloading:** Config overrides can be reloaded without restart
- **Caching system:** Player language preferences cached for optimal performance
- **Thread safety:** All language operations are thread-safe for concurrent access
- **Memory usage:** Language files stored in memory for fast access
- **Fallback efficiency:** Fallback system designed to minimize performance impact

### Advanced Configuration
- **Custom fallback language:** Modify `defaultLanguage` in LanguageManager for non-English servers
- **Debug mode:** Enable `neoessentials.debug.messages` system property for detailed missing key logging
- **Override priority:** Config files always take priority over built-in resource files
- **Placeholder optimization:** Placeholder replacement optimized for performance with minimal string operations

---

## 📚 Related Documentation

- **[Configuration Guide](Configuration.md)** - Main configuration settings and JSON structure
- **[Commands](Commands.md)** - Complete command reference including language commands
- **[Placeholders System](Placeholders.md)** - Custom placeholder system integration  
- **[Permissions System](Permissions.md)** - Permission node details and configuration
- **[API Documentation](API_DOCUMENTATION.md)** - Developer integration and language system API

*Documentation updated to reflect actual NeoEssentials 2.0.0 implementation*
