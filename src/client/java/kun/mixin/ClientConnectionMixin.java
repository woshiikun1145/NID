package kun.mixin;

import io.netty.channel.Channel;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import kun.client.BandwidthTracker;
import kun.client.PacketTracker;
import net.minecraft.network.ClientConnection;
import net.minecraft.network.packet.Packet;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.concurrent.atomic.AtomicLong;

@Mixin(ClientConnection.class)
public abstract class ClientConnectionMixin {

    @Shadow private Channel channel;

    @Inject(method = "channelRead0", at = @At("HEAD"))
    private void onPacketReceived(ChannelHandlerContext ctx, Packet<?> packet, CallbackInfo ci) {
        PacketTracker.onPacketReceived();
    }

    @Inject(method = "channelActive", at = @At("RETURN"))
    private void onChannelActive(ChannelHandlerContext ctx, CallbackInfo ci) {
        if (this.channel != null && !this.channel.eventLoop().inEventLoop()) {
            this.channel.eventLoop().execute(this::addTrafficHandler);
        } else {
            addTrafficHandler();
        }
    }

    @Unique
    private void addTrafficHandler() {
        if (this.channel == null || this.channel.pipeline().get("bandwidthTracker") != null) return;

        BandwidthTracker.init();

        this.channel.pipeline().addBefore("splitter", "bandwidthTracker", new ChannelDuplexHandler() {
            private final AtomicLong inboundBytes = new AtomicLong(0);
            private final AtomicLong outboundBytes = new AtomicLong(0);

            @Override
            public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
                if (msg instanceof io.netty.buffer.ByteBuf buf) {
                    int bytes = buf.readableBytes();
                    inboundBytes.addAndGet(bytes);
                    BandwidthTracker.addDownlinkBytes(bytes);
                }
                super.channelRead(ctx, msg);
            }

            @Override
            public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
                if (msg instanceof io.netty.buffer.ByteBuf buf) {
                    int bytes = buf.readableBytes();
                    outboundBytes.addAndGet(bytes);
                    BandwidthTracker.addUplinkBytes(bytes);
                }
                super.write(ctx, msg, promise);
            }

            @Override
            public void handlerRemoved(ChannelHandlerContext ctx) throws Exception {
                super.handlerRemoved(ctx);
            }
        });
    }
}