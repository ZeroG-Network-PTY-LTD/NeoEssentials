package com.zerog.neoessentials.scheduler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.*;
import java.time.temporal.ChronoUnit;

/**
 * Cron expression parser and evaluator.
 * Supports standard cron format: minute hour day month dayOfWeek
 */
public class CronParser {
    private static final Logger LOGGER = LoggerFactory.getLogger(CronParser.class);
    
    private final String cronExpression;
    private final int[] minutes;
    private final int[] hours;
    private final int[] daysOfMonth;
    private final int[] months;
    private final int[] daysOfWeek;
    
    public CronParser(String cronExpression) throws IllegalArgumentException {
        this.cronExpression = cronExpression;
        
        String[] parts = cronExpression.trim().split("\\s+");
        if (parts.length != 5) {
            throw new IllegalArgumentException("Invalid cron expression. Expected 5 fields: minute hour day month dayOfWeek");
        }
        
        try {
            this.minutes = parseField(parts[0], 0, 59);
            this.hours = parseField(parts[1], 0, 23);
            this.daysOfMonth = parseField(parts[2], 1, 31);
            this.months = parseField(parts[3], 1, 12);
            this.daysOfWeek = parseField(parts[4], 0, 6); // 0 = Sunday
        } catch (Exception e) {
            throw new IllegalArgumentException("Failed to parse cron expression: " + cronExpression, e);
        }
    }
    
    /**
     * Parse a single cron field.
     * Supports wildcards, numbers, ranges, lists, and steps.
     */
    private int[] parseField(String field, int min, int max) {
        if ("*".equals(field)) {
            // All values
            int[] values = new int[max - min + 1];
            for (int i = 0; i < values.length; i++) {
                values[i] = min + i;
            }
            return values;
        }
        
        if (field.contains(",")) {
            // List of values: 1,3,5
            String[] parts = field.split(",");
            int[] values = new int[parts.length];
            for (int i = 0; i < parts.length; i++) {
                values[i] = Integer.parseInt(parts[i].trim());
            }
            return values;
        }
        
        if (field.contains("/")) {
            // Step values: */5 or 1-10/2
            String[] parts = field.split("/");
            int step = Integer.parseInt(parts[1]);
            
            int rangeMin = min;
            int rangeMax = max;
            
            if (!parts[0].equals("*")) {
                if (parts[0].contains("-")) {
                    String[] range = parts[0].split("-");
                    rangeMin = Integer.parseInt(range[0]);
                    rangeMax = Integer.parseInt(range[1]);
                } else {
                    rangeMin = Integer.parseInt(parts[0]);
                    rangeMax = max;
                }
            }
            
            int count = ((rangeMax - rangeMin) / step) + 1;
            int[] values = new int[count];
            for (int i = 0; i < count; i++) {
                values[i] = rangeMin + (i * step);
            }
            return values;
        }
        
        if (field.contains("-")) {
            // Range: 1-5
            String[] parts = field.split("-");
            int rangeMin = Integer.parseInt(parts[0]);
            int rangeMax = Integer.parseInt(parts[1]);
            int[] values = new int[rangeMax - rangeMin + 1];
            for (int i = 0; i < values.length; i++) {
                values[i] = rangeMin + i;
            }
            return values;
        }
        
        // Single value
        return new int[] { Integer.parseInt(field) };
    }
    
    /**
     * Calculate the next execution time from now
     */
    public long getNextExecutionTime(ZoneId timezone) {
        return getNextExecutionTime(ZonedDateTime.now(timezone));
    }
    
    /**
     * Calculate the next execution time from a given time
     */
    public long getNextExecutionTime(ZonedDateTime from) {
        ZonedDateTime next = from.truncatedTo(ChronoUnit.MINUTES).plusMinutes(1);
        
        // Find next matching time (max 4 years to prevent infinite loop)
        for (int i = 0; i < 525600 * 4; i++) { // 525600 minutes per year
            if (matches(next)) {
                return next.toInstant().toEpochMilli();
            }
            next = next.plusMinutes(1);
        }
        
        // Should never reach here unless cron expression is invalid
        LOGGER.error("Failed to find next execution time for cron: {}", cronExpression);
        return -1;
    }
    
    /**
     * Check if a given time matches the cron expression
     */
    public boolean matches(ZonedDateTime time) {
        int minute = time.getMinute();
        int hour = time.getHour();
        int dayOfMonth = time.getDayOfMonth();
        int month = time.getMonthValue();
        int dayOfWeek = time.getDayOfWeek().getValue() % 7; // Convert to 0=Sunday
        
        return contains(minutes, minute) &&
               contains(hours, hour) &&
               contains(daysOfMonth, dayOfMonth) &&
               contains(months, month) &&
               contains(daysOfWeek, dayOfWeek);
    }
    
    /**
     * Check if array contains value
     */
    private boolean contains(int[] array, int value) {
        for (int v : array) {
            if (v == value) return true;
        }
        return false;
    }
    
    /**
     * Validate cron expression format
     */
    public static boolean isValid(String cronExpression) {
        try {
            new CronParser(cronExpression);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * Get human-readable description of cron expression
     */
    public String getDescription() {
        StringBuilder desc = new StringBuilder();
        
        // Describe frequency
        if (isEveryMinute()) {
            desc.append("Every minute");
        } else if (isHourly()) {
            desc.append("Every hour at minute ").append(minutes[0]);
        } else if (isDaily()) {
            desc.append("Every day at ").append(formatTime(hours[0], minutes[0]));
        } else if (isWeekly()) {
            desc.append("Every ").append(getDayName(daysOfWeek[0]))
                .append(" at ").append(formatTime(hours[0], minutes[0]));
        } else if (isMonthly()) {
            desc.append("Every month on day ").append(daysOfMonth[0])
                .append(" at ").append(formatTime(hours[0], minutes[0]));
        } else {
            desc.append("Custom schedule: ").append(cronExpression);
        }
        
        return desc.toString();
    }
    
    private boolean isEveryMinute() {
        return minutes.length == 60 && hours.length == 24;
    }
    
    private boolean isHourly() {
        return minutes.length == 1 && hours.length == 24;
    }
    
    private boolean isDaily() {
        return minutes.length == 1 && hours.length == 1 && daysOfMonth.length == 31;
    }
    
    private boolean isWeekly() {
        return minutes.length == 1 && hours.length == 1 && daysOfWeek.length == 1;
    }
    
    private boolean isMonthly() {
        return minutes.length == 1 && hours.length == 1 && daysOfMonth.length == 1 && months.length == 12;
    }
    
    private String formatTime(int hour, int minute) {
        return String.format("%02d:%02d", hour, minute);
    }
    
    private String getDayName(int dayOfWeek) {
        String[] days = {"Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday"};
        return days[dayOfWeek];
    }
}
