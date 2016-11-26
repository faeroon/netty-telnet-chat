package com.example.telnetirc;

import com.example.telnetirc.auth.InMemoryAuthService;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.DelimiterBasedFrameDecoder;
import io.netty.handler.codec.Delimiters;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;

/**
 * @author Denis Pakhomov.
 * @version 1.0
 */
public class ServerChannelInitializer extends ChannelInitializer<SocketChannel> {

    private static final int USER_CHANNEL_LIMIT = 2;


    private static final StringDecoder STRING_DECODER = new StringDecoder();
    private static final CommandDecoder COMMAND_DECODER = new CommandDecoder();
    private static final ChatServerHandler CHAT_SERVER_HANDLER = new ChatServerHandler(USER_CHANNEL_LIMIT,
            new InMemoryAuthService());

    private static final StringEncoder STRING_ENCODER = new StringEncoder();

    @Override
    protected void initChannel(SocketChannel ch) throws Exception {
        ch.pipeline().addLast(new DelimiterBasedFrameDecoder(8192, Delimiters.lineDelimiter()));
        ch.pipeline().addLast(STRING_DECODER);
        ch.pipeline().addLast(STRING_ENCODER);
        ch.pipeline().addLast(COMMAND_DECODER);
        ch.pipeline().addLast(CHAT_SERVER_HANDLER);

    }
}
