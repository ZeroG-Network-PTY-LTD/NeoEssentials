package com.zerog.neoessentials.systems.security;

import java.util.Set;
import java.util.HashSet;

/**
 * Security configuration settings
 */
public class SecurityConfig {
    private boolean enabled = true;
    private boolean auditLoggingEnabled = true;
    private boolean threatDetectionEnabled = true;
    private boolean sessionManagementEnabled = true;
    private boolean bruteForceProtectionEnabled = true;
    private int maxLoginAttempts = 5;
    private long sessionTimeout = 3600000; // 1 hour in milliseconds
    private long loginCooldown = 300000; // 5 minutes in milliseconds
    private Set<String> trustedIPs = new HashSet<>();
    private Set<String> blockedIPs = new HashSet<>();
    private Set<String> adminUsers = new HashSet<>();
    private boolean requireStrongPasswords = true;
    private int passwordMinLength = 8;
    private boolean twoFactorAuthEnabled = false;
    private boolean ipWhitelistEnabled = false;
    private boolean autoBlockSuspiciousActivity = true;
    private double riskThreshold = 0.7;
    private long maxAuditLogSize = 100 * 1024 * 1024; // 100MB
    private int maxFailedLogins = 5;
    private boolean autoBlockSuspiciousIPs = true;
    private boolean securityReportsEnabled = true;
    
    public SecurityConfig() {
        // Default trusted IPs
        trustedIPs.add("127.0.0.1");
        trustedIPs.add("localhost");
    }
    
    // Getters and setters
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    
    public boolean isAuditLoggingEnabled() { return auditLoggingEnabled; }
    public void setAuditLoggingEnabled(boolean auditLoggingEnabled) { this.auditLoggingEnabled = auditLoggingEnabled; }
    
    public boolean isThreatDetectionEnabled() { return threatDetectionEnabled; }
    public void setThreatDetectionEnabled(boolean threatDetectionEnabled) { this.threatDetectionEnabled = threatDetectionEnabled; }
    
    public boolean isSessionManagementEnabled() { return sessionManagementEnabled; }
    public void setSessionManagementEnabled(boolean sessionManagementEnabled) { this.sessionManagementEnabled = sessionManagementEnabled; }
    
    public boolean isBruteForceProtectionEnabled() { return bruteForceProtectionEnabled; }
    public void setBruteForceProtectionEnabled(boolean bruteForceProtectionEnabled) { this.bruteForceProtectionEnabled = bruteForceProtectionEnabled; }
    
    public int getMaxLoginAttempts() { return maxLoginAttempts; }
    public void setMaxLoginAttempts(int maxLoginAttempts) { this.maxLoginAttempts = maxLoginAttempts; }
    
    public long getSessionTimeout() { return sessionTimeout; }
    public void setSessionTimeout(long sessionTimeout) { this.sessionTimeout = sessionTimeout; }
    
    public long getLoginCooldown() { return loginCooldown; }
    public void setLoginCooldown(long loginCooldown) { this.loginCooldown = loginCooldown; }
    
    public Set<String> getTrustedIPs() { return trustedIPs; }
    public void setTrustedIPs(Set<String> trustedIPs) { this.trustedIPs = trustedIPs; }
    
    public Set<String> getBlockedIPs() { return blockedIPs; }
    public void setBlockedIPs(Set<String> blockedIPs) { this.blockedIPs = blockedIPs; }
    
    public Set<String> getAdminUsers() { return adminUsers; }
    public void setAdminUsers(Set<String> adminUsers) { this.adminUsers = adminUsers; }
    
    public boolean isRequireStrongPasswords() { return requireStrongPasswords; }
    public void setRequireStrongPasswords(boolean requireStrongPasswords) { this.requireStrongPasswords = requireStrongPasswords; }
    
    public int getPasswordMinLength() { return passwordMinLength; }
    public void setPasswordMinLength(int passwordMinLength) { this.passwordMinLength = passwordMinLength; }
    
    public boolean isTwoFactorAuthEnabled() { return twoFactorAuthEnabled; }
    public void setTwoFactorAuthEnabled(boolean twoFactorAuthEnabled) { this.twoFactorAuthEnabled = twoFactorAuthEnabled; }
    
    public boolean isIpWhitelistEnabled() { return ipWhitelistEnabled; }
    public void setIpWhitelistEnabled(boolean ipWhitelistEnabled) { this.ipWhitelistEnabled = ipWhitelistEnabled; }
    
    public boolean isAutoBlockSuspiciousActivity() { return autoBlockSuspiciousActivity; }
    public void setAutoBlockSuspiciousActivity(boolean autoBlockSuspiciousActivity) { this.autoBlockSuspiciousActivity = autoBlockSuspiciousActivity; }
    
    public double getRiskThreshold() { return riskThreshold; }
    public void setRiskThreshold(double riskThreshold) { this.riskThreshold = riskThreshold; }
    
    public long getMaxAuditLogSize() { return maxAuditLogSize; }
    public void setMaxAuditLogSize(long maxAuditLogSize) { this.maxAuditLogSize = maxAuditLogSize; }
    
    public int getMaxFailedLogins() { return maxFailedLogins; }
    public void setMaxFailedLogins(int maxFailedLogins) { this.maxFailedLogins = maxFailedLogins; }
    
    public boolean isAutoBlockSuspiciousIPs() { return autoBlockSuspiciousIPs; }
    public void setAutoBlockSuspiciousIPs(boolean autoBlockSuspiciousIPs) { this.autoBlockSuspiciousIPs = autoBlockSuspiciousIPs; }
    
    public boolean isSecurityReportsEnabled() { return securityReportsEnabled; }
    public void setSecurityReportsEnabled(boolean securityReportsEnabled) { this.securityReportsEnabled = securityReportsEnabled; }
    
    // Alias methods for compatibility
    public void setAuditLogEnabled(boolean enabled) { setAuditLoggingEnabled(enabled); }
    public boolean isAuditLogEnabled() { return isAuditLoggingEnabled(); }
    
    public boolean isTrustedIP(String ip) {
        return trustedIPs.contains(ip);
    }
    
    public boolean isBlockedIP(String ip) {
        return blockedIPs.contains(ip);
    }
    
    public boolean isAdminUser(String username) {
        return adminUsers.contains(username);
    }
}
