package com.zerog.neoessentials.webdashboard;

/**
 * Alias for DashboardAPI to maintain backward compatibility
 * @deprecated Use {@link DashboardAPI} instead
 */
@Deprecated
public class WebDashboardServer {
    
    private final DashboardAPI api;
    
    private WebDashboardServer() {
        this.api = DashboardAPI.getInstance();
    }
    
    /**
     * Get the singleton instance
     * @return the WebDashboardServer instance
     */
    public static WebDashboardServer getInstance() {
        return InstanceHolder.INSTANCE;
    }
    
    private static class InstanceHolder {
        private static final WebDashboardServer INSTANCE = new WebDashboardServer();
    }
    
    public boolean isRunning() {
        return api.isRunning();
    }
    
    public void start() {
        api.start();
    }
    
    public void stop() {
        api.stop();
    }
    
    public int getPort() {
        return api.getPort();
    }
}
