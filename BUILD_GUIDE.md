# Building NeoEssentials in JetBrains IntelliJ IDEA

## ✅ Quick Build Guide

This guide will help you build the NeoEssentials mod using IntelliJ IDEA.

---

## 📋 Prerequisites

1. ✅ **Java 21 or higher** installed
2. ✅ **IntelliJ IDEA** (Community or Ultimate Edition)
3. ✅ **Git** (optional, for version control)

---

## 🚀 Building the Mod

### Method 1: Using Gradle Panel (Recommended) ⭐

**Step 1: Open Gradle Panel**
1. In IntelliJ, look for the **Gradle** tab on the right side
2. If not visible, go to: `View` → `Tool Windows` → `Gradle`

**Step 2: Refresh Gradle Project**
1. Click the **Refresh** button (🔄) in the Gradle panel
2. Wait for dependencies to download (first time may take 5-10 minutes)

**Step 3: Build the Mod**
1. In Gradle panel, expand: `NeoEssentials` → `Tasks` → `build`
2. Double-click on **`build`** task
3. Wait for build to complete (watch the progress in bottom panel)

**Step 4: Find Your JAR**
- **Location**: `build/libs/neoessentials-1.0.2.2-HotFix+build.XXX.jar`
- The build number increments automatically each build

---

### Method 2: Using Terminal in IntelliJ

**Step 1: Open Terminal**
1. Click **Terminal** tab at bottom of IntelliJ
2. Or press `Alt+F12`

**Step 2: Run Gradle Build**
```bash
# Windows (PowerShell)
.\gradlew.bat build

# Or use the wrapper directly
./gradlew build
```

**Step 3: Wait for Completion**
- First build: ~5-10 minutes (downloads dependencies)
- Subsequent builds: ~30-60 seconds

**Step 4: Check Output**
```
BUILD SUCCESSFUL in XXs
```

**Step 5: Find Your JAR**
```
build/libs/neoessentials-1.0.2.2-HotFix+build.XXX.jar
```

---

### Method 3: Using Pre-configured Run Configurations ✨ NEW

**Pre-configured tasks now available in IntelliJ**:
- **Clean Build** - Clean and build from scratch
- **Build Server JAR** - Quick build for server deployment

**To Use**:
1. Look at the configuration dropdown (top-right of IntelliJ)
2. Select **"Clean Build"** or **"Build Server JAR"**
3. Click green **▶** (Run) button
4. Or press `Shift+F10`

**Configurations Created**:
- `.idea/runConfigurations/Clean_Build.xml`
- `.idea/runConfigurations/Build_Server_JAR.xml`

**Manual Configuration** (if needed):
1. Click: `Run` → `Edit Configurations...`
2. Click **+** → `Gradle`
3. Name: `Build NeoEssentials`
4. Gradle project: `NeoEssentials`
5. Tasks: `build`
6. Click **OK**


---

## 🧹 Clean Build (Recommended for Major Changes)

If you've made significant changes or want a fresh build:

### Using Gradle Panel
1. Expand: `NeoEssentials` → `Tasks` → `build`
2. Double-click **`clean`**
3. Wait for completion
4. Double-click **`build`**

### Using Terminal
```bash
.\gradlew.bat clean build
```

---

## 🧪 Running the Mod for Testing

### Method 1: Using Gradle runClient Task

**In Gradle Panel**:
1. Expand: `NeoEssentials` → `Tasks` → `neoforge runs`
2. Double-click **`runClient`** (for client testing)
3. Or **`runServer`** (for server testing)

**In Terminal**:
```bash
# Run client (opens Minecraft)
.\gradlew.bat runClient

# Run server (console only)
.\gradlew.bat runServer
```

### Method 2: Using Pre-configured Run Configurations

IntelliJ should have auto-generated:
- **runClient** - Launch Minecraft client with mod
- **runServer** - Launch dedicated server with mod

**To Use**:
1. Select configuration from dropdown (top-right)
2. Click green **▶** (Run) button
3. Or press `Shift+F10`

---

## 📦 Build Output Explained

After successful build, you'll see:

```
build/libs/
├── neoessentials-1.0.2.2-HotFix+build.XXX.jar  ← Main JAR (use this!)
└── neoessentials-1.0.2.2-HotFix+build.XXX-sources.jar (optional, source code)
```

**The main JAR** is what you install on your server/client.

---

## 🔧 Troubleshooting

### Issue 1: "Gradle sync failed"

**Solution**:
```bash
# Clean Gradle cache and rebuild
.\gradlew.bat clean --refresh-dependencies
.\gradlew.bat build
```

### Issue 2: "Cannot find Java 21"

**Solution**:
1. Install Java 21 JDK
2. In IntelliJ: `File` → `Project Structure` → `Project`
3. Set **Project SDK** to Java 21
4. Set **Language level** to 21

### Issue 3: "Out of memory error"

**Solution - Increase Gradle Memory**:

