// src/client/java/kun/client/KeyBindings.java
package kun.client;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

public class KeyBindings {
    private static KeyBinding copyKey;

    public static void register() {
        // 注册热键为 N 键（无修饰键）
        copyKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.pingdisplay.copy",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_N,
                "category.pingdisplay"
        ));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.currentScreen != null) return;
            if (copyKey.wasPressed()) {
                if (!PacketTracker.isConnected()) {
                    System.out.println("[PingDisplay] Not connected, copy ignored");
                    return;
                }

                // 检测是否按下了 Ctrl 键（左或右）
                long handle = client.getWindow().getHandle();
                boolean ctrlPressed = InputUtil.isKeyPressed(handle, GLFW.GLFW_KEY_LEFT_CONTROL) ||
                        InputUtil.isKeyPressed(handle, GLFW.GLFW_KEY_RIGHT_CONTROL);

                String text;
                if (ctrlPressed) {
                    // Ctrl + N：详细复制
                    text = CopyFormatter.formatDetailed(client);
                    System.out.println("[PingDisplay] Detailed copy (Ctrl+N)");
                } else {
                    // 单独 N：简洁复制
                    text = CopyFormatter.formatSimple(client);
                    System.out.println("[PingDisplay] Simple copy (N)");
                }

                if (text != null && !text.isEmpty()) {
                    setClipboard(text);
                    // 可选：动作栏提示
                    if (client.player != null) {
                        client.player.sendMessage(Text.literal("§a[PPSD] 已复制网络信息到剪贴板"), true);
                    }
                }
            }
        });
    }

    private static void setClipboard(String text) {
        // 优先使用 Minecraft 剪贴板
        try {
            MinecraftClient.getInstance().keyboard.setClipboard(text);
            System.out.println("[PPSD] Copied with Minecraft clipboard");
        } catch (Exception e) {
            System.out.println("[PPSD] Minecraft clipboard failed: " + e);
            // 备用：AWT 剪贴板
            try {
                java.awt.Toolkit.getDefaultToolkit().getSystemClipboard()
                        .setContents(new java.awt.datatransfer.StringSelection(text), null);
                System.out.println("[PPSD] Copied with AWT clipboard");
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }
}