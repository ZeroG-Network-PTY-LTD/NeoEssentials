#!/bin/bash
# Cleanup script to remove multi-version structure and revert to single NeoForge 1.21.1 version

echo "Starting cleanup of multi-version structure..."

# Remove multi-module directories
rm -rf common
rm -rf neoforge-1.20.5
rm -rf neoforge-1.20.1
rm -rf forge-1.20.1
rm -rf forge-1.19.4
rm -rf neoforge-1.21.1

# Remove multi-module build files
rm -f build.gradle.multi

# Revert the settings.gradle file
cat > settings.gradle << 'EOL'
pluginManagement {
    repositories {
        maven { url = 'https://maven.neoforged.net/releases' }
        gradlePluginPortal()
    }
}

plugins {
    id 'org.gradle.toolchains.foojay-resolver-convention' version '0.9.0'
}

rootProject.name = 'NeoEssentials'
EOL

echo "Removed multi-module structure"

# Remove multi-version documentation
rm -f multiversion-progress.md
rm -f MULTIVERSION_GUIDE.md

echo "Removed multi-version documentation"

# Update migration files to remove multi-version tasks
cat > MIGRATION_TASKS.md << 'EOL'
# NeoEssentials Migration Tasks

This document outlines the remaining tasks for NeoEssentials development.

## Priority Tasks

1. **Core Functionality**
   - [ ] Complete command implementations
   - [ ] Finalize permissions system
   - [ ] Complete storage system

2. **Features**
   - [ ] Economy system
   - [ ] Teleportation commands
   - [ ] Home and warp system
   - [ ] Kit system

## Secondary Tasks

1. **UI Improvements**
   - [ ] Admin panel
   - [ ] User interfaces for common features
   - [ ] Menu system

2. **Documentation**
   - [ ] Complete user documentation
   - [ ] Server admin guides
   - [ ] Command reference

## Final Steps

1. **Testing**
   - [ ] Test all commands
   - [ ] Test permissions system
   - [ ] Test database systems

2. **Release Preparation**
   - [ ] Create build scripts
   - [ ] Prepare release notes
EOL

echo "Updated migration tasks"

# Update MIGRATION_SUMMARY.md to remove multi-version references
cat > MIGRATION_SUMMARY.md << 'EOL'
# NeoEssentials Migration Summary

NeoEssentials is focused on NeoForge 1.21.1 compatibility.

## Completed Tasks

1. **Core Implementation**
   - Implemented command system
   - Created permissions integration
   - Implemented storage system
   - Set up configuration system

2. **Feature Development**
   - Added teleportation commands
   - Implemented home system
   - Added warp functionality
   - Created kit system

## Current Status

The mod is currently being developed for NeoForge 1.21.1 with compatibility planned for future 1.21.x versions through NeoForge's compatibility versioning.

## Next Steps

1. Continue implementing additional features and commands
2. Complete documentation
3. Extensive testing
4. Prepare for initial release
EOL

echo "Updated migration summary"

echo "Cleanup completed successfully!"
