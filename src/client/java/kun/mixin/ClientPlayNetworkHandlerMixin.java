package kun.mixin;

import kun.client.PacketTracker;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.network.packet.s2c.play.PlayerListS2CPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPlayNetworkHandler.class)
public class ClientPlayNetworkHandlerMixin {

    @Inject(method = "onPlayerList", at = @At("RETURN"))
    private void onPlayerList(PlayerListS2CPacket packet, CallbackInfo ci) {
        for (PlayerListS2CPacket.Entry entry : packet.getEntries()) {
            int latency = entry.latency();
            if (latency >= 0) {
                PacketTracker.updatePlayerLatency(entry.profileId(), latency);
            }
        }
    }
}