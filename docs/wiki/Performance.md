# Performance

NeoEssentials includes basic performance monitoring and optimization systems to help track your server's performance. The performance system provides command execution tracking, memory usage monitoring, and caching functionality.

## 📊 Performance Monitoring

### Available Performance Commands
Monitor server performance with these commands:

```bash
/performance stats             # Show performance statistics
/performance memory           # Show memory usage information
/performance cache clear      # Clear performance cache
/performance cache info       # Show cache statistics
/performance async            # Show async executor statistics
/performance gc               # Trigger garbage collection
```

### Performance Monitoring Features
The performance system tracks:

**Key Metrics:**
- **Command Performance** - Execution times and frequency tracking
- **Memory Usage** - JVM heap and non-heap memory monitoring
- **Cache Statistics** - Performance cache size and efficiency
- **System Metrics** - Basic system load and uptime information

### Performance Statistics
View comprehensive performance statistics:

```bash
/performance stats
```

**Example Output:**
```
=== Performance Statistics ===
Average Command Time: 2.45ms
Total Commands: 1,247
Memory Usage: 65.2%
Cache Size: 128
```

## ⚡ Performance Optimization

### Caching System
NeoEssentials includes a basic caching system for frequently accessed data:

```bash
/performance cache clear       # Clear all cached data
/performance cache info        # Show cache statistics
```

**Cache Features:**
- **Command Result Caching** - Caches frequently executed command results
- **Automatic Expiration** - Cache entries expire after 5 minutes
- **Memory Management** - Cache size limited to 1000 entries
- **Performance Tracking** - Tracks cache hit/miss ratios

### Memory Management
Monitor and manage memory usage:

```bash
/performance memory           # Show detailed memory information
/performance gc              # Trigger garbage collection (admin only)
```

**Memory Information Includes:**
- Used Memory (MB)
- Free Memory (MB) 
- Maximum Memory (MB)
- Memory Usage Percentage

## 📈 Performance Analysis

### Command Performance Tracking
NeoEssentials automatically tracks command execution performance:

- **Execution Time Tracking** - Monitors how long commands take to execute
- **Command Frequency** - Tracks which commands are used most often  
- **Performance Warnings** - Logs commands that take longer than 100ms
- **Historical Data** - Maintains performance metrics for analysis

### System Metrics
Basic system performance information is available:

- **Memory Usage** - JVM heap and non-heap memory statistics
- **System Load** - Operating system load average
- **Uptime** - Server runtime information
- **CPU Information** - Available processor cores

### Performance Reports
Generate performance reports with command execution data:

```bash
/performance stats             # View current performance statistics
```

The performance report includes:
- Average command execution time
- Total commands executed
- Memory usage percentage
- Cache utilization
- Most used commands
- Slowest performing commands

## 🔧 Performance Configuration

### Basic Configuration
Performance monitoring can be configured through the PerformanceManager:

**Performance Settings:**
- **Monitoring Enabled** - Toggle performance tracking on/off
- **Cache Size** - Maximum number of cached entries (default: 1000)
- **Cache Expiration** - Cache entry lifetime (default: 5 minutes)
- **Warning Threshold** - Command execution time warning (default: 100ms)

## 📋 Best Practices

### Performance Tips
To optimize NeoEssentials performance:

1. **Monitor Command Performance** - Use `/performance stats` regularly to check command execution times
2. **Clear Cache When Needed** - Use `/performance cache clear` if you suspect cache issues
3. **Monitor Memory Usage** - Use `/performance memory` to check JVM memory consumption
4. **Check for Slow Commands** - Commands taking over 100ms will be logged as warnings

### Troubleshooting Performance Issues

**High Memory Usage:**
1. Check `/performance memory` for current usage
2. Use `/performance gc` to trigger garbage collection
3. Clear performance cache with `/performance cache clear`

**Slow Command Execution:**
1. Check `/performance stats` for average execution times
2. Review server logs for performance warnings
3. Consider reducing server load or optimizing configurations

**Cache Issues:**
1. Check cache statistics with `/performance cache info`
2. Clear cache if needed with `/performance cache clear`
3. Monitor cache hit/miss ratios in performance statistics

---

*For additional support with performance issues, consult the server logs or contact the NeoEssentials development team.*
