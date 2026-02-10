package com.yourname.gtstracker.config;

public class ConfigModel {
    public boolean chatMonitoringEnabled = true;
    public boolean autoScanOnGTSOpen = true;
    public int scanIntervalSeconds = 60;

    public double alertThresholdPercent = 20.0;
    public double spikeThresholdPercent = 50.0;

    public int dataRetentionDays = 90;
    public int minSamplesForAverage = 5;

    public boolean showOverlayOnGTS = true;
    public boolean bloombergTheme = true;

    public boolean isChatMonitoringEnabled() {
        return chatMonitoringEnabled;
    }

    public void setChatMonitoringEnabled(boolean chatMonitoringEnabled) {
        this.chatMonitoringEnabled = chatMonitoringEnabled;
    }
}
