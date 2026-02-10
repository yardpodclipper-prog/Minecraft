package com.yourname.gtstracker.database.models;

public class PriceStats {
    public double average;
    public int min;
    public int max;
    public double median;
    public int sampleSize;
    public long oldestSample;
    public long newestSample;

    public boolean hasEnoughData() {
        return sampleSize >= 5;
    }

    public double getPercentDiff(int price) {
        if (average == 0) {
            return 0;
        }
        return ((price - average) / average) * 100.0;
    }
}
