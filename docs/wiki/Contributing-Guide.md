# Contributing to NeoEssentials

Thank you for your interest in contributing to the NeoEssentials project! This guide will help you get started with contributing code, documentation, or other improvements to the mod.

![Contributing Icon](../images/icons/contributing.png)

## Table of Contents

- [Code of Conduct](#code-of-conduct)
- [Getting Started](#getting-started)
- [Development Environment](#development-environment)
- [Contribution Workflow](#contribution-workflow)
- [Coding Standards](#coding-standards)
- [Testing](#testing)
- [Documentation](#documentation)
- [Submitting Pull Requests](#submitting-pull-requests)
- [Reporting Bugs](#reporting-bugs)
- [Feature Requests](#feature-requests)

## Code of Conduct

By participating in this project, you are expected to uphold our Code of Conduct:

- Be respectful and inclusive
- Provide constructive feedback
- Focus on what's best for the community
- Show empathy towards other community members

## Getting Started

### Recommended Knowledge

- Java programming
- Minecraft/NeoForge mod development
- Git and GitHub workflows
- Understanding of Minecraft server operations

### Types of Contributions

We welcome various types of contributions:

- Code improvements and bug fixes
- New features
- Documentation updates
- Translations
- Testing and quality assurance
- Artwork and assets

## Development Environment

### Setting Up Your Environment

1. **Fork the Repository**:
   - Visit [NeoEssentials GitHub Repository](https://github.com/ZeroG-Network/NeoEssentials)
   - Click the "Fork" button to create your own copy

2. **Clone Your Fork**:
   ```bash
   git clone https://github.com/your-username/NeoEssentials.git
   cd NeoEssentials
   ```

3. **Set Up the Development Environment**:
   ```bash
   ./gradlew setupDevWorkspace
   ```

4. **Import into Your IDE**:
   - For IntelliJ IDEA:
     ```bash
     ./gradlew genIntellijRuns
     ```
     Then, import the project as a Gradle project

   - For Eclipse:
     ```bash
     ./gradlew genEclipseRuns
     ```
     Then, import the project as an Eclipse project

5. **Build the Project**:
   ```bash
   ./gradlew build
   ```

### Project Structure

```
NeoEssentials/
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/zerog/neoessentials/
│   │   │       ├── api/           # Public API
│   │   │       ├── commands/      # Command implementations
│   │   │       ├── config/        # Configuration handlers
│   │   │       ├── economy/       # Economy system
│   │   │       ├── home/          # Home system
│   │   │       ├── permissions/   # Permission system
│   │   │       ├── storage/       # Data storage
│   │   │       ├── ui/            # User interfaces
│   │   │       │   └── tab/       # Tablist features
│   │   │       ├── util/          # Utility classes
│   │   │       ├── warp/          # Warp system
│   │   │       └── NeoEssentials.java  # Main class
│   │   └── resources/
│   │       ├── assets/
│   │       ├── data/
│   │       ├── default-neoessentials/ # Default configurations
│   │       └── META-INF/
│   └── test/
│       └── java/
│           └── com/zerog/neoessentials/
├── gradle/
├── docs/                      # Documentation
├── build.gradle
├── gradle.properties
└── README.md
```

## Contribution Workflow

### 1. Choose an Issue

- Browse the [issue tracker](https://github.com/ZeroG-Network/NeoEssentials/issues)
- Look for issues labeled `good first issue` or `help wanted`
- Comment on the issue to express your interest

### 2. Create a Branch

Create a new branch for your work:

```bash
git checkout -b feature/your-feature-name
# or
git checkout -b fix/your-fix-name
```

Use a clear, descriptive name that reflects the purpose of your changes.

### 3. Make Changes

- Write your code or documentation changes
- Follow the project's coding standards (see below)
- Keep changes focused on a single issue/feature

### 4. Test Your Changes

- Run the existing test suite:
  ```bash
  ./gradlew test
  ```
- Add appropriate tests for your changes
- Test the mod in a Minecraft environment:
  ```bash
  ./gradlew runClient
  # or
  ./gradlew runServer
  ```

### 5. Commit Changes

Write clear, concise commit messages:

```bash
git add .
git commit -m "Type: Brief description of changes"
```

Where `Type` is one of:
- `feat:` - A new feature
- `fix:` - A bug fix
- `docs:` - Documentation changes
- `style:` - Code style/formatting changes
- `refactor:` - Code refactoring
- `test:` - Adding or updating tests
- `chore:` - Maintenance tasks

### 6. Keep Up to Date

Regularly sync your fork with the upstream repository:

```bash
git remote add upstream https://github.com/ZeroG-Network/NeoEssentials.git
git fetch upstream
git checkout main
git merge upstream/main
git checkout your-branch-name
git rebase main
```

### 7. Push Changes

```bash
git push origin your-branch-name
```

### 8. Create a Pull Request

- Go to your fork on GitHub
- Click "New Pull Request"
- Select your branch and fill out the PR template
- Link the PR to any relevant issues

## Coding Standards

### Java Coding Style

- Use 4 spaces for indentation, not tabs
- Class names in PascalCase (`PlayerHomeManager`)
- Method and variable names in camelCase (`getPlayerHome`)
- Constants in UPPER_SNAKE_CASE (`MAX_HOMES`)
- Use descriptive names for variables, methods, and classes
- Add JavaDoc comments for public APIs

### Code Quality Guidelines

- Keep methods focused and concise
- Minimize dependencies between components
- Handle exceptions appropriately
- Write defensive code with null checks
- Add meaningful comments for complex logic
- Avoid magic numbers (use named constants)

### Performance Considerations

- Be mindful of server performance impact
- Avoid expensive operations on the main server thread
- Cache results when appropriate
- Consider large server environments

## Testing

### Unit Testing

- Write JUnit tests for your code when applicable
- Place tests in the `src/test/java` directory
- Name test classes with a `Test` suffix

### Manual Testing

- Test your changes in a single-player environment
- Test on a small multiplayer server if possible
- Check compatibility with other mods
- Verify that existing features still work

## Documentation

### Code Documentation

- Add JavaDoc comments to public classes and methods
- Explain the purpose, parameters, and return values
- Document exceptions that may be thrown

### User Documentation

- Update wiki pages affected by your changes
- Add examples for new features
- Keep configuration documentation up-to-date

## Submitting Pull Requests

### PR Requirements

- Clearly describe the changes and their purpose
- Reference any related issues
- Include steps to test the changes
- Ensure CI checks pass
- Address review comments promptly

### Review Process

1. Wait for code review from maintainers
2. Make requested changes if needed
3. Once approved, maintainers will merge your PR
4. Your contribution will be included in the next release

## Reporting Bugs

When reporting bugs, please include:

- A clear, descriptive title
- Steps to reproduce the issue
- Expected vs. actual behavior
- Server environment details (NeoForge version, other mods)
- Relevant logs or error messages
- Screenshots if applicable

## Feature Requests

For feature requests:

- Clearly describe the feature and its use case
- Explain how it benefits NeoEssentials users
- Include mockups or examples if possible
- Be open to discussion and feedback

---

Thank you for contributing to NeoEssentials! Your efforts help improve the mod for the entire community.

*For additional questions, join our [Discord server](https://discord.gg/dUGAQF2Mga) or reach out on [GitHub](https://github.com/ZeroG-Network/NeoEssentials).*
