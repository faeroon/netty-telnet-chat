package com.example.telnetirc.unit;

import com.example.telnetirc.CommandDecoder;
import com.example.telnetirc.command.*;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.handler.codec.DecoderException;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Denis Pakhomov.
 * @version 1.0
 */
public class CommandDecoderTest {

    /**
     * проверяем, что нельзя отправить пустую сервисную команду
     */
    @Test(expected = DecoderException.class)
    public void testEmptyCommandReturnsFalse() {
        EmbeddedChannel embeddedChannel = new EmbeddedChannel(new CommandDecoder());
        embeddedChannel.writeInbound("/");
    }

    /**
     * проверяем, что при отправлении несуществующей сервисной команды возвращается исключение
     */
    @Test(expected = DecoderException.class)
    public void testCreateUndefinedCommandThrowsException() {
        EmbeddedChannel embeddedChannel = new EmbeddedChannel(new CommandDecoder());
        embeddedChannel.writeInbound("/vote");
    }

    /**
     * проверяем корректное создание чат-команды
     */
    @Test
    public void testCreateChatCommand() {
        EmbeddedChannel embeddedChannel = new EmbeddedChannel(new CommandDecoder());
        String message = "test chat";
        embeddedChannel.writeInbound(message);

        ChatCommand chatCommand = (ChatCommand) embeddedChannel.readInbound();

        assertThat(chatCommand).isNotNull();
        assertThat(chatCommand.getMessage()).isEqualTo(message);
    }

    /**
     * проверяем корректное создание сервисной команды добавления пользователя в канал
     */
    @Test
    public void testCreateJoinCommand() {
        EmbeddedChannel embeddedChannel = new EmbeddedChannel(new CommandDecoder());
        String message = "/join profsouz";
        embeddedChannel.writeInbound(message);

        JoinCommand joinCommand = (JoinCommand) embeddedChannel.readInbound();

        assertThat(joinCommand).isNotNull();
        assertThat(joinCommand.getChannel()).isEqualTo("profsouz");
    }

    /**
     * проверяем корректное создание сервисной команды аутентификации
     */
    @Test
    public void testCreateLoginCommand() {
        EmbeddedChannel embeddedChannel = new EmbeddedChannel(new CommandDecoder());
        String message = "/login vasya password";
        embeddedChannel.writeInbound(message);

        LoginCommand loginCommand = (LoginCommand) embeddedChannel.readInbound();

        assertThat(loginCommand).isNotNull();
        assertThat(loginCommand.getName()).isEqualTo("vasya");
        assertThat(loginCommand.getPassword()).isEqualTo("password");
    }

    /**
     * проверяем корректное создание сервисной команды "выйти с сервера"
     */
    @Test
    public void testCreateLeaveCommand() {
        EmbeddedChannel embeddedChannel = new EmbeddedChannel(new CommandDecoder());
        String message = "/leave";
        embeddedChannel.writeInbound(message);

        LeaveCommand leaveCommand = (LeaveCommand) embeddedChannel.readInbound();

        assertThat(leaveCommand).isNotNull();
    }

    /**
     * проверям корректное создание сервисной команды получения списка доступных пользователей в канале
     */
    @Test
    public void testCreateUsersCommand() {
        EmbeddedChannel embeddedChannel = new EmbeddedChannel(new CommandDecoder());
        String message = "/users";
        embeddedChannel.writeInbound(message);

        UsersCommand usersCommand = (UsersCommand) embeddedChannel.readInbound();

        assertThat(usersCommand).isNotNull();
    }
}
