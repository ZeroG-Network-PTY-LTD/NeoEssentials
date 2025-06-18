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
}
