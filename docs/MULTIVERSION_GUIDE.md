# NeoEssentials Multi-Version Setup Guide

This guide explains how to work with the new multi-project setup for NeoEssentials, which supports multiple Minecraft versions and both NeoForge and Forge mod loaders.

## Project Structure

The project is now organized as a multi-project Gradle build with the following components:

- `common` - Contains shared code that is not specific to any Minecraft version or mod loader
- `neoforge-1.21.1` - The main NeoForge version for Minecraft 1.21.1
- `forge-1.20.1` - Forge version for Minecraft 1.20.1
- `forge-1.21.1` - Forge version for Minecraft 1.21.1

## Getting Started

To get started with the multi-version setup, follow these steps:

1. Rename the original `build.gradle` to something like `build.gradle.original` to preserve it.
2. Rename `build.gradle.multi` to `build.gradle`.
3. Setup the project structure by running:

```bash
# Create source templates for each module
./gradlew createSourceTemplate --project-dir neoforge-1.20.5
./gradlew createSourceTemplate --project-dir neoforge-1.20.1
./gradlew createSourceTemplate --project-dir forge-1.20.1
./gradlew createSourceTemplate --project-dir forge-1.19.4

# Import the existing mod code into the 1.21.1 module
./gradlew importExistingMod --project-dir neoforge-1.21.1
```

## Working with the Common Module

The `common` module should contain code that is shared across all versions, such as:

- Utility classes that don't depend on Minecraft or mod loader APIs
- Data models that are version-independent
- Constants and interfaces that are used across versions

When adding code to the common module, make sure it does not import any Minecraft or mod loader classes.

### Common Module Classes

The common module includes the following classes:

#### Utility Classes
- `CommonTimeUtil`: Provides time parsing, formatting, and conversion utilities
- `CommonTextUtil`: Offers text formatting and color code handling
- `CommonPermissionUtil`: Contains permission constants and caching utilities
- `StringToBooleanArgumentType`: A version-independent command argument type for boolean values

#### Data Classes
- `Location`: Simple location storage class (world name, x, y, z, yaw, pitch)
- `HomeData`: Data class for player home locations
- `UserData`: Core player data storage class
- `WarpData`: Data class for server warp locations

#### Configuration
- `CommonConfig`: Version-independent configuration settings
- `CommonDatabaseConfig`: Database connection settings

### Adding to the Common Module

When adding new code to the common module:

1. Avoid any Minecraft-specific imports or dependencies
2. Use standard Java libraries and included common dependencies
3. Create abstract base classes or interfaces for features requiring game-specific implementation
4. Use the `checkPlatformImports` task to verify no game-specific imports were accidentally included

### Using Common Classes in Version Modules

To use common classes in version-specific modules:

1. Add a dependency on the common module in your version module's `build.gradle`:
   ```gradle
   dependencies {
       implementation project(':common')
   }
   ```

2. Create adapter classes that bridge between common code and version-specific implementations
3. Extend common base classes with version-specific implementations

Example:
```java
// In a version-specific module
public class TimeUtil extends CommonTimeUtil {
    // Add Minecraft-specific methods or override common ones
}
```

## Version-Specific Development

Each version-specific module has its own source directory and resources. When working on a specific version:

1. Navigate to the module directory (e.g., `cd neoforge-1.21.1`)
2. Run Gradle tasks specific to that module (e.g., `../gradlew build`)
3. Adapt code from the main version to work with the specific Minecraft version

## Building All Versions

To build all versions at once, run:

```bash
./gradlew buildAll
```

This will compile all modules and increment the build number.

## Publishing

To publish all versions to your local Maven repository, run:

```bash
./gradlew publishAll
```

## Cross-Version Compatibility Tips

When adapting code for different Minecraft versions:

1. Move version-independent code to the common module
2. Create version-specific adapters for Minecraft APIs that change between versions
3. Use reflection or compile-time checks when necessary for API differences
4. Consider using abstraction layers to hide version-specific implementation details

## Forge vs. NeoForge Differences

Key differences between Forge and NeoForge to consider:

1. Package names: `net.minecraftforge` vs `net.neoforged.neoforge`
2. Event bus systems may have slightly different APIs
3. Registration mechanisms for game objects may differ

When porting from NeoForge to Forge, these differences should be addressed with appropriate adapter code.

## Adding More Versions

If you need to add support for more Minecraft versions:

1. Create a new module in `settings.gradle` following the existing pattern
2. Copy and adapt an existing build.gradle file to match the target version
3. Set up the source template using the `createSourceTemplate` task
4. Implement the version-specific code adapters

## Setting Up with MDK Files

Each version-specific module needs appropriate MDK (Mod Development Kit) files to build correctly. Follow these steps for each module:

### Downloading MDK Files

