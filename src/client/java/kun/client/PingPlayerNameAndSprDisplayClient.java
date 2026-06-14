// src/client/java/kun/client/PingPlayerNameAndSprDisplayClient.java
package kun.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;

import java.util.List;

@Environment(EnvType.CLIENT)
public class PingPlayerNameAndSprDisplayClient implements ClientModInitializer {
    private static long lastSampleTime = 0;
    private static long lastWarningTime = 0;
    private static boolean lastPingWarning = false;
    private static boolean lastUplinkWarning = false;
    private static boolean lastDownlinkWarning = false;

    @Override
    public void onInitializeClient() {
        // 初始化追踪器
        PacketTracker.init();
        BandwidthTracker.init();   // 内部可为空，但确保静态代码块执行

        // 注册 HUD 渲染
        HudRenderCallback.EVENT.register(new HudRenderer());
        // 注册热键
        KeyBindings.register();

        // 连接服务器时重置所有统计数据
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            PacketTracker.reset();
            BandwidthTracker.reset();
            lastSampleTime = 0;
            lastWarningTime = 0;
            lastPingWarning = false;
            lastUplinkWarning = false;
            lastDownlinkWarning = false;
            System.out.println("[PingDisplay] Connected to server, statistics reset.");
        });

        // 断开服务器时清空数据
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            PacketTracker.onDisconnect();
            BandwidthTracker.reset();
            System.out.println("[PingDisplay] Disconnected from server.");
        });

        // 每秒采样一次 Ping、SPR 和带宽
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player == null || client.getNetworkHandler() == null) return;
            long now = System.currentTimeMillis();
            if (now - lastSampleTime >= 1000) {
                // 获取 Ping 和 SPR
                int ping = getCurrentPing(client);
                double spr = PacketTracker.getCurrentSpr();
                PacketTracker.addSample(ping, spr);

                // 关键：每秒调用带宽统计采样（计算每秒增量并存储）
                BandwidthTracker.tickSample();

                lastSampleTime = now;

                // 卡顿检测（每15秒检查一次，避免聊天刷屏）
                if (now - lastWarningTime >= 15000) {
                    checkAndWarn(client);
                    lastWarningTime = now;
                }
            }
        });
    }

    /**
     * 获取当前玩家的 Ping（毫秒）
     */
    private static int getCurrentPing(MinecraftClient client) {
        if (client.getNetworkHandler() == null || client.player == null) return 0;
        var entry = client.getNetworkHandler().getPlayerListEntry(client.player.getUuid());
        return entry != null ? entry.getLatency() : 0;
    }

    /**
     * 检测卡顿条件并发送警告到聊天栏
     */
    private static void checkAndWarn(MinecraftClient client) {
        if (client.player == null) return;

        boolean pingHigh = isPingAbnormal();
        boolean uplinkLow = isBandwidthAbnormal(true);
        boolean downlinkLow = isBandwidthAbnormal(false);

        if (pingHigh && !lastPingWarning) {
            client.player.sendMessage(Text.literal("§c[PPSD]警告：Ping异常升高"), false);
            lastPingWarning = true;
        } else if (!pingHigh) {
            lastPingWarning = false;
        }

        if (uplinkLow && !lastUplinkWarning) {
            client.player.sendMessage(Text.literal("§c[PPSD]警告：带宽（Upl）异常降低"), false);
            lastUplinkWarning = true;
        } else if (!uplinkLow) {
            lastUplinkWarning = false;
        }

        if (downlinkLow && !lastDownlinkWarning) {
            client.player.sendMessage(Text.literal("§c[PPSD]警告：带宽（Dnl）异常降低"), false);
            lastDownlinkWarning = true;
        } else if (!downlinkLow) {
            lastDownlinkWarning = false;
        }
    }

    /**
     * 判断 Ping 是否异常：
     * 最近15秒内所有 Ping 值 > (全程平均 Ping + 500ms)
     */
    private static boolean isPingAbnormal() {
        List<PacketTracker.PingSample> allPings = PacketTracker.getAllPingSamples();
        if (allPings.isEmpty()) return false;
        double avgAll = allPings.stream().mapToInt(s -> s.value).average().orElse(0);
        List<PacketTracker.PingSample> recent = PacketTracker.getRecentPingSamples(15);
        if (recent.isEmpty()) return false;
        for (PacketTracker.PingSample s : recent) {
            if (s.value <= avgAll + 500) return false;
        }
        return true;
    }

    /**
     * 判断带宽是否异常：
     * 最近15秒内所有上行（或下行）带宽值 < (全程平均带宽 * 0.5)
     */
    private static boolean isBandwidthAbnormal(boolean isUplink) {
        if (isUplink) {
            List<BandwidthTracker.BandwidthSample> all = BandwidthTracker.getAllUplinkSamples();
            if (all.isEmpty()) return false;
            double avgAll = all.stream().mapToDouble(s -> s.value).average().orElse(0);
            List<BandwidthTracker.BandwidthSample> recent = BandwidthTracker.getRecentUplinkSamples(15);
            if (recent.isEmpty()) return false;
            double threshold = avgAll * 0.5;
            for (BandwidthTracker.BandwidthSample s : recent) {
                if (s.value >= threshold) return false;
            }
            return true;
        } else {
            List<BandwidthTracker.BandwidthSample> all = BandwidthTracker.getAllDownlinkSamples();
            if (all.isEmpty()) return false;
            double avgAll = all.stream().mapToDouble(s -> s.value).average().orElse(0);
            List<BandwidthTracker.BandwidthSample> recent = BandwidthTracker.getRecentDownlinkSamples(15);
            if (recent.isEmpty()) return false;
            double threshold = avgAll * 0.5;
            for (BandwidthTracker.BandwidthSample s : recent) {
                if (s.value >= threshold) return false;
            }
            return true;
        }
    }
}