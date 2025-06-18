package com.zerog.neoessentials.utils;

import java.util.Calendar;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.text.SimpleDateFormat;

/**
 * Utility class for handling time and date operations
 */
public class TimeUtil {

    private static final Pattern TIME_PATTERN = Pattern.compile("(?:([0-9]+)\\s*y[a-z]*[,\\s]*)?" + "(?:([0-9]+)\\s*mo[a-z]*[,\\s]*)?" + "(?:([0-9]+)\\s*w[a-z]*[,\\s]*)?" + "(?:([0-9]+)\\s*d[a-z]*[,\\s]*)?" + "(?:([0-9]+)\\s*h[a-z]*[,\\s]*)?" + "(?:([0-9]+)\\s*m[a-z]*[,\\s]*)?" + "(?:([0-9]+)\\s*(?:s[a-z]*)?)?", Pattern.CASE_INSENSITIVE);
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("MMM d, yyyy HH:mm");
    
    /**
     * Parse a time specification like "1d2h30m" into a Date in the future
     * 
     * @param timeSpec String time specification
     * @return Date when the time will expire
     * @throws IllegalArgumentException if timeSpec is invalid
     */
    public static Date parseTimeSpecification(String timeSpec) {
        Matcher matcher = TIME_PATTERN.matcher(timeSpec);
        if (!matcher.matches()) {
            throw new IllegalArgumentException("Invalid time format");
        }
        
        Calendar calendar = Calendar.getInstance();
        boolean found = false;
        
        // Years
        String years = matcher.group(1);
        if (years != null && !years.isEmpty()) {
            calendar.add(Calendar.YEAR, Integer.parseInt(years));
            found = true;
        }
        
        // Months
        String months = matcher.group(2);
        if (months != null && !months.isEmpty()) {
            calendar.add(Calendar.MONTH, Integer.parseInt(months));
            found = true;
        }
        
        // Weeks
        String weeks = matcher.group(3);
        if (weeks != null && !weeks.isEmpty()) {
            calendar.add(Calendar.WEEK_OF_YEAR, Integer.parseInt(weeks));
            found = true;
        }
        
        // Days
        String days = matcher.group(4);
        if (days != null && !days.isEmpty()) {
            calendar.add(Calendar.DAY_OF_MONTH, Integer.parseInt(days));
            found = true;
        }
        
        // Hours
        String hours = matcher.group(5);
        if (hours != null && !hours.isEmpty()) {
            calendar.add(Calendar.HOUR_OF_DAY, Integer.parseInt(hours));
            found = true;
        }
        
        // Minutes
        String minutes = matcher.group(6);
        if (minutes != null && !minutes.isEmpty()) {
            calendar.add(Calendar.MINUTE, Integer.parseInt(minutes));
            found = true;
        }
        
        // Seconds
        String seconds = matcher.group(7);
        if (seconds != null && !seconds.isEmpty()) {
            calendar.add(Calendar.SECOND, Integer.parseInt(seconds));
            found = true;
        }
        
        if (!found) {
            throw new IllegalArgumentException("Invalid time format");
        }
        
        return calendar.getTime();
    }
    
    /**
<<<<<<< HEAD
     * Parse a time specification like "1d2h30m" into a Date in the future
     * 
     * @param timeSpec String time specification
     * @return Date object representing the time in the future
     */
    public static Date parseTimeToDate(String timeSpec) {
        Calendar cal = Calendar.getInstance();
        Matcher matcher = TIME_PATTERN.matcher(timeSpec);
        
        long totalMillis = 0;
        boolean found = false;
        
        while (matcher.find()) {
            if (matcher.group() == null || matcher.group().isEmpty()) {
                continue;
            }
            found = true;
            
            for (int i = 0; i < matcher.groupCount(); i++) {
                String val = matcher.group(i + 1);
                if (val == null || val.isEmpty()) continue;
                
                int amount = Integer.parseInt(val);
                switch (i) {
                    case 0: totalMillis += amount * 31536000000L; break; // years
                    case 1: totalMillis += amount * 2592000000L; break;  // months
                    case 2: totalMillis += amount * 604800000L; break;   // weeks
                    case 3: totalMillis += amount * 86400000L; break;    // days
                    case 4: totalMillis += amount * 3600000L; break;     // hours
                    case 5: totalMillis += amount * 60000L; break;       // minutes
                    case 6: totalMillis += amount * 1000L; break;        // seconds
                }
            }
        }
        
        if (!found) {
            return null;
        }
        
        cal.setTimeInMillis(cal.getTimeInMillis() + totalMillis);
        return cal.getTime();
    }
    
    /**
=======
>>>>>>> 1fb47d4 (Implement messaging and moderation commands, add time utility for duration parsing)
     * Format a date for display
     * 
     * @param date Date to format
     * @return Formatted date string
     */
    public static String formatDate(Date date) {
        return DATE_FORMAT.format(date);
    }
    
