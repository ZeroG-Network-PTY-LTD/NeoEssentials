# Performance

NeoEssentials is designed with performance in mind, offering comprehensive monitoring, optimization tools, and configuration options to ensure your server runs smoothly even under heavy load.

## 📊 Performance Monitoring

### Real-time Performance Metrics
Monitor server performance with detailed metrics:

```bash
/performance                    # Open performance dashboard
/tps                           # Current TPS information
/lag                           # Lag analysis and reporting
/memory                        # Memory usage statistics
/cpu                           # CPU usage information
/entities                      # Entity count per world
```

### Performance Dashboard
The performance dashboard provides real-time monitoring:

**Key Metrics:**
- **TPS (Ticks Per Second)** - Server performance indicator (target: 20 TPS)
- **Memory Usage** - RAM consumption and allocation
- **CPU Usage** - Processor utilization percentage
- **Entity Count** - Living entities causing server load
- **Chunk Loading** - Active chunks and loading statistics
- **Player Count** - Current and maximum player counts

### Performance Alerts
Automatic alerts when performance issues are detected:

```toml
[performance.alerts]
# Enable performance monitoring
enabled = true

# TPS warning threshold
tpsWarning = 18.0
tpsCritical = 15.0

# Memory warning threshold (percentage)
memoryWarning = 80
memoryCritical = 90

# CPU warning threshold (percentage)
cpuWarning = 80
cpuCritical = 90

# Entity count warning
entityWarning = 500
entityCritical = 1000

# Alert methods
alertMethods = ["console", "discord", "email"]
```

## ⚡ Performance Optimization

### NeoEssentials Optimizations
Built-in optimizations in NeoEssentials:

#### Async Processing
```toml
[performance.async]
# Enable asynchronous processing
enabled = true

# Player data operations
playerDataAsync = true

# Database operations
databaseAsync = true

# File I/O operations
fileIOAsync = true

# Notification processing
notificationAsync = true

# Thread pool configuration
coreThreads = 4
maxThreads = 16
queueSize = 1000
```

#### Caching System
```toml
[performance.cache]
# Enable intelligent caching
enabled = true

# Cache sizes (entries)
playerDataCache = 1000
configCache = 100
languageCache = 50
permissionCache = 500

# Cache expiration (seconds)
playerDataTTL = 1800  # 30 minutes
configTTL = 3600      # 1 hour
languageTTL = 7200    # 2 hours
permissionTTL = 900   # 15 minutes

# Cache cleanup interval (seconds)
cleanupInterval = 300
```

#### Batch Operations
```toml
[performance.batching]
# Enable batch processing
enabled = true

# Batch sizes
databaseBatch = 100
fileBatch = 50
notificationBatch = 25

# Batch timeouts (milliseconds)
batchTimeout = 5000
maxWaitTime = 10000

# Auto-flush on shutdown
flushOnShutdown = true
```

### Server Optimization Commands
Optimize server performance with commands:

```bash
/optimize entities              # Remove unnecessary entities
/optimize chunks                # Unload unused chunks
/optimize memory                # Force garbage collection
/optimize database              # Optimize database tables
/optimize cache                 # Clear and rebuild caches
```

### Performance Profiles
Pre-configured performance profiles:

```bash
/performance profile <name>     # Apply performance profile
/performance profile list       # List available profiles
/performance profile create <name> # Create custom profile
```

**Available Profiles:**
- **Default** - Balanced performance and features
- **High Performance** - Maximum performance, reduced features
- **Low Resource** - Minimal resource usage
- **Large Server** - Optimized for 100+ players
- **Creative** - Optimized for creative mode servers

## 🔧 Configuration Optimization

### Feature Optimization
Optimize individual features for performance:

```toml
[features.optimization]
# Essential commands optimization
essentialCommands = {
  cacheResults = true,
  asyncExecution = true,
  rateLimiting = true
}

# GUI system optimization
guiSystem = {
  cacheGUIs = true,
  asyncLoading = true,
  lazyLoading = true,
  maxConcurrentGUIs = 50
}

# Teleportation optimization
teleportation = {
  cacheLocations = true,
  asyncTeleport = true,
  chunkPreloading = true,
  safetyChecks = "fast"
}

# Security optimization
security = {
  batchProcessing = true,
  asyncAnalysis = true,
  cacheResults = true,
  scanInterval = 60
}
```

