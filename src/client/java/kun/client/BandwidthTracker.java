// src/client/java/kun/client/BandwidthTracker.java
package kun.client;

import java.util.*;
import java.util.concurrent.ConcurrentLinkedDeque;

public class BandwidthTracker {
    private static final int MAX_RECENT_SAMPLES = 60;
    private static final Deque<BandwidthSample> uplinkHistory = new ConcurrentLinkedDeque<>();
    private static final Deque<BandwidthSample> downlinkHistory = new ConcurrentLinkedDeque<>();
    private static final List<BandwidthSample> allUplinkHistory = new ArrayList<>();
    private static final List<BandwidthSample> allDownlinkHistory = new ArrayList<>();

    private static long lastUplinkBytes = 0;
    private static long lastDownlinkBytes = 0;
    private static double currentUplinkKib = 0;
    private static double currentDownlinkKib = 0;

    // 由 ChannelDuplexHandler 累计字节数（需要支持外部获取并重置）
    private static long totalUplinkBytes = 0;
    private static long totalDownlinkBytes = 0;

    public static void addUplinkBytes(long bytes) {
        totalUplinkBytes += bytes;
    }

    public static void addDownlinkBytes(long bytes) {
        totalDownlinkBytes += bytes;
    }

    // 每秒调用：计算增量并存储样本
    public static void tickSample() {
        long now = System.currentTimeMillis();
        long uplinkDelta = totalUplinkBytes - lastUplinkBytes;
        long downlinkDelta = totalDownlinkBytes - lastDownlinkBytes;
        lastUplinkBytes = totalUplinkBytes;
        lastDownlinkBytes = totalDownlinkBytes;

        double uplinkKib = uplinkDelta / 1024.0;
        double downlinkKib = downlinkDelta / 1024.0;
        currentUplinkKib = uplinkKib;
        currentDownlinkKib = downlinkKib;

        BandwidthSample uplinkSample = new BandwidthSample(now, uplinkKib);
        BandwidthSample downlinkSample = new BandwidthSample(now, downlinkKib);

        uplinkHistory.add(uplinkSample);
        if (uplinkHistory.size() > MAX_RECENT_SAMPLES) uplinkHistory.pollFirst();
        downlinkHistory.add(downlinkSample);
        if (downlinkHistory.size() > MAX_RECENT_SAMPLES) downlinkHistory.pollFirst();

        allUplinkHistory.add(uplinkSample);
        allDownlinkHistory.add(downlinkSample);
    }

    public static void reset() {
        uplinkHistory.clear();
        downlinkHistory.clear();
        allUplinkHistory.clear();
        allDownlinkHistory.clear();
        totalUplinkBytes = 0;
        totalDownlinkBytes = 0;
        lastUplinkBytes = 0;
        lastDownlinkBytes = 0;
        currentUplinkKib = 0;
        currentDownlinkKib = 0;
    }

    public static double getCurrentUplink() { return currentUplinkKib; }
    public static double getCurrentDownlink() { return currentDownlinkKib; }

    public static Collection<BandwidthSample> getRecentUplinkSamples() { return uplinkHistory; }
    public static Collection<BandwidthSample> getRecentDownlinkSamples() { return downlinkHistory; }
    public static List<BandwidthSample> getAllUplinkSamples() { return allUplinkHistory; }
    public static List<BandwidthSample> getAllDownlinkSamples() { return allDownlinkHistory; }

    public static List<BandwidthSample> getRecentUplinkSamples(int seconds) {
        List<BandwidthSample> result = new ArrayList<>();
        long cutoff = System.currentTimeMillis() - seconds * 1000L;
        for (BandwidthSample s : uplinkHistory) {
            if (s.timestamp >= cutoff) result.add(s);
        }
        return result;
    }

    public static List<BandwidthSample> getRecentDownlinkSamples(int seconds) {
        List<BandwidthSample> result = new ArrayList<>();
        long cutoff = System.currentTimeMillis() - seconds * 1000L;
        for (BandwidthSample s : downlinkHistory) {
            if (s.timestamp >= cutoff) result.add(s);
        }
        return result;
    }

    public static BandwidthStats calculateStats(Collection<BandwidthSample> samples) {
        if (samples.isEmpty()) return new BandwidthStats(0, 0, 0);
        double sum = 0, min = Double.MAX_VALUE, max = Double.MIN_VALUE;
        for (BandwidthSample s : samples) {
            sum += s.value;
            min = Math.min(min, s.value);
            max = Math.max(max, s.value);
        }
        return new BandwidthStats(sum / samples.size(), min, max);
    }

    public static void init() { }

    public static class BandwidthSample {
        public final long timestamp;
        public final double value;
        public BandwidthSample(long ts, double v) { timestamp = ts; value = v; }
    }

    public static class BandwidthStats {
        public final double avg, min, max;
        public BandwidthStats(double avg, double min, double max) {
            this.avg = avg; this.min = min; this.max = max;
        }
    }
}