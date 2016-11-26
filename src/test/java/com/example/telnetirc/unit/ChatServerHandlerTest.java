package com.example.telnetirc.unit;

import com.example.telnetirc.ChatServerHandler;
import com.example.telnetirc.auth.AuthService;
import com.example.telnetirc.command.*;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.util.AttributeKey;
import org.junit.Before;
import org.junit.Test;

import java.util.ResourceBundle;

import static com.example.telnetirc.auth.AuthResult.ALREADY_AUTHENTICATED;
import static com.example.telnetirc.auth.AuthResult.AUTHENTICATED;
import static com.example.telnetirc.auth.AuthResult.INCORRECT_PASSWORD;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author Denis Pakhomov.
 * @version 1.0
 */
public class ChatServerHandlerTest {

    private ChatServerHandler chatServerHandler;

    private AuthService authService;

    private ResourceBundle resource = ResourceBundle.getBundle("messages/messages");

    private String username = "vasya";
    private String password = "password";
    private String channelName = "friends";

    @Before
    public void setUp() {
        authService = mock(AuthService.class);
        chatServerHandler = new ChatServerHandler(2, authService);
    }

    //region test login command

    /**
     * проверяем, что при авторизации с неверным паролем возвращается сообщение об ошибке, и логин не записывается
     * в свойства канала
     */
    @Test
    public void testLoginWithIncorrectPassword() {
        when(authService.authenticate(username, password)).thenReturn(INCORRECT_PASSWORD);

        EmbeddedChannel channel = new EmbeddedChannel(chatServerHandler);
        channel.releaseOutbound();
        channel.writeInbound(new LoginCommand(new String[]{username, password}));
        String response = (String) channel.readOutbound();

        assertThat(channel.<String>attr(AttributeKey.valueOf("username")).get()).isNull();
        assertThat(response).isEqualTo(resource.getString("login.error.incorrect_password"));
    }

    /**
     * проверяем, что при авторизации с корректным паролем, логин записывается в свойства канала, и возвращается
     * сообщение об успешной авторизации
     */
    @Test
    public void testLoginWithCorrectPassword() {
        when(authService.authenticate(username, password)).thenReturn(AUTHENTICATED);

        EmbeddedChannel channel = new EmbeddedChannel(chatServerHandler);
        channel.releaseOutbound();
        channel.writeInbound(new LoginCommand(new String[]{username, password}));
        String response = (String) channel.readOutbound();

        assertThat(response).isEqualTo(resource.getString("login.success"));
        assertThat(channel.<String>attr(AttributeKey.valueOf("username")).get()).isEqualTo(username);
    }

    /**
     * проверяем, что при авторизации под уже активным пользователем, логин не записывется в свойства канала, и
     * возвращается соответствующее сообщение
     */
    @Test
    public void testLoginWhenAnotherUserAuthenticated() {
        when(authService.authenticate(username, password)).thenReturn(ALREADY_AUTHENTICATED);

        EmbeddedChannel channel = new EmbeddedChannel(chatServerHandler);
        channel.releaseOutbound();
        channel.writeInbound(new LoginCommand(new String[]{username, password}));
        String response = (String) channel.readOutbound();

        assertThat(channel.<String>attr(AttributeKey.valueOf("username")).get()).isNull();
        assertThat(response).isEqualTo(resource.getString("login.error.another_auth"));
    }

    /**
     * проверяем, что возвращается сообщение об ошибке, если уже авторизованный пользователь пытается авторизоваться
     * снова
     */
    @Test
    public void testLoginWhenAlreadyAuthenticatedReturnsError() {
        EmbeddedChannel channel = new EmbeddedChannel(chatServerHandler);
        channel.attr(AttributeKey.valueOf("username")).set("petya");
        channel.writeInbound(new LoginCommand(new String[]{username, password}));
        channel.readOutbound();
        String response = (String) channel.readOutbound();

        assertThat(response).isEqualTo(resource.getString("login.error.already_auth"));
    }

    //endregion

    //region test join command

    /**
     * проверяем, что при попытке добавления в канал без авторизации возвращается сообщение об ошибке
     */
    @Test
    public void testJoinWhenAnonymousThenReturnsError() {
        EmbeddedChannel channel = new EmbeddedChannel(chatServerHandler);
        channel.releaseOutbound();
        
        channel.writeInbound(new JoinCommand(new String[]{channelName}));
        String response = (String) channel.readOutbound();
        
        assertThat(response).isEqualTo(resource.getString("join.error.anonymous"));
    }