Edit `gradle.properties`:
```properties
org.gradle.jvmargs=-Xmx4G -Xms1G -XX:MaxMetaspaceSize=1G
```

Or in IntelliJ:
1. `File` → `Settings` → `Build, Execution, Deployment` → `Build Tools` → `Gradle`
2. **Gradle VM options**: `-Xmx4G -Xms1G`

### Issue 4: "Compilation errors"

**Check for Errors**:
1. Look at **Problems** panel (bottom of IntelliJ)
2. Fix any red underlined code
3. Run: `Build` → `Rebuild Project`

### Issue 5: Build takes forever

**Speed up builds**:
1. Enable Gradle daemon: Add to `gradle.properties`:
   ```properties
   org.gradle.daemon=true
   org.gradle.parallel=true
   org.gradle.caching=true
   ```

2. In IntelliJ Settings:
   - Enable: `Build, Execution, Deployment` → `Compiler` → `Build project automatically`
   - Enable: `Build, Execution, Deployment` → `Gradle` → `Build and run using: IntelliJ IDEA`

---

## 📝 Build Configuration Files

### gradle.properties
```properties
# Mod Properties
mod_id=neoessentials
mod_name=NeoEssentials
mod_version=1.0.2.2-HotFix
mod_group_id=com.zerog.neoessentials

# NeoForge
neo_version=21.5.92
minecraft_version=1.21.5

# Gradle Settings (for performance)
org.gradle.jvmargs=-Xmx4G -Xms1G
org.gradle.daemon=true
org.gradle.parallel=true
org.gradle.caching=true
```

---

## 🎯 Quick Reference

| Task | Command | Description |
|------|---------|-------------|
| **Build** | `.\gradlew.bat build` | Build the mod JAR |
| **Clean** | `.\gradlew.bat clean` | Delete old build files |
| **Clean Build** | `.\gradlew.bat clean build` | Fresh build from scratch |
| **Run Client** | `.\gradlew.bat runClient` | Test in Minecraft client |
| **Run Server** | `.\gradlew.bat runServer` | Test dedicated server |
| **Refresh** | `.\gradlew.bat --refresh-dependencies` | Re-download dependencies |

---

## 🚀 First-Time Setup Checklist

- [ ] Open project in IntelliJ IDEA
- [ ] Wait for Gradle sync to complete
- [ ] Check Java version (should be 21+)
- [ ] Run Gradle refresh (🔄 button)
- [ ] Run `.\gradlew.bat build`
- [ ] Check `build/libs/` for JAR file
- [ ] ✅ Success!

---

## 📊 Build Time Estimates

| Build Type | First Time | Subsequent |
|------------|------------|------------|
| **Clean Build** | 5-10 minutes | 1-2 minutes |
| **Incremental** | N/A | 30-60 seconds |
| **No Changes** | N/A | 10-20 seconds |

---

## 🎓 Pro Tips

### Tip 1: Use Build on Save
Enable auto-build for faster development:
1. `Settings` → `Build, Execution, Deployment` → `Compiler`
2. ✅ Enable: `Build project automatically`

### Tip 2: Use Gradle Daemon
Speeds up builds significantly:
```properties
# In gradle.properties
org.gradle.daemon=true
```

### Tip 3: Parallel Builds
If you have multiple CPU cores:
```properties
# In gradle.properties
org.gradle.parallel=true
org.gradle.workers.max=4
```

### Tip 4: Build Cache
Reuse previous build outputs:
```properties
# In gradle.properties
org.gradle.caching=true
```

### Tip 5: Exclude from Build
If building is slow, exclude unnecessary files:
1. Right-click folder in Project view
2. `Mark Directory as` → `Excluded`

---

## 📁 Project Structure

```
NeoEssentials/
├── src/main/java/          ← Your code changes
├── src/main/resources/     ← Configs, assets
├── build.gradle            ← Build configuration
├── gradle.properties       ← Mod version, settings
├── build/                  ← Build output (generated)
│   └── libs/               ← JAR files here!
├── gradlew.bat             ← Windows Gradle wrapper
└── gradlew                 ← Unix/Mac Gradle wrapper
```

---

## ✅ Success Indicators

**Build Successful**:
```
BUILD SUCCESSFUL in 45s
XX actionable tasks: XX executed
```

**JAR Created**:
```
> Task :jar
Created: build/libs/neoessentials-1.0.2.2-HotFix+build.XXX.jar
```

**No Errors**:
- No red text in build output
- JAR file exists in `build/libs/`
- File size > 1 MB (typical: 2-5 MB)

---

## 🎉 Quick Start Commands

**If you just want to build right now:**

```bash
# Open terminal in IntelliJ (Alt+F12)
# Then run:
.\gradlew.bat clean build

# Wait for "BUILD SUCCESSFUL"
# JAR will be in: build/libs/
```

**That's it!** 🎊

---

**Last Updated**: January 1, 2026  
**Gradle Version**: 8.x  
**Java Version**: 21+  
**NeoForge Version**: 21.5.92  
**Minecraft Version**: 1.21.5

