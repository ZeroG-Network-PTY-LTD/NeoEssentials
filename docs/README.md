# NeoEssentials Documentation

This directory contains comprehensive documentation for the NeoEssentials mod. The documentation is organized into several sections to help users find the information they need.

## Directory Structure

- **`/wiki/`** - Content formatted for the GitHub wiki and platform descriptions
- **`/images/`** - Images used in documentation
- **`/Releases/`** - Version changelogs for all platforms (CurseForge, Modrinth, GitHub)

## Platform Descriptions

Professional mod descriptions for various platforms:

- [`/wiki/CurseForge-Description.md`](wiki/CurseForge-Description.md) - Professional mod description for CurseForge
- [`/wiki/Modrinth-Description.md`](wiki/Modrinth-Description.md) - Professional mod description for Modrinth

## Release Changelogs

Comprehensive release documentation for all platforms:

- **[Releases Directory](Releases/)** - Complete changelog history
  - [`/Releases/v1.0.0/`](Releases/v1.0.0/) - Initial release changelogs (v1.0.0)
  - [`/Releases/v1.0.1/`](Releases/v1.0.1/) - Major tablist overhaul changelogs (v1.0.1) 
  - [`/Releases/v1.0.2/`](Releases/v1.0.2/) - Future release (will include features from development builds v1.0.1.67-91+)

## Wiki Documentation

Comprehensive wiki files available for GitHub Pages:

- [`Home.md`](wiki/Home.md) - Main wiki homepage
- [`Installation-Guide.md`](wiki/Installation-Guide.md) - Complete installation instructions
- [`Configuration-Guide.md`](wiki/Configuration-Guide.md) - Detailed configuration guide
- [`Commands-Reference.md`](wiki/Commands-Reference.md) - Complete command reference
- [`Troubleshooting.md`](wiki/Troubleshooting.md) - Common issues and solutions
- [`Frequently-Asked-Questions.md`](wiki/Frequently-Asked-Questions.md) - FAQ section
- [`Performance-Optimization.md`](wiki/Performance-Optimization.md) - Performance tuning guide
- [`Text-Formatting.md`](wiki/Text-Formatting.md) - Color and text formatting guide
- [`Animation-System.md`](wiki/Animation-System.md) - Advanced animation system guide
- [`Tablist-System.md`](wiki/Tablist-System.md) - Comprehensive tablist documentation
- [`YML-Configuration.md`](wiki/YML-Configuration.md) - YAML configuration guide
- [`JSON-Templates.md`](wiki/JSON-Templates.md) - JSON template system guide
- [`Changelog.md`](wiki/Changelog.md) - Version history and changes
- [`TABLIST_JSON_MIGRATION.md`](TABLIST_JSON_MIGRATION.md) - Guide for migrating from TOML to JSON/YML
- [`SERVER_DEPLOYMENT_GUIDE.md`](SERVER_DEPLOYMENT_GUIDE.md) - Guide for deploying on a server
- [`SERVER_OPERATORS_GUIDE.md`](SERVER_OPERATORS_GUIDE.md) - Guide for server operators

## Wiki Content

The `/wiki/` directory contains content formatted for the GitHub wiki:

- [`/wiki/Home.md`](wiki/Home.md) - Wiki home page
- [`/wiki/Home-Advanced.md`](wiki/Home-Advanced.md) - Advanced wiki home page with detailed navigation

## Platform-Specific READMEs

- [`/platforms/README_CURSEFORGE.md`](platforms/README_CURSEFORGE.md) - CurseForge description
- [`/platforms/README_MODRINTH.md`](platforms/README_MODRINTH.md) - Modrinth description
- [`/platforms/README_CURSEFORGE_ADVANCED.md`](platforms/README_CURSEFORGE_ADVANCED.md) - Enhanced CurseForge description

## Contributing to Documentation

When contributing to the documentation:

1. Follow the existing format and style
2. Include practical examples where appropriate
3. Use proper Markdown formatting
4. Place images in the `/images/` directory
5. Update this README when adding new documentation files

## Documentation Standards

All documentation should:

- Be clear and concise
- Include examples where appropriate
- Use proper headings and structure
- Include tables for data where appropriate
- Link to related documentation

## Generating Documentation

The documentation can be built into various formats using:

```bash
# Generate HTML documentation
./gradlew generateHtmlDocs

# Generate PDF documentation
./gradlew generatePdfDocs
```

## License

This documentation is licensed under the same license as the NeoEssentials mod.

## Recent Development Status

### Phase 3 Complete - Interactive GUI Systems (Build 1.0.2.125)

**Status:** ✅ **COMPLETE** - All GUI systems modernized and fully functional

**Major Achievements:**
- **Interactive GUI Systems:** Complete modernization of TeleportHistoryGUI, PlayerSettingsGUI, and BookmarkGUI using MenuSystem framework
- **Enhanced Commands:** Added `/tpbookmark gui`, `/playersettings gui`, and comprehensive world management commands
- **World Management:** New WorldManagementCommands with `/worldinfo`, `/dimensions`, and dimension teleportation
- **Data Persistence:** Complete integration of teleport history and player settings with DataManager
- **Build Stability:** 5 consecutive successful builds with zero compilation errors

**Key Documentation:**
- [`PHASE_3_STATUS_v1.0.2.125.md`](PHASE_3_STATUS_v1.0.2.125.md) - Complete Phase 3 status report
- [`INTERACTIVE_GUI_GUIDE.md`](INTERACTIVE_GUI_GUIDE.md) - User guide for new GUI systems
- [`PRODUCTION_DEPLOYMENT_GUIDE_v1.0.2.md`](PRODUCTION_DEPLOYMENT_GUIDE_v1.0.2.md) - Admin deployment guide

**Next Phase:** In-game testing and Phase 4 planning (performance optimization and additional utilities)
