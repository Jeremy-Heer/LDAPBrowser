package com.example.ldapbrowser.service;

import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
* Service for managing application logs
*/
@Service
public class LoggingService {

public enum LogLevel {
INFO, WARNING, ERROR, DEBUG
}

public static class LogEntry {
private final LocalDateTime timestamp;
private final LogLevel level;
private final String category;
private final String message;
private final String details;

public LogEntry(LogLevel level, String category, String message, String details) {
this.timestamp = LocalDateTime.now();
this.level = level;
this.category = category;
this.message = message;
this.details = details;
}

public LogEntry(LogLevel level, String category, String message) {
this(level, category, message, null);
}

public LocalDateTime getTimestamp() {
return timestamp;
}

public LogLevel getLevel() {
return level;
}

public String getCategory() {
return category;
}

public String getMessage() {
return message;
}

public String getDetails() {
return details;
}

public String getFormattedTimestamp() {
return timestamp.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
}

@Override
public String toString() {
StringBuilder sb = new StringBuilder();
sb.append("[").append(getFormattedTimestamp()).append("] ");
sb.append("[").append(level).append("] ");
sb.append("[").append(category).append("] ");
sb.append(message);
if (details != null && !details.trim().isEmpty()) {
 sb.append(" - ").append(details);
}
return sb.toString();
}
}

private final List<LogEntry> logs = new CopyOnWriteArrayList<>();
private final List<Runnable> logUpdateListeners = new CopyOnWriteArrayList<>();

// Maximum number of log entries to keep in memory
private static final int MAX_LOG_ENTRIES = 1000;

/**
* Add a log entry
*/
public void log(LogLevel level, String category, String message, String details) {
LogEntry entry = new LogEntry(level, category, message, details);
logs.add(entry);

// Keep only the most recent entries
if (logs.size() > MAX_LOG_ENTRIES) {
logs.remove(0);
}

// Notify listeners
notifyLogUpdateListeners();
}

/**
* Add a log entry without details
*/
public void log(LogLevel level, String category, String message) {
log(level, category, message, null);
}

/**
* Convenience methods for different log levels
*/
public void logInfo(String category, String message) {
log(LogLevel.INFO, category, message);
}

public void logInfo(String category, String message, String details) {
log(LogLevel.INFO, category, message, details);
}

public void logWarning(String category, String message) {
log(LogLevel.WARNING, category, message);
}

public void logWarning(String category, String message, String details) {
log(LogLevel.WARNING, category, message, details);
}

public void logError(String category, String message) {
log(LogLevel.ERROR, category, message);
}

public void logError(String category, String message, String details) {
log(LogLevel.ERROR, category, message, details);
}

public void logDebug(String category, String message) {
log(LogLevel.DEBUG, category, message);
}

public void logDebug(String category, String message, String details) {
log(LogLevel.DEBUG, category, message, details);
}

/**
* Connection-related logging methods
*/
public void logConnection(String serverName, String message) {
logInfo("CONNECTION", serverName + ": " + message);
}

public void logConnectionError(String serverName, String message, String error) {
logError("CONNECTION", serverName + ": " + message, error);
}

/**
* Search-related logging methods
*/
public void logSearch(String serverName, String baseDn, String filter, int resultCount) {
String message = String.format("Search in %s: base='%s', filter='%s', results=%d",
serverName, baseDn, filter, resultCount);
logInfo("SEARCH", message);
}

public void logSearchError(String serverName, String baseDn, String filter, String error) {
String message = String.format("Search failed in %s: base='%s', filter='%s'",
serverName, baseDn, filter);
logError("SEARCH", message, error);
}

/**
* Modification-related logging methods
*/
public void logModification(String serverName, String dn, String operation) {
String message = String.format("%s on %s: %s", operation, serverName, dn);
logInfo("MODIFY", message);
}

public void logModificationError(String serverName, String dn, String operation, String error) {
String message = String.format("%s failed on %s: %s", operation, serverName, dn);
logError("MODIFY", message, error);
}

/**
* Import/Export related logging methods
*/
public void logImport(String serverName, String source, int entriesProcessed) {
String message = String.format("Import to %s from %s: %d entries processed",
serverName, source, entriesProcessed);
logInfo("IMPORT", message);
}

public void logExport(String serverName, String destination, int entriesExported) {
String message = String.format("Export from %s to %s: %d entries exported",
serverName, destination, entriesExported);
logInfo("EXPORT", message);
}

/**
* Get all log entries
*/
public List<LogEntry> getAllLogs() {
return new ArrayList<>(logs);
}

/**
* Get logs filtered by level
*/
public List<LogEntry> getLogsByLevel(LogLevel level) {
return logs.stream()
.filter(entry -> entry.getLevel() == level)
.collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
}

/**
* Get logs filtered by category
*/
public List<LogEntry> getLogsByCategory(String category) {
return logs.stream()
.filter(entry -> entry.getCategory().equalsIgnoreCase(category))
.collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
}

/**
* Get the most recent logs
*/
public List<LogEntry> getRecentLogs(int count) {
if (logs.size() <= count) {
return new ArrayList<>(logs);
}
return new ArrayList<>(logs.subList(logs.size() - count, logs.size()));
}

/**
* Clear all logs
*/
public void clearLogs() {
logs.clear();
notifyLogUpdateListeners();
}

/**
* Add a listener for log updates
*/
public void addLogUpdateListener(Runnable listener) {
logUpdateListeners.add(listener);
}

/**
* Remove a log update listener
*/
public void removeLogUpdateListener(Runnable listener) {
logUpdateListeners.remove(listener);
}

/**
* Notify all log update listeners
*/
private void notifyLogUpdateListeners() {
logUpdateListeners.forEach(Runnable::run);
}

/**
* Get log statistics
*/
public LogStats getLogStats() {
int totalLogs = logs.size();
int errorCount = (int) logs.stream().filter(entry -> entry.getLevel() == LogLevel.ERROR).count();
int warningCount = (int) logs.stream().filter(entry -> entry.getLevel() == LogLevel.WARNING).count();
int infoCount = (int) logs.stream().filter(entry -> entry.getLevel() == LogLevel.INFO).count();
int debugCount = (int) logs.stream().filter(entry -> entry.getLevel() == LogLevel.DEBUG).count();

return new LogStats(totalLogs, errorCount, warningCount, infoCount, debugCount);
}

public static class LogStats {
private final int totalLogs;
private final int errorCount;
private final int warningCount;
private final int infoCount;
private final int debugCount;

public LogStats(int totalLogs, int errorCount, int warningCount, int infoCount, int debugCount) {
this.totalLogs = totalLogs;
this.errorCount = errorCount;
this.warningCount = warningCount;
this.infoCount = infoCount;
this.debugCount = debugCount;
}

public int getTotalLogs() { return totalLogs; }
public int getErrorCount() { return errorCount; }
public int getWarningCount() { return warningCount; }
public int getInfoCount() { return infoCount; }
public int getDebugCount() { return debugCount; }
}
}