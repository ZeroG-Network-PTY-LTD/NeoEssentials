# NeoEssentials Multi-Version Setup Guide

This guide explains how to work with the new multi-project setup for NeoEssentials, which supports multiple Minecraft versions and both NeoForge and Forge mod loaders.

## Project Structure

The project is now organized as a multi-project Gradle build with the following components:

- `common` - Contains shared code that is not specific to any Minecraft version or mod loader
- `neoforge-1.21.1` - The main NeoForge version for Minecraft 1.21.1
- `neoforge-1.20.5` - NeoForge version for Minecraft 1.20.5
- `neoforge-1.20.1` - NeoForge version for Minecraft 1.20.1
- `forge-1.20.1` - Forge version for Minecraft 1.20.1
- `forge-1.19.4` - Forge version for Minecraft 1.19.4

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

## Troubleshooting

If you encounter issues with the multi-project setup:

1. Check that all module names in `settings.gradle` match directory names
2. Ensure that Gradle wrapper version is compatible with all plugins
3. Verify that each module's `build.gradle` has the correct dependencies for its target version
4. Make sure the common module doesn't contain any version-specific code
