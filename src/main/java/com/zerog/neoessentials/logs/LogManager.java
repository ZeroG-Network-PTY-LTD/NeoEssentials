package com.zerog.neoessentials.logs;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.*;
import java.time.Instant;
import java.util.*;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import java.util.zip.GZIPInputStream;

/**
 * Manages server log file access and searching.
 * Provides real-time log tailing, filtering, and searching capabilities.
 */
public class LogManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(LogManager.class);
    private static LogManager instance;
    
    private final Path logsDirectory;
    private final Path latestLogPath;
    
    private LogManager() {
        this.logsDirectory = Paths.get("logs");
        this.latestLogPath = logsDirectory.resolve("latest.log");
        
        if (!Files.exists(logsDirectory)) {
            LOGGER.warn("Logs directory does not exist: {}", logsDirectory);
        }
    }
    
    public static LogManager getInstance() {
        if (instance == null) {
            instance = new LogManager();
        }
        return instance;
    }
    
    /**
     * Data class for log entry
     */
    public static class LogEntry {
        private final String timestamp;
        private final String level;
        private final String thread;
        private final String logger;
        private final String message;
        private final long lineNumber;
        
        public LogEntry(String timestamp, String level, String thread, String logger, String message, long lineNumber) {
            this.timestamp = timestamp;
            this.level = level;
            this.thread = thread;
            this.logger = logger;
            this.message = message;
            this.lineNumber = lineNumber;
        }
        
        public String getTimestamp() { return timestamp; }
        public String getLevel() { return level; }
        public String getThread() { return thread; }
        public String getLogger() { return logger; }
        public String getMessage() { return message; }
        public long getLineNumber() { return lineNumber; }
    }
    
    /**
     * Get the most recent lines from the latest log file
     */
    public List<LogEntry> tailLog(int lineCount) {
        if (!Files.exists(latestLogPath)) {
            LOGGER.warn("Latest log file does not exist: {}", latestLogPath);
            return Collections.emptyList();
        }
        
        try {
            List<String> allLines = Files.readAllLines(latestLogPath);
            int totalLines = allLines.size();
            int startIndex = Math.max(0, totalLines - lineCount);
            
            List<LogEntry> entries = new ArrayList<>();
            for (int i = startIndex; i < totalLines; i++) {
                LogEntry entry = parseLogLine(allLines.get(i), i + 1);
                if (entry != null) {
                    entries.add(entry);
                }
            }
            
            return entries;
        } catch (IOException e) {
            LOGGER.error("Failed to tail log file", e);
            return Collections.emptyList();
        }
    }
    
    /**
     * Search log file with optional filters
     */
    public List<LogEntry> searchLogs(String query, String logLevel, boolean useRegex, 
                                      boolean caseSensitive, int maxResults) {
        if (!Files.exists(latestLogPath)) {
            return Collections.emptyList();
        }
        
        Pattern searchPattern = null;
        if (useRegex) {
            try {
                int flags = caseSensitive ? 0 : Pattern.CASE_INSENSITIVE;
                searchPattern = Pattern.compile(query, flags);
            } catch (PatternSyntaxException e) {
                LOGGER.warn("Invalid regex pattern: {}", query, e);
                return Collections.emptyList();
            }
        }
        
        List<LogEntry> results = new ArrayList<>();
        
        try (BufferedReader reader = Files.newBufferedReader(latestLogPath)) {
            String line;
            long lineNumber = 0;
            
            while ((line = reader.readLine()) != null && results.size() < maxResults) {
                lineNumber++;
                LogEntry entry = parseLogLine(line, lineNumber);
                
                if (entry == null) {
                    continue;
                }
                
                // Filter by log level if specified
                if (logLevel != null && !logLevel.isEmpty() && 
                    !logLevel.equalsIgnoreCase("ALL") && 
                    !entry.getLevel().equalsIgnoreCase(logLevel)) {
                    continue;
                }
                
                // Filter by search query if specified
                if (query != null && !query.isEmpty()) {
                    boolean matches;
                    if (useRegex) {
                        matches = searchPattern.matcher(line).find();
                    } else {
                        matches = caseSensitive ? 
                            line.contains(query) : 
                            line.toLowerCase().contains(query.toLowerCase());
                    }
                    
                    if (!matches) {
                        continue;
                    }
                }
                
                results.add(entry);
            }
        } catch (IOException e) {
            LOGGER.error("Failed to search logs", e);
        }
        
        return results;
    }
    
    /**
     * Parse a log line into a LogEntry
     * Format: [HH:MM:SS] [Thread/LEVEL] [Logger]: Message
     */
    private LogEntry parseLogLine(String line, long lineNumber) {
        if (line == null || line.trim().isEmpty()) {
            return null;
        }
        
        try {
            // Basic parsing for Minecraft/Forge log format
            // [12:34:56] [Server thread/INFO] [net.minecraft.server.MinecraftServer]: Starting minecraft server version 1.21.1
            
            String timestamp = "";
            String level = "INFO";
            String thread = "";
            String logger = "";
            String message = line;
            
            // Extract timestamp [HH:MM:SS]
            if (line.startsWith("[")) {
                int timestampEnd = line.indexOf("]");
                if (timestampEnd > 0) {
                    timestamp = line.substring(1, timestampEnd);
                    line = line.substring(timestampEnd + 1).trim();
                }
            }
            
            // Extract thread and level [Thread/LEVEL]
            if (line.startsWith("[")) {
                int threadEnd = line.indexOf("]");
                if (threadEnd > 0) {
                    String threadLevel = line.substring(1, threadEnd);
                    int slashIndex = threadLevel.indexOf("/");
                    if (slashIndex > 0) {
                        thread = threadLevel.substring(0, slashIndex);
                        level = threadLevel.substring(slashIndex + 1);
                    }
                    line = line.substring(threadEnd + 1).trim();
                }
            }
            
            // Extract logger [Logger]:
            if (line.startsWith("[")) {
                int loggerEnd = line.indexOf("]:");
                if (loggerEnd > 0) {
                    logger = line.substring(1, loggerEnd);
                    message = line.substring(loggerEnd + 2).trim();
                } else {
                    message = line;
                }
            }
            
            return new LogEntry(timestamp, level, thread, logger, message, lineNumber);
            
        } catch (Exception e) {
            LOGGER.debug("Failed to parse log line: {}", line, e);
            return new LogEntry("", "INFO", "", "", line, lineNumber);
        }
    }
    
    /**
     * Get list of available log files (including rotated logs)
     */
    public List<LogFileInfo> getLogFiles() {
        if (!Files.exists(logsDirectory)) {
            return Collections.emptyList();
        }
        
        List<LogFileInfo> logFiles = new ArrayList<>();
        
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(logsDirectory, "*.log*")) {
            for (Path path : stream) {
                try {
                    long size = Files.size(path);
                    Instant modified = Files.getLastModifiedTime(path).toInstant();
                    boolean isCompressed = path.toString().endsWith(".gz");
                    boolean isLatest = path.equals(latestLogPath);
                    
                    logFiles.add(new LogFileInfo(
                        path.getFileName().toString(),
                        size,
                        modified,
                        isCompressed,
                        isLatest
                    ));
                } catch (IOException e) {
                    LOGGER.warn("Failed to get info for log file: {}", path, e);
                }
            }
        } catch (IOException e) {
            LOGGER.error("Failed to list log files", e);
        }
        
        // Sort by modified time (newest first)
        logFiles.sort((a, b) -> b.getModified().compareTo(a.getModified()));
        
        return logFiles;
    }
    
    /**
     * Get log file content for download
     */
    public byte[] getLogFileContent(String fileName) throws IOException {
        Path logFile = logsDirectory.resolve(fileName);
        
        // Security check - ensure file is within logs directory
        if (!logFile.normalize().startsWith(logsDirectory.normalize())) {
            throw new SecurityException("Invalid log file path");
        }
        
        if (!Files.exists(logFile)) {
            throw new FileNotFoundException("Log file not found: " + fileName);
        }
        
        if (fileName.endsWith(".gz")) {
            // Decompress .gz file
            try (GZIPInputStream gzis = new GZIPInputStream(Files.newInputStream(logFile));
                 ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
                byte[] buffer = new byte[8192];
                int len;
                while ((len = gzis.read(buffer)) > 0) {
                    baos.write(buffer, 0, len);
                }
                return baos.toByteArray();
            }
        } else {
            return Files.readAllBytes(logFile);
        }
    }
    
    /**
     * Get log file statistics
     */
    public LogStats getLogStats() {
        if (!Files.exists(latestLogPath)) {
            return new LogStats(0, 0, new HashMap<>());
        }
        
        try {
            long fileSize = Files.size(latestLogPath);
            long lineCount = Files.lines(latestLogPath).count();
            
            // Count entries by level
            Map<String, Long> levelCounts = new HashMap<>();
            try (BufferedReader reader = Files.newBufferedReader(latestLogPath)) {
                String line;
                while ((line = reader.readLine()) != null) {
                    LogEntry entry = parseLogLine(line, 0);
                    if (entry != null) {
                        levelCounts.merge(entry.getLevel(), 1L, Long::sum);
                    }
                }
            }
            
            return new LogStats(fileSize, lineCount, levelCounts);
            
        } catch (IOException e) {
            LOGGER.error("Failed to get log stats", e);
            return new LogStats(0, 0, new HashMap<>());
        }
    }
    
    /**
     * Data class for log file info
     */
    public static class LogFileInfo {
        private final String name;
        private final long size;
        private final Instant modified;
        private final boolean compressed;
        private final boolean latest;
        
        public LogFileInfo(String name, long size, Instant modified, boolean compressed, boolean latest) {
            this.name = name;
            this.size = size;
            this.modified = modified;
            this.compressed = compressed;
            this.latest = latest;
        }
        
        public String getName() { return name; }
        public long getSize() { return size; }
        public Instant getModified() { return modified; }
        public boolean isCompressed() { return compressed; }
        public boolean isLatest() { return latest; }
    }
    
    /**
     * Data class for log statistics
     */
    public static class LogStats {
        private final long fileSize;
        private final long lineCount;
        private final Map<String, Long> levelCounts;
        
        public LogStats(long fileSize, long lineCount, Map<String, Long> levelCounts) {
            this.fileSize = fileSize;
            this.lineCount = lineCount;
            this.levelCounts = levelCounts;
        }
        
        public long getFileSize() { return fileSize; }
        public long getLineCount() { return lineCount; }
        public Map<String, Long> getLevelCounts() { return levelCounts; }
    }
}