### Database Optimization
Optimize database performance:

```toml
[database.optimization]
# Connection pooling
connectionPool = {
  minConnections = 5,
  maxConnections = 20,
  idleTimeout = 300,
  maxLifetime = 1800
}

# Query optimization
queries = {
  preparedStatements = true,
  batchUpdates = true,
  cacheQueries = true,
  queryTimeout = 30
}

# Index optimization
indexes = {
  autoOptimize = true,
  analyzeFrequency = "weekly",
  rebuildThreshold = 0.1
}
```

### Memory Management
Optimize memory usage:

```toml
[memory.optimization]
# Garbage collection tuning
gc = {
  algorithm = "G1GC",
  heapSize = "4G",
  newRatio = 3,
  survivorRatio = 8
}

# Memory pools
pools = {
  playerData = "128MB",
  cache = "256MB",
  temporary = "64MB"
}

# Memory monitoring
monitoring = {
  enabled = true,
  alertThreshold = 85,
  gcLogging = false
}
```

## 📈 Performance Analysis

### Lag Analysis
Identify and resolve lag sources:

```bash
/lag analyze                    # Analyze current lag sources
/lag history                    # Historical lag data
/lag sources                    # Top lag-causing factors
/lag entities                   # Entity-related lag
/lag plugins                    # Plugin performance impact
```

**Lag Analysis Report:**
```
=== Lag Analysis Report ===
Current TPS: 17.2 (Target: 20.0)
Primary Lag Sources:
1. Entity Processing (35% impact)
   - Too many mobs in world_nether
   - Recommendation: Reduce mob spawn rates
   
2. Chunk Loading (25% impact)
   - Players exploring new chunks rapidly
   - Recommendation: Implement chunk preloading
   
3. Database Queries (20% impact)
   - Slow player data queries
   - Recommendation: Add database indexes
```

### Performance Profiling
Detailed performance profiling:

```bash
/profile start [duration]       # Start performance profiling
/profile stop                   # Stop profiling
/profile report                 # Generate profiling report
/profile export                 # Export profiling data
```

### Performance Benchmarks
Benchmark server components:

```bash
/benchmark cpu                  # CPU benchmark
/benchmark memory               # Memory benchmark
/benchmark disk                 # Disk I/O benchmark
/benchmark network              # Network benchmark
/benchmark database             # Database benchmark
```

## 🚀 Advanced Performance Features

### Load Balancing
Distribute load across multiple threads:

```toml
[performance.loadBalancing]
# Enable load balancing
enabled = true

# Load balancing strategy
strategy = "round_robin"  # round_robin, least_loaded, random

# Thread allocation
playerThreads = 4
databaseThreads = 2
fileIOThreads = 2
networkThreads = 2

# Load monitoring
monitorLoad = true
rebalanceInterval = 300
```

### Resource Limiting
Prevent resource exhaustion:

```toml
[performance.limits]
# Player-specific limits
player = {
  maxCommandsPerSecond = 5,
  maxGUIInteractionsPerSecond = 10,
  maxTeleportsPerMinute = 3,
  maxChatMessagesPerSecond = 2
}

# Server-wide limits
server = {
  maxConcurrentTeleports = 20,
  maxConcurrentGUIUsers = 100,
  maxDatabaseConnections = 50,
  maxFileOperations = 100
}
```

### Auto-scaling
Automatically adjust performance based on load:

```toml
[performance.autoscaling]
# Enable auto-scaling
enabled = true

# Scaling triggers
triggers = {
  highCPU = 80,
  highMemory = 85,
  lowTPS = 18,
  highPlayerCount = 80
}

# Scaling actions
actions = {
  reduceCacheSize = true,
  increaseThreads = true,
  disableNonEssential = true,
  enableFastMode = true
}

# Scaling cooldown (seconds)
cooldown = 300
```

## 📊 Performance Reporting

### Automated Reports
Generate regular performance reports:

```toml
[performance.reporting]
# Enable automated reporting
enabled = true

# Report schedule
schedule = "0 6 * * *"  # Daily at 6 AM

# Report types
reports = ["daily", "weekly", "monthly"]

# Report delivery
delivery = {
  console = true,
  file = true,
  discord = false,
  email = false
}

# Report retention
retentionDays = 30
```

