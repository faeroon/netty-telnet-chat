package com.example.telnetirc;

import com.example.telnetirc.command.*;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageDecoder;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * @author Denis Pakhomov.
 * @version 1.0
 */
@Sharable
public class CommandDecoder extends MessageToMessageDecoder<String> {

    private final Map<String, Function<String[], Command>> operationCommandMap = new HashMap<>();

    public CommandDecoder() {
        operationCommandMap.put("/login", LoginCommand::new);
        operationCommandMap.put("/join", JoinCommand::new);
        operationCommandMap.put("/leave", LeaveCommand::new);
        operationCommandMap.put("/users", UsersCommand::new);
    }

    @Override
    protected void decode(ChannelHandlerContext ctx, String msg, List<Object> out) throws Exception {

        Command command;

        if (msg.startsWith("/")) {

            String[] parts = msg.trim().split("\\s");
            String prefix = parts[0];
            String[] args = parts.length > 1 ? Arrays.copyOfRange(parts, 1, parts.length) : new String[0];

            if (operationCommandMap.containsKey(prefix)) {
                command = operationCommandMap.get(prefix).apply(args);
            }
            else throw new IllegalArgumentException("Invalid command");

        } else {
            command = new ChatCommand(new String[]{msg});
        }

        out.add(command);
    }
}
