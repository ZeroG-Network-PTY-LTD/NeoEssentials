package com.zerog.neoessentials.analytics;

/**
 * Stub for dedicated analytics configuration class
 */
public class AnalyticsConfig {
    public boolean isEnabled() { return true; }
    public int getMaxEventHistory() { return 10000; }
    public long getSessionTimeoutMinutes() { return 30; }
}