package com.zerog.neoessentials.util;

import com.zerog.neoessentials.config.TablistConfig;

/**
 * Utility to validate and debug TabList configuration issues
 */
public class TabListConfigValidator {
    
    public static void validateConfig(TablistConfig config, String source) {
        DebugUtil.debugLog("[TabListValidator] Validating config from: " + source);
        
        if (config == null) {
            DebugUtil.errorLog("[TabListValidator] Config is null!");
            return;
        }
        
        if (config.tablist == null) {
            DebugUtil.errorLog("[TabListValidator] Config.tablist is null!");
            return;
        }
        
        DebugUtil.debugLog("[TabListValidator] Config.tablist.enabled: " + config.tablist.enabled);
        DebugUtil.debugLog("[TabListValidator] Config.tablist.updateInterval: " + config.tablist.updateInterval);
        
        // Validate layouts
        if (config.tablist.layouts == null) {
            DebugUtil.errorLog("[TabListValidator] Config.tablist.layouts is null!");
        } else if (config.tablist.layouts.isEmpty()) {
            DebugUtil.warnLog("[TabListValidator] Config.tablist.layouts is empty!");
        } else {
            DebugUtil.debugLog("[TabListValidator] Found " + config.tablist.layouts.size() + " layouts:");
            for (var entry : config.tablist.layouts.entrySet()) {
                var layout = entry.getValue();
                String layoutInfo = String.format("  - %s: priority=%d, condition=%s, headers=%d, footers=%d",
                    entry.getKey(),
                    layout.priority,
                    layout.conditionType != null ? layout.conditionType : "null",
                    layout.header != null ? layout.header.size() : 0,
                    layout.footer != null ? layout.footer.size() : 0);
                DebugUtil.debugLog("[TabListValidator] " + layoutInfo);
                
                // Validate layout content
                if (layout.header != null && !layout.header.isEmpty()) {
                    DebugUtil.debugLog("[TabListValidator]     Header sample: " + layout.header.get(0));
                }
                if (layout.footer != null && !layout.footer.isEmpty()) {
                    DebugUtil.debugLog("[TabListValidator]     Footer sample: " + layout.footer.get(0));
                }
            }
        }
        
        // Validate permission sets
        if (config.tablist.permissionSets == null) {
            DebugUtil.errorLog("[TabListValidator] Config.tablist.permissionSets is null!");
        } else if (config.tablist.permissionSets.isEmpty()) {
            DebugUtil.warnLog("[TabListValidator] Config.tablist.permissionSets is empty!");
        } else {
            DebugUtil.debugLog("[TabListValidator] Found " + config.tablist.permissionSets.size() + " permission sets:");
            for (var entry : config.tablist.permissionSets.entrySet()) {
                var permSet = entry.getValue();
                String permInfo = String.format("  - %s: priority=%d, layoutId=%s, permission=%s",
                    entry.getKey(),
                    permSet.priority,
                    permSet.layoutId != null ? permSet.layoutId : "null",
                    permSet.permission != null ? permSet.permission : "none");
                DebugUtil.debugLog("[TabListValidator] " + permInfo);
                
                // Check if referenced layout exists
                if (permSet.layoutId != null && config.tablist.layouts != null) {
                    if (config.tablist.layouts.containsKey(permSet.layoutId)) {
                        DebugUtil.debugLog("[TabListValidator]     Layout '" + permSet.layoutId + "' exists ✓");
                    } else {
                        DebugUtil.errorLog("[TabListValidator]     Layout '" + permSet.layoutId + "' NOT FOUND! ✗");
                    }
                }
            }
        }
        
        // Check for default permission set
        if (config.tablist.permissionSets != null && config.tablist.permissionSets.containsKey("default")) {
            DebugUtil.debugLog("[TabListValidator] Default permission set found ✓");
        } else {
            DebugUtil.warnLog("[TabListValidator] No 'default' permission set found - players without specific permissions may not get tablist");
        }
        
        // Check for default_layout
        if (config.tablist.layouts != null && config.tablist.layouts.containsKey("default_layout")) {
            DebugUtil.debugLog("[TabListValidator] Default layout found ✓");
        } else {
            DebugUtil.warnLog("[TabListValidator] No 'default_layout' found - fallback may not work properly");
        }
        
        DebugUtil.debugLog("[TabListValidator] Validation complete for: " + source);
    }
    
    public static void logConfigSummary(TablistConfig config) {
        if (config == null || config.tablist == null) {
            DebugUtil.errorLog("[TabListValidator] Cannot log summary - config is null or incomplete");
            return;
        }
        
        int layoutCount = config.tablist.layouts != null ? config.tablist.layouts.size() : 0;
        int permSetCount = config.tablist.permissionSets != null ? config.tablist.permissionSets.size() : 0;
        
        DebugUtil.debugLog("[TabListValidator] === CONFIG SUMMARY ===");
        DebugUtil.debugLog("[TabListValidator] Enabled: " + config.tablist.enabled);
        DebugUtil.debugLog("[TabListValidator] Layouts: " + layoutCount);
        DebugUtil.debugLog("[TabListValidator] Permission Sets: " + permSetCount);
        DebugUtil.debugLog("[TabListValidator] Update Interval: " + config.tablist.updateInterval);
        DebugUtil.debugLog("[TabListValidator] === END SUMMARY ===");
    }
}