    /**
     * проверяем, что при добавлении пользователя в чат-канал, в котором еще не достигнут лимит на пользователей,
     * в свойства i/o канала пишется имя канала и возвращается сообщение об успешном добавлении
     */
    @Test
    public void testJoinWhenChannelIsFreeReturnsSuccess() {
        EmbeddedChannel channel = new EmbeddedChannel(chatServerHandler);
        channel.attr(AttributeKey.valueOf("username")).set(username);
        channel.releaseOutbound();
        
        channel.writeInbound(new JoinCommand(new String[]{channelName}));
        String response = (String) channel.readOutbound();
        
        assertThat(response).isEqualTo(resource.getString("join.success"));
        assertThat(channel.attr(AttributeKey.valueOf("chatChannel")).get()).isEqualTo(channelName);
    }

    /**
     * проверяем, что при добавлении пользователя в канал, в который он уже добавлен,
     * возвращается соответствующее сообщение об ошибке
     */
    @Test
    public void testJoinWhenAlreadyJoinedToChannelReturnsError() {
        EmbeddedChannel channel = new EmbeddedChannel(chatServerHandler);
        channel.attr(AttributeKey.valueOf("username")).set(username);

        JoinCommand joinCommand = new JoinCommand(new String[]{channelName});

        channel.writeInbound(joinCommand);
        channel.releaseOutbound();

        channel.writeInbound(joinCommand);
        String response = (String) channel.readOutbound();
        
        assertThat(response).isEqualTo(resource.getString("join.error.already_joined"));
        assertThat(channel.attr(AttributeKey.valueOf("chatChannel")).get()).isEqualTo(channelName);
    }

    /**
     * проверяем, что при добавлении в канал пользователя, уже добавленного в другой канал, перед добавлением
     * происходит выход из старого канала
     */
    @Test
    public void testJoinWhenUserJoinedToAnotherChannelThenLeavesOldChannel() {
        EmbeddedChannel channel = new EmbeddedChannel(chatServerHandler);
        channel.attr(AttributeKey.valueOf("username")).set(username);

        channel.writeInbound(new JoinCommand(new String[]{"oldChannel"}));
        channel.readOutbound();

        channel.writeInbound(new JoinCommand(new String[]{channelName}));
        String response = (String) channel.readOutbound();

        assertThat(response).isEqualTo(resource.getString("join.success"));
        assertThat(channel.attr(AttributeKey.valueOf("chatChannel")).get()).isEqualTo(channelName);
    }

    /**
     * проверяем, что при добавлении в канал пользователя, в котором достигнут лимит пользователей, возвращается
     * сообщение об ошибке
     */
    @Test
    public void testJoinWhenUserLimitExceededThenReturnsError() {

        JoinCommand joinCommand = new JoinCommand(new String[]{channelName});

        // first user
        EmbeddedChannel channel1 = new EmbeddedChannel(chatServerHandler);
        channel1.attr(AttributeKey.valueOf("username")).set("petya");
        channel1.writeInbound(joinCommand);

        // second user
        EmbeddedChannel channel2 = new EmbeddedChannel(chatServerHandler);
        channel2.attr(AttributeKey.valueOf("username")).set("leonid");
        channel2.writeInbound(joinCommand);

        // test user
        EmbeddedChannel channel = new EmbeddedChannel(chatServerHandler);
        channel.attr(AttributeKey.valueOf("username")).set(username);
        channel.releaseOutbound();
        channel.writeInbound(joinCommand);

        String response = (String) channel.readOutbound();

        assertThat(response).isEqualTo(resource.getString("join.error.user_limit"));
        assertThat(channel.attr(AttributeKey.valueOf("chatChannel")).get()).isNull();
    }

    //endregion

    //region test chat command

    /**
     * проверяем, что при попытке добавить в чат сообщение неавторизованным пользователем - возвращается
     * сообщение об ошибке
     */
    @Test
    public void testChatWhenUserIsNotAuthenticatedThenReturnsError() {
        EmbeddedChannel channel = new EmbeddedChannel(chatServerHandler);
        channel.releaseOutbound();
        channel.writeInbound(new ChatCommand(new String[]{"message"}));

        String response = (String) channel.readOutbound();
        assertThat(response).isEqualTo(resource.getString("chat.error.anonymous"));
    }

