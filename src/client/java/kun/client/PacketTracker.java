package kun.client;

import java.util.*;
import java.util.concurrent.ConcurrentLinkedDeque;

public class PacketTracker {
    private static final int MAX_RECENT_SAMPLES = 60;
    private static final Deque<PingSample> pingHistory = new ConcurrentLinkedDeque<>();
    private static final Deque<SprSample> sprHistory = new ConcurrentLinkedDeque<>();
    private static final List<PingSample> allPingHistory = new ArrayList<>();
    private static final List<SprSample> allSprHistory = new ArrayList<>();

    private static final Deque<Long> packetTimestampQueue = new ConcurrentLinkedDeque<>();
    private static double currentSpr = 0.0;
    private static long lastSampleTime = 0;
    private static boolean connected = false;

    public static void init() {}

    public static void onPacketReceived() {
        if (!connected) return;
        long now = System.currentTimeMillis();
        packetTimestampQueue.add(now);
        while (!packetTimestampQueue.isEmpty() && packetTimestampQueue.peekFirst() < now - 1000) {
            packetTimestampQueue.pollFirst();
        }
        currentSpr = packetTimestampQueue.size();
    }

    public static double getCurrentSpr() {
        return currentSpr;
    }

    public static void addSample(int ping, double spr) {
        long now = System.currentTimeMillis();
        pingHistory.add(new PingSample(now, ping));
        if (pingHistory.size() > MAX_RECENT_SAMPLES) pingHistory.pollFirst();
        sprHistory.add(new SprSample(now, spr));
        if (sprHistory.size() > MAX_RECENT_SAMPLES) sprHistory.pollFirst();
        allPingHistory.add(new PingSample(now, ping));
        allSprHistory.add(new SprSample(now, spr));
    }

    public static void reset() {
        pingHistory.clear();
        sprHistory.clear();
        allPingHistory.clear();
        allSprHistory.clear();
        packetTimestampQueue.clear();
        currentSpr = 0.0;
        connected = true;
        lastSampleTime = System.currentTimeMillis();
    }

    public static void onDisconnect() {
        connected = false;
        pingHistory.clear();
        sprHistory.clear();
        allPingHistory.clear();
        allSprHistory.clear();
        packetTimestampQueue.clear();
        currentSpr = 0.0;
    }

    public static long getLastSampleTime() { return lastSampleTime; }
    public static void updateLastSampleTime(long time) { lastSampleTime = time; }
    public static boolean isConnected() { return connected; }

    public static Stats calculatePingStats(Collection<PingSample> samples) {
        if (samples.isEmpty()) return new Stats(0, 0, 0);
        double sum = 0;
        double min = Double.MAX_VALUE, max = Double.MIN_VALUE;
        for (PingSample s : samples) {
            sum += s.value;
            min = Math.min(min, s.value);
            max = Math.max(max, s.value);
        }
        return new Stats(sum / samples.size(), min, max);
    }

    public static Stats calculateSprStats(Collection<SprSample> samples) {
        if (samples.isEmpty()) return new Stats(0, 0, 0);
        double sum = 0;
        double min = Double.MAX_VALUE, max = Double.MIN_VALUE;
        for (SprSample s : samples) {
            sum += s.value;
            min = Math.min(min, s.value);
            max = Math.max(max, s.value);
        }
        return new Stats(sum / samples.size(), min, max);
    }

    public static Collection<PingSample> getRecentPingSamples() { return pingHistory; }
    public static Collection<SprSample> getRecentSprSamples() { return sprHistory; }
    public static List<PingSample> getAllPingSamples() { return allPingHistory; }
    public static List<SprSample> getAllSprSamples() { return allSprHistory; }

    // 获取最近 n 秒的样本（用于卡顿检测）
    public static List<PingSample> getRecentPingSamples(int seconds) {
        List<PingSample> result = new ArrayList<>();
        long cutoff = System.currentTimeMillis() - seconds * 1000L;
        for (PingSample s : pingHistory) {
            if (s.timestamp >= cutoff) result.add(s);
        }
        return result;
    }

    public static List<SprSample> getRecentSprSamples(int seconds) {
        List<SprSample> result = new ArrayList<>();
        long cutoff = System.currentTimeMillis() - seconds * 1000L;
        for (SprSample s : sprHistory) {
            if (s.timestamp >= cutoff) result.add(s);
        }
        return result;
    }

    public static class PingSample {
        public final long timestamp;
        public final int value;
        public PingSample(long ts, int v) { timestamp = ts; value = v; }
    }

    public static class SprSample {
        public final long timestamp;
        public final double value;
        public SprSample(long ts, double v) { timestamp = ts; value = v; }
    }
    private static final Map<UUID, Integer> playerLatencies = new HashMap<>();

    public static void updatePlayerLatency(UUID playerId, int latency) {
        if (playerId != null) {
            playerLatencies.put(playerId, latency);
        }
    }

    public static int getCurrentPingForPlayer(UUID playerId) {
        return playerLatencies.getOrDefault(playerId, 0);
    }
    public static class Stats {
        public final double avg, max, min;
        public Stats(double avg, double min, double max) {
            this.avg = avg;
            this.min = min;
            this.max = max;
        }
    }
}