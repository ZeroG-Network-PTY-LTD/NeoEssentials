# NeoEssentials - Development

> This README is intended for developers contributing to NeoEssentials. For user documentation, see [README_RELEASE.md](README_RELEASE.md).

## Development Setup

### Prerequisites

- JDK 17 or newer
- Git
- IDE (IntelliJ IDEA or Eclipse recommended)
- Basic understanding of Minecraft modding with NeoForge

### Getting Started

1. Clone the repository:
```bash
git clone https://github.com/zerog/neoessentials.git
cd neoessentials
```

2. Setup workspace:
```bash
# For Windows
gradlew genIntellijRuns
# OR
gradlew genEclipseRuns

# For Linux/macOS
./gradlew genIntellijRuns
# OR
./gradlew genEclipseRuns
```

3. Import into your IDE:
   - For IntelliJ IDEA: Import as Gradle project
   - For Eclipse: Import as Gradle project using Buildship

4. Refresh dependencies if needed:
```bash
gradlew --refresh-dependencies
```

## Project Structure

```
NeoEssentials/
├── src/main/
│   ├── java/com/zerog/neoessentials/  # Main source code
│   │   ├── commands/                  # Command implementations
│   │   ├── config/                    # Configuration classes
│   │   ├── data/                      # Data storage and management
│   │   ├── economy/                   # Economy system
│   │   ├── permissions/               # Permission handling
│   │   ├── ui/                        # User interface components
│   │   └── utils/                     # Utility classes
│   └── resources/                     # Resource files
│       ├── META-INF/                  # Mod metadata
│       └── assets/neoessentials/      # Mod assets
├── docs/                              # Documentation files
├── build.gradle                       # Gradle build script
├── gradle.properties                  # Project properties
└── settings.gradle                    # Gradle settings
```

## Multi-Version Support

The mod supports different Minecraft versions through version-specific source sets:

- **Main source code**: Common code shared between all versions
- **Version-specific folders**: Code specific to Minecraft/NeoForge versions

### Adding Support for a New Version

1. Create a new version-specific folder in the project root
2. Set up appropriate build.gradle file
3. Link it in the main settings.gradle file
4. Implement version-specific adapters and APIs

## Building

To build the mod:

```bash
# For Windows
gradlew build

# For Linux/macOS
./gradlew build
```

The built mod JAR can be found in `build/libs/`.

## Version Numbering

NeoEssentials uses an automatic build numbering system. The version format is:
```
[major].[minor].[patch].[build]
```

The build number automatically increments with each successful build.

## Testing

Before submitting changes:

1. Run the tests:
```bash
gradlew test
```

2. Test in-game to ensure everything works properly
3. Check compatibility with vanilla clients

## Code Style

- Follow Java naming conventions
- Use proper JavaDoc for public methods
- Use 4 spaces for indentation
- Keep classes focused and cohesive
- Write meaningful commit messages

## Documentation

All major features should be documented in two places:

1. **In-code documentation**: JavaDoc comments for classes and methods
2. **Wiki documentation**: Markdown files in the `docs/` directory

## Mapping Names

By default, the MDK is configured to use the official mapping names from Mojang for methods and fields 
in the Minecraft codebase. These names are covered by a specific license. All modders should be aware of this
license. For the latest license text, refer to the mapping file itself, or the reference copy here:
https://github.com/NeoForged/NeoForm/blob/main/Mojang.md

## Contributing

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Run tests and ensure build passes
5. Submit a pull request

## Release Process

1. Update version number in gradle.properties
2. Ensure all tests pass
3. Update changelog
4. Build the release version
5. Create a GitHub release
6. Deploy to CurseForge and Modrinth

## License

NeoEssentials is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.