    /**
     * проверяем, что при попытке добавить в чат сообщение, если пользователь не доабвлен в канал - возвращается
     * сообщение об ошибке
     */
    @Test
    public void testChatWhenUserIsNotJoinedToChannelReturnsError() {
        EmbeddedChannel channel = new EmbeddedChannel(chatServerHandler);
        channel.attr(AttributeKey.valueOf("username")).set(username);
        channel.releaseOutbound();
        channel.writeInbound(new ChatCommand(new String[]{"message"}));

        String response = (String) channel.readOutbound();
        assertThat(response).isEqualTo(resource.getString("chat.error.no_channel"));
    }

    /**
     * проверяем, что при отправлении сообщения в чат - оно отправляется всем добавленным в канал клиентам
     */
    @Test
    public void testChatWhenUserJoinedToChannelSendMessageToAllUsersOfChannel() {

        JoinCommand joinCommand = new JoinCommand(new String[]{channelName});

        EmbeddedChannel channel1 = new EmbeddedChannel(chatServerHandler);
        channel1.readOutbound();
        channel1.attr(AttributeKey.valueOf("username")).set("petya");
        channel1.writeInbound(joinCommand);

        EmbeddedChannel channel = new EmbeddedChannel(chatServerHandler);
        channel.attr(AttributeKey.valueOf("username")).set(username);
        channel.writeInbound(joinCommand);
        channel.releaseOutbound();
        channel.writeInbound(new ChatCommand(new String[]{"message"}));

        String message = (String) channel.readOutbound();
        assertThat(message).contains("message");
        assertThat(message).contains(username);
    }

    //endregion

    //region test leave command

    /**
     * проверяем, что перед отключением от сервера авторизованного пользователя из канала он разлогинивается
     */
    @Test
    public void testLeaveWhenUserIsAuthenticatedThenLogoutAndCloseChannel() {
        EmbeddedChannel channel = new EmbeddedChannel(chatServerHandler);
        channel.attr(AttributeKey.valueOf("username")).set(username);
        channel.releaseOutbound();

        channel.writeInbound(new LeaveCommand(new String[]{}));

        assertThat(channel.isOpen()).isFalse();
        verify(authService).logout(username);
    }

    /**
     * проверяем, что перед отключением пользователя, добавленного в чат-канал, он выходит из чат-канала
     */
    @Test
    public void testLeaveWhenUserJoinedChannelThenLeaveChatAndCloseChannel() {
        JoinCommand joinCommand = new JoinCommand(new String[]{channelName});

        EmbeddedChannel channel1 = new EmbeddedChannel(chatServerHandler);
        channel1.attr(AttributeKey.valueOf("username")).set("petya");
        channel1.writeInbound(joinCommand);
        channel1.releaseOutbound();

        EmbeddedChannel channel = new EmbeddedChannel(chatServerHandler);
        channel.attr(AttributeKey.valueOf("username")).set(username);
        channel.writeInbound(joinCommand);
        channel.releaseOutbound();
        channel.writeInbound(new LeaveCommand(new String[]{}));

        channel1.writeInbound(new UsersCommand(new String[]{}));
        String result = (String) channel1.readOutbound();

        assertThat(channel.isOpen()).isFalse();
        assertThat(result).doesNotContain(username);
    }

    //endregion

    //region test users command

    /**
     * проверяем, что возвращается ошибка, если запросить пользователей канала, не добаввившись в канал
     */
    @Test
    public void testUsersWhenUserNotJoinedToChannelThenReturnsError() {
        EmbeddedChannel channel = new EmbeddedChannel(chatServerHandler);
        channel.attr(AttributeKey.valueOf("username")).set(username);
        channel.releaseOutbound();
        channel.writeInbound(new UsersCommand(new String[]{}));

        String result = (String) channel.readOutbound();
        assertThat(result).isEqualTo(resource.getString("users.error.no_channel"));
    }

    /**
     * проверяем получения списка активных пользователей канала, если пользователь добавлен в канал
     */
    @Test
    public void testUserWhenJoinedToChannelThenReturnsUsersList() {
        JoinCommand joinCommand = new JoinCommand(new String[]{channelName});

        EmbeddedChannel channel1 = new EmbeddedChannel(chatServerHandler);
        channel1.readOutbound();
        channel1.attr(AttributeKey.valueOf("username")).set("petya");
        channel1.writeInbound(joinCommand);

        EmbeddedChannel channel = new EmbeddedChannel(chatServerHandler);
        channel.attr(AttributeKey.valueOf("username")).set(username);
        channel.writeInbound(joinCommand);
        channel.releaseOutbound();
        channel.writeInbound(new UsersCommand(new String[]{}));

        String result = (String) channel.readOutbound();
        assertThat(result).contains(username);
        assertThat(result).contains("petya");
    }

    //endregion
}
