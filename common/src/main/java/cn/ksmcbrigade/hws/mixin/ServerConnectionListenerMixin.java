package cn.ksmcbrigade.hws.mixin;

import cn.ksmcbrigade.hws.CommonClass;
import cn.ksmcbrigade.hws.handlers.HTCPHandler;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import net.minecraft.server.network.ServerConnectionListener;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.io.IOException;
import java.lang.reflect.Method;

@Mixin(ServerConnectionListener.class)
public class ServerConnectionListenerMixin {

    @Redirect(method = "startTcpServerListener", at = @At(value = "INVOKE", target = "Lio/netty/bootstrap/ServerBootstrap;childHandler(Lio/netty/channel/ChannelHandler;)Lio/netty/bootstrap/ServerBootstrap;"))
    public ServerBootstrap startHttpServerListener(ServerBootstrap instance, ChannelHandler childHandler) throws IOException {
        CommonClass.genConfig();
        return instance.childHandler(new ChannelInitializer<>() {
            @Override
            protected void initChannel(Channel ch) throws Exception {
                ch.pipeline().addFirst(new HTCPHandler(CommonClass.webDir,CommonClass.indexes));
                if (childHandler instanceof ChannelInitializer<?> channelInitializer) {
                    Method method = channelInitializer.getClass().getDeclaredMethod("initChannel", Channel.class);
                    method.setAccessible(true);
                    method.invoke(childHandler, ch);
                }
            }
        });
    }
}
