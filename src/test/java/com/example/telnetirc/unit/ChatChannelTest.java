package com.example.telnetirc.unit;

import com.example.telnetirc.chat.ChatChannel;
import io.netty.channel.Channel;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.ChannelGroupFuture;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/**
 * @author Denis Pakhomov.
 * @version 1.0
 */
public class ChatChannelTest {

    private ChannelGroup channelGroup;

    @Before
    public void setUp() {
        channelGroup = mock(ChannelGroup.class);
        when(channelGroup.writeAndFlush(any())).thenReturn(mock(ChannelGroupFuture.class));
    }

    //region constructor tests

    /**
     * проверяем, что нельзя создать канал с лимитов в одного пользователя
     */
    @Test(expected = IllegalArgumentException.class)
    public void testCreateChatChannelWithUserLimitLessTwoThrowsException() {
        ChatChannel chatChannel = new ChatChannel(1, 5, channelGroup);
    }

    /**
     * проверяем, что нельзя создать канал, который при join возвращает 0 последних сообщений канала
     */
    @Test(expected = IllegalArgumentException.class)
    public void testCreateChatChannelWithZeroLastMessagesLessOneThrowsException() {
        ChatChannel chatChannel = new ChatChannel(3, 0, channelGroup);
    }

    /**
     * проверяем, что можно создать канал с лимитом в 2 пользователя
     */
    @Test
    public void testCreateChatChannelWithUsersLimitEqTwoAndLastMessagesEqOne() {
        ChatChannel chatChannel = new ChatChannel(2, 1, channelGroup);
    }

    //endregion

    //region join tests

    /**
     * проверяем, что нельзя добавить в канал пользователя без логина
     */
    @Test(expected = IllegalArgumentException.class)
    public void testJoinWhenUsernameIsNullThrowsException() {
        ChatChannel chatChannel = new ChatChannel(2, 1, channelGroup);
        Channel mockChannel = mock(Channel.class);

        chatChannel.join(mockChannel, null);
    }

    /**
     * проверяем, что нельзя добавить в канал пользователя с пустым логином
     */
    @Test(expected = IllegalArgumentException.class)
    public void testJoinWhenUsernameIsEmptyThrowsException() {
        ChatChannel chatChannel = new ChatChannel(2, 1, channelGroup);
        Channel mockChannel = mock(Channel.class);

        chatChannel.join(mockChannel, "");
    }

    /**
     * проверяем, что нельзя добавить пользователя в канал без указания его i/o channel
     */
    @Test(expected = IllegalArgumentException.class)
    public void testJoinWhenChannelIsNullThrowsException() {
        ChatChannel chatChannel = new ChatChannel(2, 1, channelGroup);

        chatChannel.join(null, "vasya");
    }

    /**
     * проверяем, что можно добавить пользователя в канал, пока лимит пользователей не превышен
     */
    @Test
    public void testJoinWhenUsersLimitNotExceededReturnsTrue() {
        ChatChannel chatChannel = new ChatChannel(2, 1, channelGroup);
        Channel mockChannel = mock(Channel.class);

        boolean joinResult = chatChannel.join(mockChannel, "vasya");
        List<String> users = chatChannel.users().collect(Collectors.toList());

        assertThat(joinResult).isTrue();
        assertThat(users).hasSize(1);
        assertThat(users).containsOnly("vasya");
        verify(channelGroup).add(mockChannel);
    }

    /**
     * проверяем, что нельзя добавить повторно одного и того же клиента в канал
     */
    @Test
    public void testJoinWithSameUserWithoutLeaveReturnsFalse() {
        ChatChannel chatChannel = new ChatChannel(2, 1, channelGroup);
        String username = "vasya";
        Channel channel = mock(Channel.class);
        chatChannel.join(channel, username);

        boolean joinResult = chatChannel.join(channel, "vasya");

        assertThat(joinResult).isFalse();
        verify(channelGroup, only()).add(channel);
    }

    /**
     * проверяем, нельзя добавить пользователя в канал, у которого лимит пользователей достиг максимума
     */
    @Test
    public void testJoinWhenUsersLimitExceededReturnsFalse() {
        ChatChannel chatChannel = new ChatChannel(2, 1, channelGroup);

        chatChannel.join(mock(Channel.class), "vasya");
        chatChannel.join(mock(Channel.class), "petya");

        Channel mockChannel = mock(Channel.class);
        boolean joinResult = chatChannel.join(mockChannel, "vanya");

        assertThat(joinResult).isFalse();
        verify(channelGroup, never()).add(mockChannel);
    }

    //endregion

    //region leave tests

    /**
     * проверяем, что нельзя удалить пользователя без логина из канала
     */
    @Test(expected = IllegalArgumentException.class)
    public void testLeaveWhenUsernameIsNullThrowsException() {
        ChatChannel chatChannel = new ChatChannel(2, 1, channelGroup);
        Channel mockChannel = mock(Channel.class);

        chatChannel.leave(mockChannel, null);
    }

