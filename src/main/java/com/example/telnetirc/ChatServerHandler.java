package com.example.telnetirc;

import com.example.telnetirc.auth.AuthService;
import com.example.telnetirc.chat.ChatChannel;
import com.example.telnetirc.command.*;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.util.AttributeKey;
import io.netty.util.concurrent.GlobalEventExecutor;

import java.text.MessageFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Обработчик команд чата, поступаемых от пользователя
 *
 * @author Denis Pakhomov.
 * @version 1.0
 */
@Sharable
public class ChatServerHandler extends SimpleChannelInboundHandler<Command> {

    private final int userChannelLimit;
    private final static int LAST_MESSAGE_COUNT = 10;

    private ResourceBundle resource = PropertyResourceBundle.getBundle("messages/messages", Locale.getDefault());

    private final Map<Class<? extends Command>, CommandHandler> commandDispatcher =
            new HashMap<>();

    private final AuthService authService;
    private final ConcurrentHashMap<String, ChatChannel> chatChannelMap = new ConcurrentHashMap<>();

    private final AttributeKey<String> usernameAttr = AttributeKey.valueOf("username");
    private final AttributeKey<String> chatChannelNameAttr = AttributeKey.valueOf("chatChannel");

    private Function<ChannelHandlerContext, Optional<String>> usernameGetter = ctx -> Optional.ofNullable(
            ctx.channel().attr(usernameAttr).get());
    private Function<ChannelHandlerContext, Optional<String>> chatChannelGetter = ctx -> Optional.ofNullable(
            ctx.channel().attr(chatChannelNameAttr).get());

    public ChatServerHandler(int userChannelLimit, AuthService authService) {

        this.userChannelLimit = userChannelLimit;
        this.authService = authService;

        commandDispatcher.put(LoginCommand.class, new LoginHandler());
        commandDispatcher.put(LeaveCommand.class, new LeaveHandler());
        commandDispatcher.put(JoinCommand.class, new JoinHandler());
        commandDispatcher.put(UsersCommand.class, new UsersHandler());
        commandDispatcher.put(ChatCommand.class, new ChatHandler());
    }

    @Override
    public void channelRegistered(ChannelHandlerContext ctx) throws Exception {
        ctx.channel().writeAndFlush(resource.getString("welcome"));
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Command msg) throws Exception {

        if (commandDispatcher.containsKey(msg.getClass())) {
            commandDispatcher.get(msg.getClass()).handle(ctx, msg);
        } else
            ctx.writeAndFlush(resource.getString("handler.error.not_implemented"));
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        String message = Optional.ofNullable(cause.getCause()).map(e -> e.getMessage()).orElse(cause.getMessage());
        ctx.writeAndFlush(String.format("Error: %s\r\n", message));
    }

    //region command handlers

    /**
     * Обработчик конкретной команды пользователя
     *
     * @param <T> тип команды пользователя
     */
    private interface CommandHandler<T extends Command> {

        @SuppressWarnings("unchecked")
        default void handle(ChannelHandlerContext ctx, Command command) {
            handleInner(ctx, (T) command);
        }

        void handleInner(ChannelHandlerContext ctx, T command);
    }

    /**
     * Обработчик команды аутентификации пользователя
     */
    private class LoginHandler implements CommandHandler<LoginCommand> {

        @Override
        public void handleInner(ChannelHandlerContext ctx, LoginCommand command) {
            if (usernameGetter.apply(ctx).isPresent()) {
                ctx.writeAndFlush(resource.getString("login.error.already_auth"));
                return;
            }

            switch (authService.authenticate(command.getName(), command.getPassword())) {
                case INCORRECT_PASSWORD:
                    ctx.writeAndFlush(resource.getString("login.error.incorrect_password"));
                    break;
                case ALREADY_AUTHENTICATED:
                    ctx.writeAndFlush(resource.getString("login.error.another_auth"));
                    break;
                case AUTHENTICATED:
                    ctx.channel().attr(usernameAttr).set(command.getName());
                    ctx.writeAndFlush(resource.getString("login.success"));
                    break;
                default:
                    ctx.writeAndFlush(resource.getString("login.error.unexpected"));
            }
        }
    }

    /**
     * Обработчик команды выхода пользователя из чата
     */
    private class LeaveHandler implements CommandHandler<LeaveCommand> {

        @Override
        public void handleInner(ChannelHandlerContext ctx, LeaveCommand command) {
            usernameGetter.apply(ctx).ifPresent(username -> {
                chatChannelGetter.apply(ctx).ifPresent(chatChannel -> chatChannelMap.get(chatChannel)
                        .leave(ctx.channel(), username));
                authService.logout(username);
            });

            ctx.writeAndFlush(resource.getString("logout.success"));
            ctx.channel().close();
        }
    }

    /**
     * Обработчик команды добавления пользователя в канал чата
     */
    private class JoinHandler implements CommandHandler<JoinCommand> {

        @Override
        public void handleInner(ChannelHandlerContext ctx, JoinCommand command) {

            Optional<String> username = usernameGetter.apply(ctx);
            if (!username.isPresent()) {
                ctx.writeAndFlush(resource.getString("join.error.anonymous"));
                return;
            }

            Optional<String> channelName = chatChannelGetter.apply(ctx);

            if (channelName.map(channel -> channel.equals(command.getChannel())).orElse(false)) {
                ctx.writeAndFlush(resource.getString("join.error.already_joined"));
                return;
            }

            channelName.ifPresent(channel -> chatChannelMap.get(channel).leave(ctx.channel(), username.get()));

            chatChannelMap.putIfAbsent(command.getChannel(), new ChatChannel(userChannelLimit, LAST_MESSAGE_COUNT,
                    new DefaultChannelGroup(GlobalEventExecutor.INSTANCE)));

            ChatChannel chatChannel = chatChannelMap.get(command.getChannel());

            if (chatChannel.join(ctx.channel(), username.get())) {
                ctx.channel().attr(chatChannelNameAttr).set(command.getChannel());
                ctx.writeAndFlush(resource.getString("join.success"));

            } else ctx.writeAndFlush(resource.getString("join.error.user_limit"));
        }
    }

    /**
     * Обработчик команды просмотра пользователей в канале чата
     */
    private class UsersHandler implements CommandHandler<UsersCommand> {

        @Override
        public void handleInner(ChannelHandlerContext ctx, UsersCommand command) {
            String message = chatChannelGetter.apply(ctx)
                    .map(chatChannelName -> chatChannelMap.get(chatChannelName))
                    .map(chatChannel -> chatChannel.users().collect(Collectors.joining(", ")))
                    .map(users -> MessageFormat.format(resource.getString("users.online"), users))
                    .orElse(resource.getString("users.error.no_channel"));

            ctx.writeAndFlush(message);
        }
    }

    /**
     * Обработчик команды добавления сообщения в канал чата
     */
    private class ChatHandler implements CommandHandler<ChatCommand> {

        @Override
        public void handleInner(ChannelHandlerContext ctx, ChatCommand command) {

            Optional<String> username = usernameGetter.apply(ctx);
            Optional<String> channelName = chatChannelGetter.apply(ctx);

            if (username.isPresent() && channelName.isPresent()) {
                chatChannelMap.get(channelName.get()).chat(username.get(), command.getMessage());
            } else {
                String messageCode = !username.isPresent() ? "chat.error.anonymous" : "chat.error.no_channel";
                ctx.writeAndFlush(resource.getString(messageCode));
            }
        }
    }

    //endregion
}
