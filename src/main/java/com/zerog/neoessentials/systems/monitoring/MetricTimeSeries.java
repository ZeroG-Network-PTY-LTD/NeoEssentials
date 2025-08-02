package com.zerog.neoessentials.systems.monitoring;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Time series data for metrics
 */
public class MetricTimeSeries {
    private final String name;
    private final List<DataPoint> dataPoints = new CopyOnWriteArrayList<>();
    
    public MetricTimeSeries(String name) {
        this.name = name;
    }
    
    public void addDataPoint(double value, LocalDateTime timestamp) {
        dataPoints.add(new DataPoint(value, timestamp));
        
        // Keep only last 1000 data points to prevent memory issues
        if (dataPoints.size() > 1000) {
            dataPoints.remove(0);
        }
    }
    
    public void addDataPoint(double value) {
        addDataPoint(value, LocalDateTime.now());
    }
    
    public List<DataPoint> getDataPoints() {
        return List.copyOf(dataPoints);
    }
    
    public String getName() {
        return name;
    }
    
    public double getLatestValue() {
        return dataPoints.isEmpty() ? 0.0 : dataPoints.get(dataPoints.size() - 1).value;
    }
    
    public double getAverageValue() {
        return dataPoints.stream().mapToDouble(dp -> dp.value).average().orElse(0.0);
    }
    
    public double getMaxValue() {
        return dataPoints.stream().mapToDouble(dp -> dp.value).max().orElse(0.0);
    }
    
    public double getMinValue() {
        return dataPoints.stream().mapToDouble(dp -> dp.value).min().orElse(0.0);
    }
    
    public void clear() {
        dataPoints.clear();
    }
    
    public int size() {
        return dataPoints.size();
    }
    
    public static class DataPoint {
        public final double value;
        public final LocalDateTime timestamp;
        
        public DataPoint(double value, LocalDateTime timestamp) {
            this.value = value;
            this.timestamp = timestamp;
        }
        
        @Override
        public String toString() {
            return String.format("DataPoint{value=%.2f, timestamp=%s}", value, timestamp);
        }
    }
}