    /**
     * проверяем, что нельзя удалить пользователя с пустым логином из канала
     */
    @Test(expected = IllegalArgumentException.class)
    public void testLeaveWhenUsernameIsEmptyThrowsException() {
        ChatChannel chatChannel = new ChatChannel(2, 1, channelGroup);
        Channel mockChannel = mock(Channel.class);

        chatChannel.leave(mockChannel, "");
    }

    /**
     * проверяем, что нельзя удалить пользователя без i/o channel-а из чат-канала
     */
    @Test(expected = IllegalArgumentException.class)
    public void testLeaveWhenChannelIsNullThrowsException() {
        ChatChannel chatChannel = new ChatChannel(2, 1, channelGroup);

        chatChannel.leave(null, "vasya");
    }

    /**
     * проверяем, что нельзя удалить пользователя из канала, в котором этот пользователь не состоит
     */
    @Test
    public void testLeaveWhenUserNotJoinedReturnsFalse() {
        ChatChannel chatChannel = new ChatChannel(2, 1, channelGroup);

        Channel channel = mock(Channel.class);

        boolean leaveResult = chatChannel.leave(channel, "vasya");
        assertThat(leaveResult).isFalse();
        verify(channelGroup, never()).remove(channel);
    }

    /**
     * проверяем, что успешно удаляются пользователи, добавленные в канал
     */
    @Test
    public void testLeaveWhenUserJoinedReturnsTrue() {
        ChatChannel chatChannel = new ChatChannel(2, 1, channelGroup);

        Channel channel = mock(Channel.class);

        chatChannel.join(channel, "vasya");

        boolean leaveResult = chatChannel.leave(channel, "vasya");
        assertThat(leaveResult).isTrue();
        verify(channelGroup).remove(channel);
    }

    //endregion

    //region chat tests

    /**
     * проверяем, что нельзя чатить в канал, не указав свой логин
     */
    @Test(expected = IllegalArgumentException.class)
    public void testChatWhenUsernameIsNullThenThrowsException() {
        ChatChannel chatChannel = new ChatChannel(2, 1, channelGroup);
        chatChannel.chat(null, "text");
    }

    /**
     * проверяем, что нельзя чатить в канал с пустым логином
     */
    @Test(expected = IllegalArgumentException.class)
    public void testChatWhenUsernameIsEmptyThenThrowsException() {
        ChatChannel chatChannel = new ChatChannel(2, 1, channelGroup);
        chatChannel.chat("", "text");
    }

    /**
     * проверяем, что нельзя чатить в канал, не указав текст сообщения
     */
    @Test(expected = IllegalArgumentException.class)
    public void testChatWhenTextIsNullThrowsException() {
        ChatChannel chatChannel = new ChatChannel(2, 1, channelGroup);
        chatChannel.chat("vasya", null);
    }

    /**
     * проверяем, что нельзя чатить в канал пустые сообщения
     */
    @Test(expected = IllegalArgumentException.class)
    public void testChatWhenTextIsEmptyThenThrowsException() {
        ChatChannel chatChannel = new ChatChannel(2, 1, channelGroup);
        chatChannel.chat("vasya", "");
    }

    /**
     * проверяем, что сообщения, добавленные в канал, посылаются всем добавленным пользователям
     */
    @Test
    public void testChatSendMessageToAllUsersOfTheGroup() {
        ChatChannel chatChannel = new ChatChannel(2, 1, channelGroup);

        ArgumentCaptor<String> messageCaptor = ArgumentCaptor.forClass(String.class);
        when(channelGroup.writeAndFlush(messageCaptor.capture())).thenReturn(mock(ChannelGroupFuture.class));

        String text = "some text";
        chatChannel.chat("vasya", text);

        verify(channelGroup).writeAndFlush(any());

        String message = messageCaptor.getValue();
        assertThat(message).isNotNull();
        assertThat(message).containsSequence("vasya");
        assertThat(message).containsSequence(text);
    }

    //endregion

    /**
     * проверяем, что при добавлении в канал пользователя, он получает последние X сообщений данного канала,
     * начиная с ранних
     */
    @Test
    public void testGetLastMessagesWriteMessagesNoMoreLimit() {
        ChatChannel chatChannel = new ChatChannel(2, 2, channelGroup);

        Channel mockChannel = mock(Channel.class);
        ArgumentCaptor<String> messageCaptor = ArgumentCaptor.forClass(String.class);
        when(mockChannel.write(messageCaptor.capture())).thenReturn(null);

        chatChannel.join(mock(Channel.class), "vasya");
        chatChannel.chat("vasya", "text1");
        chatChannel.chat("vasya", "text2");
        chatChannel.chat("vasya", "text3");


        chatChannel.join(mockChannel, "petya");

        verify(mockChannel, times(2)).write(anyString());
        List<String> messages = messageCaptor.getAllValues();
        assertThat(messages).isNotNull();
        assertThat(messages).hasSize(2);

        String text2Message = messages.get(0);
        assertThat(text2Message).containsSequence("vasya");
        assertThat(text2Message).containsSequence("text2");

        String text3Message = messages.get(1);
        assertThat(text3Message).containsSequence("vasya");
        assertThat(text3Message).containsSequence("text3");

    }

}
