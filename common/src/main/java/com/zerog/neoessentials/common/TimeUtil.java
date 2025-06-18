package com.zerog.neoessentials.common;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Time utility functions that are shared across all versions
 */
public class TimeUtil {

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
    
    /**
     * Format a date into a standard format string
     * 
     * @param date The date to format
     * @return The formatted date string
     */
    public static String formatDate(Date date) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        return sdf.format(date);
    }
    
    /**
     * Parse a date string in the standard format
     * 
     * @param dateStr The date string to parse
     * @return The parsed Date
     * @throws ParseException If the date string is invalid
     */
    public static Date parseDate(String dateStr) throws ParseException {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        return sdf.parse(dateStr);
    }
}
