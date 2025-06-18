package com.zerog.neoessentials.common.utils;

import java.util.Calendar;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.text.SimpleDateFormat;

/**
 * Utility class for handling time and date operations.
 * Version-independent implementation.
 */
public class CommonTimeUtil {

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
    
    /**
     * Convert a time string like "1d2h3m" to seconds
     * Supports years (y), weeks (w), days (d), hours (h), minutes (m), seconds (s)
     * 
     * @param timeStr The time string to parse
     * @return The time in seconds
     * @throws IllegalArgumentException If the time string is invalid
     */
    public static long parseTimeStringToSeconds(String timeStr) {
        if (timeStr == null || timeStr.isEmpty()) {
            throw new IllegalArgumentException("Time string cannot be empty");
        }
        
        long seconds = 0;
        Pattern pattern = Pattern.compile("(\\d+)([ywdhms])");
        Matcher matcher = pattern.matcher(timeStr.toLowerCase());
        
        boolean foundMatch = false;
        while (matcher.find()) {
            foundMatch = true;
            int amount = Integer.parseInt(matcher.group(1));
            String unit = matcher.group(2);
            
            switch (unit) {
                case "y":
                    seconds += amount * 31536000L; // 365 days
                    break;
                case "w":
                    seconds += amount * 604800L; // 7 days
                    break;
                case "d":
                    seconds += amount * 86400L; // 24 hours
                    break;
                case "h":
                    seconds += amount * 3600L;
                    break;
                case "m":
                    seconds += amount * 60L;
                    break;
                case "s":
                    seconds += amount;
                    break;
            }
        }
        
        if (!foundMatch) {
            try {
                // Try to parse as a number of seconds
                seconds = Long.parseLong(timeStr);
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Invalid time format: " + timeStr);
            }
        }
        
        return seconds;
    }
    
    /**
     * Format seconds into a human readable string (e.g., "1d 2h 3m 4s")
     * 
     * @param seconds The seconds to format
     * @return A human readable time string
     */
    public static String formatSecondsToTimeString(long seconds) {
        if (seconds <= 0) {
            return "0s";
        }
        
        long years = seconds / 31536000;
        seconds %= 31536000;
        
        long weeks = seconds / 604800;
        seconds %= 604800;
        
        long days = seconds / 86400;
        seconds %= 86400;
        
        long hours = seconds / 3600;
        seconds %= 3600;
        
        long minutes = seconds / 60;
        seconds %= 60;
        
        StringBuilder sb = new StringBuilder();
        
        if (years > 0) {
            sb.append(years).append("y ");
        }
        
        if (weeks > 0) {
            sb.append(weeks).append("w ");
        }
        
        if (days > 0) {
            sb.append(days).append("d ");
        }
        
        if (hours > 0) {
            sb.append(hours).append("h ");
        }
        
        if (minutes > 0) {
            sb.append(minutes).append("m ");
        }
        
        if (seconds > 0 || sb.length() == 0) {
            sb.append(seconds).append("s");
        } else {
            // Remove the trailing space if we have other units
            sb.deleteCharAt(sb.length() - 1);
        }
        
        return sb.toString();
    }
}