    /**
     * Get the time remaining until a specific date
     * 
     * @param date Future date
     * @return Human-readable time remaining string
     */
    public static String getTimeRemaining(Date date) {
        if (date == null) {
            return "forever";
        }
        
        long diff = date.getTime() - new Date().getTime();
        if (diff <= 0) {
            return "expired";
        }
        
        long diffSeconds = diff / 1000 % 60;
        long diffMinutes = diff / (60 * 1000) % 60;
        long diffHours = diff / (60 * 60 * 1000) % 24;
        long diffDays = diff / (24 * 60 * 60 * 1000);
        
        StringBuilder sb = new StringBuilder();
        if (diffDays > 0) {
            sb.append(diffDays).append(" day").append(diffDays > 1 ? "s" : "").append(" ");
        }
        
        if (diffHours > 0) {
            sb.append(diffHours).append(" hour").append(diffHours > 1 ? "s" : "").append(" ");
        }
        
        if (diffMinutes > 0) {
            sb.append(diffMinutes).append(" minute").append(diffMinutes > 1 ? "s" : "").append(" ");
        }
        
        if (diffSeconds > 0 && diffDays == 0 && diffHours == 0) {
            sb.append(diffSeconds).append(" second").append(diffSeconds > 1 ? "s" : "").append(" ");
        }
        
        return sb.toString().trim();
    }
<<<<<<< HEAD
<<<<<<< HEAD
    
    /**
     * Format a duration in milliseconds into a human-readable string
     * 
     * @param durationMillis Duration in milliseconds
     * @return Human readable duration string
     */
    public static String formatDuration(long durationMillis) {
        if (durationMillis < 0) {
            return "permanently";
        }
        
        long seconds = durationMillis / 1000;
        if (seconds <= 0) {
            return "0 seconds";
        }
        
        long minutes = seconds / 60;
        seconds = seconds % 60;
        long hours = minutes / 60;
        minutes = minutes % 60;
        long days = hours / 24;
        hours = hours % 24;
        long weeks = days / 7;
        days = days % 7;
        long months = weeks / 4;
        weeks = weeks % 4;
        long years = months / 12;
        months = months % 12;
        
        StringBuilder sb = new StringBuilder();
        if (years > 0) sb.append(years).append(" year").append(years > 1 ? "s" : "").append(" ");
        if (months > 0) sb.append(months).append(" month").append(months > 1 ? "s" : "").append(" ");
        if (weeks > 0) sb.append(weeks).append(" week").append(weeks > 1 ? "s" : "").append(" ");
        if (days > 0) sb.append(days).append(" day").append(days > 1 ? "s" : "").append(" ");
        if (hours > 0) sb.append(hours).append(" hour").append(hours > 1 ? "s" : "").append(" ");
        if (minutes > 0) sb.append(minutes).append(" minute").append(minutes > 1 ? "s" : "").append(" ");
        if (seconds > 0) sb.append(seconds).append(" second").append(seconds > 1 ? "s" : "");
        
        return sb.toString().trim();
    }
    
    /**
     * Alias for formatDuration for backward compatibility
     * 
     * @param durationMillis Duration in milliseconds
     * @return Human readable duration string
     */
    public static String formatTimeDuration(long durationMillis) {
        return formatDuration(durationMillis);
    }
=======
>>>>>>> 1fb47d4 (Implement messaging and moderation commands, add time utility for duration parsing)
=======
    
    /**
     * Format a time duration in seconds to a human-readable string
     * 
     * @param seconds Duration in seconds
     * @return Human-readable duration string
     */
    public static String formatTimeDuration(long seconds) {
        if (seconds < 0) {
            return "forever";
        }
        
        long minutes = seconds / 60;
        long hours = minutes / 60;
        long days = hours / 24;
        
        seconds = seconds % 60;
        minutes = minutes % 60;
        hours = hours % 24;
        
        StringBuilder sb = new StringBuilder();
        if (days > 0) {
            sb.append(days).append(" day").append(days > 1 ? "s" : "").append(" ");
        }
        
        if (hours > 0) {
            sb.append(hours).append(" hour").append(hours > 1 ? "s" : "").append(" ");
        }
        
        if (minutes > 0) {
            sb.append(minutes).append(" minute").append(minutes > 1 ? "s" : "").append(" ");
        }
        
        if (seconds > 0 && days == 0 && hours == 0) {
            sb.append(seconds).append(" second").append(seconds > 1 ? "s" : "").append(" ");
        }
        
        String result = sb.toString().trim();
        return result.isEmpty() ? "0 seconds" : result;
    }
>>>>>>> 2b0efb3 (Implement powertool and jail management systems)
}
