package com.example.telnetirc;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;

import java.net.InetSocketAddress;

/**
 * @author Denis Pakhomov.
 * @version 1.0
 */
public class TelnetIrcServer {

    private final int port;

    public TelnetIrcServer(int port) {
        this.port = port;
    }

    public static void main(String[] args) throws Exception {
        TelnetIrcServer server = new TelnetIrcServer(Integer.parseInt(args[0]));
        server.start();
    }

    public void start() throws Exception {
        final EventLoopGroup eventLoopGroup = new NioEventLoopGroup();
        try {
            ServerBootstrap bootstrap = new ServerBootstrap();
            bootstrap.group(eventLoopGroup)
                    .channel(NioServerSocketChannel.class)
                    .localAddress(new InetSocketAddress(port))
                    .childHandler(new ServerChannelInitializer());

            ChannelFuture future = bootstrap.bind().sync();
            future.channel().closeFuture().sync();
        } finally {
            eventLoopGroup.shutdownGracefully().sync();
        }
    }
}