### Performance Metrics Export
Export performance data for analysis:

```bash
/performance export json        # Export as JSON
/performance export csv         # Export as CSV
/performance export grafana     # Export for Grafana
/performance export prometheus  # Export for Prometheus
```

## 🔍 Performance Troubleshooting

### Common Performance Issues

#### Low TPS (Ticks Per Second)
**Symptoms:** Server feels laggy, slow response times
**Causes:**
- Too many entities
- Excessive chunk loading
- Plugin conflicts
- Database bottlenecks
- Memory leaks

**Solutions:**
```bash
/optimize entities              # Remove excess entities
/lag analyze                    # Identify lag sources
/performance profile high       # Apply high-performance profile
/gc                            # Force garbage collection
```

#### High Memory Usage
**Symptoms:** Out of memory errors, frequent GC
**Causes:**
- Memory leaks
- Large cache sizes
- Too many loaded chunks
- Plugin memory issues

**Solutions:**
```bash
/memory analyze                 # Analyze memory usage
/cache clear                    # Clear caches
/optimize memory                # Force garbage collection
/performance profile low        # Apply low-resource profile
```

#### Database Performance Issues
**Symptoms:** Slow player data loading, query timeouts
**Causes:**
- Missing database indexes
- Too many concurrent connections
- Slow queries
- Database server issues

**Solutions:**
```bash
/database optimize              # Optimize database
/database analyze               # Analyze performance
/performance cache increase     # Increase caching
```

### Debug Commands
```bash
/debug performance              # Performance debugging
/debug memory                   # Memory debugging
/debug threads                  # Thread debugging
/debug database                 # Database debugging
```

## ⚙️ Performance Configuration Best Practices

### JVM Optimization
Recommended JVM flags for optimal performance:

```bash
# For servers with 4GB+ RAM
-Xms4G -Xmx4G
-XX:+UseG1GC
-XX:G1HeapRegionSize=32M
-XX:+UnlockExperimentalVMOptions
-XX:+DisableExplicitGC
-XX:+AlwaysPreTouch
-XX:G1NewSizePercent=20
-XX:G1ReservePercent=20
-XX:MaxGCPauseMillis=50
-XX:G1HeapWastePercent=5
-XX:G1MixedGCCountTarget=4
-XX:InitiatingHeapOccupancyPercent=15
-XX:G1MixedGCLiveThresholdPercent=90
-XX:G1RSetUpdatingPauseTimePercent=5
-XX:SurvivorRatio=32
-XX:+PerfDisableSharedMem
-XX:MaxTenuringThreshold=1
```

### NeoEssentials Configuration
Optimal NeoEssentials settings for performance:

```toml
[performance]
# Core performance settings
asyncProcessing = true
cacheEnabled = true
batchOperations = true

# Feature-specific optimizations
[features]
security.asyncProcessing = true
teleportation.cacheLocations = true
gui.lazyLoading = true
economy.batchTransactions = true

# Resource limits
[limits]
maxPlayerDataCacheSize = 1000
maxConcurrentTeleports = 20
maxDatabaseConnections = 15
```

## 📚 Performance Monitoring Tools

### Built-in Tools
NeoEssentials includes comprehensive monitoring:

- **Real-time TPS monitoring**
- **Memory usage tracking**
- **CPU usage monitoring**
- **Entity count tracking**
- **Database performance metrics**
- **Cache hit/miss ratios**

### External Integration
Integrate with external monitoring tools:

```toml
[monitoring.external]
# Prometheus metrics
prometheus = {
  enabled = true,
  port = 9090,
  endpoint = "/metrics"
}

# Grafana dashboards
grafana = {
  enabled = true,
  dashboardUrl = "http://grafana.server.com"
}

# Custom webhooks
webhooks = [
  {
    url = "http://monitoring.server.com/webhook",
    events = ["high_cpu", "low_tps", "memory_warning"]
  }
]
```

---

## 📚 Related Documentation

- **[Configuration](Configuration.md)** - Performance configuration options
- **[Storage](Storage.md)** - Database and storage optimization
- **[Security Features](Security.md)** - Security impact on performance
- **[API Reference](API.md)** - Performance monitoring API

*Last Updated: August 6, 2025*
