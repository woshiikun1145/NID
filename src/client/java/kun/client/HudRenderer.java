// src/client/java/kun/client/HudRenderer.java
package kun.client;

import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.util.Colors;
import org.jetbrains.annotations.NotNull;
import kun.client.BandwidthTracker;


@SuppressWarnings("deprecation")
public class HudRenderer implements HudRenderCallback {
    //ping阈值
    private static final int PING_WHITE = 150;
    private static final int PING_YELLOW = 300;
    private static final int PING_ORANGE = 1000;
    //SPR阈值
    private static final double SPR_RED = 10.0;
    private static final double SPR_ORANGE = 30.0;
    private static final double SPR_YELLOW = 100.0;

    // 方块尺寸
    private static final int BAR_WIDTH = 2;
    private static final int BAR_HEIGHT_LOW = 2;
    private static final int BAR_HEIGHT_MID = 4;
    private static final int BAR_HEIGHT_HIGH = 6;
    private static final int BAR_SPACING = 0;

    // 方块Y轴偏移（正值向下移动，使方块相对于文字更靠下）
    private static final int BAR_Y_OFFSET = 7;

    @Override
    public void onHudRender(@NotNull DrawContext drawContext, RenderTickCounter tickCounter) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.getNetworkHandler() == null) return;
        if (!PacketTracker.isConnected()) return;

        int ping = getCurrentPing(client);
        double spr = PacketTracker.getCurrentSpr();
        String playerName = client.player.getName().getString();

        int pingColor = getPingColor(ping);
        int sprColor = getSprColor(spr);
        int idColor = Colors.WHITE;

        String pingText = String.format(" %d ms", ping);
        String sprText = String.format("SPR: %.1f pkt/s", spr);
        String idText = String.format("ID: %s", playerName);

        TextRenderer tr = client.textRenderer;
        int textY = client.getWindow().getScaledHeight() - tr.fontHeight - 5; // 文字基线
        int currentX = 5;

        // 绘制三个竖条，底部相对于文字基线向下偏移 BAR_Y_OFFSET
        int barColor = getPingColor(ping);

        int barBottomY = textY + BAR_Y_OFFSET;
        drawBar(drawContext, currentX, barBottomY, BAR_HEIGHT_LOW, barColor);
        currentX += BAR_WIDTH + BAR_SPACING;
        drawBar(drawContext, currentX, barBottomY, BAR_HEIGHT_MID, barColor);
        currentX += BAR_WIDTH + BAR_SPACING;
        drawBar(drawContext, currentX, barBottomY, BAR_HEIGHT_HIGH, barColor);
        currentX += BAR_WIDTH + BAR_SPACING + 2;

        // 绘制文字
        double uplink = BandwidthTracker.getCurrentUplink();
        double downlink = BandwidthTracker.getCurrentDownlink();

        String bandwidthText = String.format("Bandwidth: Upl: %.1f KiB/s | Dnl: %.1f KiB/s", uplink, downlink);
        int bandwidthY = textY - client.textRenderer.fontHeight - 2;
        drawContext.drawText(client.textRenderer, bandwidthText, 5, bandwidthY, 0xFFFFFF, true);

        drawContext.drawText(tr, bandwidthText, 5, bandwidthY, Colors.WHITE, true);
        drawContext.drawText(tr, pingText, currentX, textY, pingColor, true);
        currentX += tr.getWidth(pingText) + 4;
        drawContext.drawText(tr, sprText, currentX, textY, sprColor, true);
        currentX += tr.getWidth(sprText) + 4;
        drawContext.drawText(tr, idText, currentX, textY, idColor, true);
    }

    private void drawBar(DrawContext ctx, int x, int bottomY, int height, int color) {
        ctx.fill(x, bottomY - height, x + BAR_WIDTH, bottomY, color);
    }

    private static final int COLOR_YELLOW = 0xFFFFD632;  // 黄色
    private static final int COLOR_ORANGE = 0xFFFF890C;  // 橙色
    // Colors.WHITE 和 Colors.RED 已经是正确的 ARGB 格式，无需修改

    private int getPingColor(int ping) {
        if (ping < PING_WHITE) return Colors.WHITE;
        if (ping < PING_YELLOW) return COLOR_YELLOW;
        if (ping < PING_ORANGE) return COLOR_ORANGE;
        return Colors.RED;
    }

    private int getSprColor(double spr) {
        if (spr <= SPR_RED) return Colors.RED;
        if (spr <= SPR_ORANGE) return COLOR_ORANGE;
        if (spr <= SPR_YELLOW) return COLOR_YELLOW;
        return Colors.WHITE;
    }

    private int getCurrentPing(MinecraftClient client) {
        if (client.getNetworkHandler() == null || client.player == null) return 0;
        var entry = client.getNetworkHandler().getPlayerListEntry(client.player.getUuid());
        return entry != null ? entry.getLatency() : 0;
    }
}