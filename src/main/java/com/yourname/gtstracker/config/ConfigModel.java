package com.yourname.gtstracker.config;

/**
 * Runtime configuration for GTSTracker client features.
 */
public class ConfigModel {
    private boolean chatMonitoringEnabled = true;

    public boolean isChatMonitoringEnabled() {
        return chatMonitoringEnabled;
    }

    public void setChatMonitoringEnabled(boolean chatMonitoringEnabled) {
        this.chatMonitoringEnabled = chatMonitoringEnabled;
    }
}
