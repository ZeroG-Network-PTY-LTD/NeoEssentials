package com.zerog.neoessentials.data;

import java.util.List;

/**
 * Plugin Compatibility Report
 */
public class PluginCompatibilityReport {
    private final int totalPlugins;
    private final int integratedPlugins;
    private final int failedPlugins;
    private final List<String> issues;
    
    public PluginCompatibilityReport(int totalPlugins, int integratedPlugins, int failedPlugins, List<String> issues) {
        this.totalPlugins = totalPlugins;
        this.integratedPlugins = integratedPlugins;
        this.failedPlugins = failedPlugins;
        this.issues = issues;
    }
    
    public int getTotalPlugins() { return totalPlugins; }
    public int getIntegratedPlugins() { return integratedPlugins; }
    public int getFailedPlugins() { return failedPlugins; }
    public List<String> getIssues() { return issues; }
}
