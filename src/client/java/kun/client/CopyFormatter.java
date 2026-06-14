// src/client/java/kun/client/CopyFormatter.java
package kun.client;

import net.minecraft.client.MinecraftClient;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
//import java.util.List;

public class CopyFormatter {

    private static final DateTimeFormatter SIMPLE_TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm:ss");
    private static final DateTimeFormatter DETAILED_TIME_FORMAT = DateTimeFormatter.ofPattern("HHmmss");

    /**
     * 简洁复制格式
     */
    public static String formatSimple(MinecraftClient client) {
        int ping = getCurrentPing(client);
        double spr = PacketTracker.getCurrentSpr();
        double uplink = BandwidthTracker.getCurrentUplink();
        double downlink = BandwidthTracker.getCurrentDownlink();
        String playerName = client.player != null ? client.player.getName().getString() : "Unknown";
        String time = LocalTime.now().format(SIMPLE_TIME_FORMAT);

        return String.format("Ping: %d ms  SPR: %.1f pkt/s  Bandwidth: Upl: %.1f KiB/s | Dnl: %.1f KiB/s  Player Name: %s  Time: %s",
                ping, spr, uplink, downlink, playerName, time);
    }

    /**
     * 详细复制格式
     */
    public static String formatDetailed(MinecraftClient client) {
        String serverAddress = client.getCurrentServerEntry() != null ?
                client.getCurrentServerEntry().address : "Unknown";
        String playerName = client.player != null ? client.player.getName().getString() : "Unknown";
        String time = LocalTime.now().format(DETAILED_TIME_FORMAT);

        int pingNow = getCurrentPing(client);
        double sprNow = PacketTracker.getCurrentSpr();
        double uplinkNow = BandwidthTracker.getCurrentUplink();
        double downlinkNow = BandwidthTracker.getCurrentDownlink();

        // 统计 Ping
        PacketTracker.Stats pingRecent = PacketTracker.calculatePingStats(PacketTracker.getRecentPingSamples());
        PacketTracker.Stats pingAll = PacketTracker.calculatePingStats(PacketTracker.getAllPingSamples());

        // 统计 SPR
        PacketTracker.Stats sprRecent = PacketTracker.calculateSprStats(PacketTracker.getRecentSprSamples());
        PacketTracker.Stats sprAll = PacketTracker.calculateSprStats(PacketTracker.getAllSprSamples());

        // 统计带宽 (上行)
        BandwidthTracker.BandwidthStats uplinkRecent = BandwidthTracker.calculateStats(BandwidthTracker.getRecentUplinkSamples());
        BandwidthTracker.BandwidthStats uplinkAll = BandwidthTracker.calculateStats(BandwidthTracker.getAllUplinkSamples());

        // 统计带宽 (下行)
        BandwidthTracker.BandwidthStats downlinkRecent = BandwidthTracker.calculateStats(BandwidthTracker.getRecentDownlinkSamples());
        BandwidthTracker.BandwidthStats downlinkAll = BandwidthTracker.calculateStats(BandwidthTracker.getAllDownlinkSamples());

        return String.format("""
                        Server: %s
                        Player Name: %s
                        Time: %s
                        Ping:
                            Now %d ms
                            In 1 minute
                                avg %.1f ms max %.0f ms min %.0f ms
                            Since I joined the server
                                avg %.1f ms max %.0f ms min %.0f ms
                        SPR:
                            Now %.1f pkt/s
                            In 1 minute
                                avg %.1f pkt/s max %.1f pkt/s min %.1f pkt/s
                            Since I joined the server
                                avg %.1f pkt/s max %.1f pkt/s min %.1f pkt/s
                        Bandwidth:
                            Now Upl: %.1f KiB/s | Dnl: %.1f KiB/s
                            In 1 minute
                                avg Upl: %.1f KiB/s | Dnl: %.1f KiB/s
                                max Upl: %.1f KiB/s | Dnl: %.1f KiB/s
                                min Upl: %.1f KiB/s | Dnl: %.1f KiB/s
                            Since I joined the server
                                avg Upl: %.1f KiB/s | Dnl: %.1f KiB/s
                                max Upl: %.1f KiB/s | Dnl: %.1f KiB/s
                                min Upl: %.1f KiB/s | Dnl: %.1f KiB/s""",
                serverAddress, playerName, time,
                pingNow,
                pingRecent.avg, pingRecent.max, pingRecent.min,
                pingAll.avg, pingAll.max, pingAll.min,
                sprNow,
                sprRecent.avg, sprRecent.max, sprRecent.min,
                sprAll.avg, sprAll.max, sprAll.min,
                uplinkNow, downlinkNow,
                uplinkRecent.avg, downlinkRecent.avg,
                uplinkRecent.max, downlinkRecent.max,
                uplinkRecent.min, downlinkRecent.min,
                uplinkAll.avg, downlinkAll.avg,
                uplinkAll.max, downlinkAll.max,
                uplinkAll.min, downlinkAll.min);
    }

    private static int getCurrentPing(MinecraftClient client) {
        if (client.getNetworkHandler() == null || client.player == null) return 0;
        var entry = client.getNetworkHandler().getPlayerListEntry(client.player.getUuid());
        return entry != null ? entry.getLatency() : 0;
    }
}