1. Download the appropriate MDK for each target version:
   - **NeoForge 1.21.1**: Download from [NeoForge website](https://neoforged.net/)
   - **NeoForge 1.20.5**: Download from [NeoForge website](https://neoforged.net/)
   - **NeoForge 1.20.1**: Download from [NeoForge website](https://neoforged.net/)
   - **Forge 1.20.1**: Download from [Forge website](https://files.minecraftforge.net/net/minecraftforge/forge/index_1.20.1.html)
   - **Forge 1.19.4**: Download from [Forge website](https://files.minecraftforge.net/net/minecraftforge/forge/index_1.19.4.html)

### MDK Download Instructions

Since you'll need the MDK files for each version, here are the specific download links and instructions:

#### NeoForge MDK Files

1. **NeoForge 1.21.1 MDK**:
   - Go to the [NeoForged website](https://neoforged.net/docs/getstarted/mod/)
   - Download the latest MDK for 1.21.1
   - Direct link (check for updated versions): https://maven.neoforged.net/releases/net/neoforged/neoforge/1.21.1-latest/neoforge-1.21.1-latest-mdk.zip

2. **NeoForge 1.20.5 MDK**:
   - Go to the [NeoForged website](https://neoforged.net/docs/getstarted/mod/)
   - Download the latest MDK for 1.20.5
   - Direct link (check for updated versions): https://maven.neoforged.net/releases/net/neoforged/neoforge/1.20.5-latest/neoforge-1.20.5-latest-mdk.zip

3. **NeoForge 1.20.1 MDK**:
   - Go to the [NeoForged website](https://neoforged.net/docs/getstarted/mod/)
   - Download the MDK for 1.20.1
   - Direct link (check for updated versions): https://maven.neoforged.net/releases/net/neoforged/neoforge/1.20.1-latest/neoforge-1.20.1-latest-mdk.zip

#### Forge MDK Files

1. **Forge 1.20.1 MDK**:
   - Go to [Forge Files](https://files.minecraftforge.net/net/minecraftforge/forge/index_1.20.1.html)
   - Find the recommended or latest version
   - Click "MDK" to download the Mod Development Kit
   - Direct link for recommended (example): https://maven.minecraftforge.net/net/minecraftforge/forge/1.20.1-47.2.0/forge-1.20.1-47.2.0-mdk.zip

2. **Forge 1.19.4 MDK**:
   - Go to [Forge Files](https://files.minecraftforge.net/net/minecraftforge/forge/index_1.19.4.html)
   - Find the recommended or latest version
   - Click "MDK" to download the Mod Development Kit
   - Direct link for recommended (example): https://maven.minecraftforge.net/net/minecraftforge/forge/1.19.4-45.1.0/forge-1.19.4-45.1.0-mdk.zip

### Using Command Line to Download

If you prefer using command line tools, you can use PowerShell or curl to download the MDK files:

```powershell
# PowerShell examples (replace URLs with the latest versions)
Invoke-WebRequest -Uri "https://maven.neoforged.net/releases/net/neoforged/neoforge/1.21.1-latest/neoforge-1.21.1-latest-mdk.zip" -OutFile "neoforge-1.21.1-mdk.zip"

# Using curl
curl -o neoforge-1.21.1-mdk.zip https://maven.neoforged.net/releases/net/neoforged/neoforge/1.21.1-latest/neoforge-1.21.1-latest-mdk.zip
```

### Extracting MDK Files

After downloading, extract each MDK to a temporary location:

```bash
# Windows
mkdir temp-mdk
tar -xf neoforge-1.21.1-mdk.zip -C temp-mdk
# OR use your preferred extraction tool (WinRAR, 7-Zip, etc.)

# Then examine the contents to understand the structure
```

Once extracted, you can copy the necessary configuration files, resource templates, and example code into your corresponding version-specific module as outlined in the "Integrating MDK Files" section.

### Integrating MDK Files

For each module, extract the downloaded MDK and copy the following essential files:

1. **Build files**:
   - Copy gradle-related files specific to that version (don't overwrite the root gradle files)
   - Ensure that the `gradle/wrapper` directory has the correct versions

2. **Example mod files**:
   - Extract and examine the example mod source structure
   - Note the required package structure and mod declaration files for that version

3. **Version-specific resources**:
   - Copy the `src/main/resources` structure that contains version-specific metadata files
   - For NeoForge: `META-INF/mods.toml`, `pack.mcmeta`, etc.
   - For Forge: `META-INF/mods.toml`, `pack.mcmeta`, etc.

### Adapting Build Scripts

For each version module, modify the build.gradle file to:

1. Include the correct plugin for that mod loader version:
   ```gradle
   // For NeoForge (newer versions)
   plugins {
       id 'net.neoforged.moddev' version '...'
   }
   
   // For Forge (older versions)
   plugins {
       id 'net.minecraftforge.gradle' version '...'
   }
   ```

2. Reference the correct Minecraft and mod loader versions:
   ```gradle
   minecraft {
       // Version-specific configuration
       mappings channel: 'official', version: '...'
   }
   ```

3. Add dependency on the common module:
   ```gradle
   dependencies {
       implementation project(':common')
       // Other version-specific dependencies
   }
   ```

### Example: NeoForge 1.21.1 Module Setup

After downloading the NeoForge 1.21.1 MDK:

1. Extract the MDK zip file to a temporary location
2. Copy the necessary resources and example files to your `neoforge-1.21.1` module
3. Update the module's build.gradle to include:
   - Correct NeoForge plugin and version
   - Dependency on the common module
   - Appropriate run configurations for testing

By using the MDK files as a reference, you ensure that each module has the correct structure and dependencies for its specific Minecraft version and mod loader.

## Troubleshooting

If you encounter issues with the multi-project setup:

1. Check that all module names in `settings.gradle` match directory names
2. Ensure that Gradle wrapper version is compatible with all plugins
3. Verify that each module's `build.gradle` has the correct dependencies for its target version
4. Make sure the common module doesn't contain any version-specific code
