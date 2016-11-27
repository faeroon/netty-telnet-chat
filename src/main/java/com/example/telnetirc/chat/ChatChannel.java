package com.example.telnetirc.chat;

import io.netty.channel.Channel;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.ChannelGroupFuture;
import io.netty.channel.group.ChannelGroupFutureListener;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.FutureListener;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

/**
 * Канал общения в чате
 *
 * @author Denis Pakhomov.
 * @version 1.0
 */
public class ChatChannel {

    private final int lastMessageCount;
    private final ChannelGroup group;
    private final Semaphore usersLimitSemaphore;
    private final ConcurrentHashMap<String, LocalDateTime> users;
    private final ConcurrentLinkedDeque<ChatMessage> messagesDeque;
    private final AtomicInteger messageHistorySize;

    /**
     *
     * @param usersLimit максимальное количество пользователей в чате
     * @param lastMessageCount количество сообщений в логе
     * @param channelGroup
     */
    public ChatChannel(int usersLimit, int lastMessageCount, ChannelGroup channelGroup) {
        if (usersLimit < 2) throw new IllegalArgumentException("Users limit on chat channel should be more than 2");
        if (lastMessageCount < 1) throw new IllegalArgumentException("Last messages count should be more than 1");

        group = channelGroup;
        messagesDeque = new ConcurrentLinkedDeque<>();
        usersLimitSemaphore = new Semaphore(usersLimit);
        users = new ConcurrentHashMap<>(usersLimit);
        this.lastMessageCount = lastMessageCount;
        this.messageHistorySize = new AtomicInteger(0);
    }

    /**
     * <p>Добавить пользователя в канал чата</p>
     * <p>При добавлении пользователя в канал возвращается false, если достигнуто максимальное
     * количество пользователей на канал</p>
     *
     * @param channel название канала
     * @param username имя пользователя
     * @return добавился ли пользователь в канал
     */
    public boolean join(Channel channel, String username) {

        if (username == null || username.isEmpty()) throw new IllegalArgumentException("username is null or empty");
        if (channel == null) throw new IllegalArgumentException("channel can't be null");

        if (usersLimitSemaphore.tryAcquire()) {
            if (users.putIfAbsent(username, LocalDateTime.now()) == null) {

                getLastMessages().stream().forEach(message -> channel.write(message.toString()));
                channel.flush();

                group.add(channel);
                return true;
            } else {
                usersLimitSemaphore.release();
            }
        }
        return false;
    }

    /**
     * Получаем последние сообщения пользователя с канала
     *
     * @return список последних сообщений пользователей в канале
     */
    private List<ChatMessage> getLastMessages() {
        List<ChatMessage> messages = new ArrayList<>(lastMessageCount);
        Iterator<ChatMessage> iterator = messagesDeque.descendingIterator();
        for (int i = 0; i < lastMessageCount && iterator.hasNext(); i++) {
            messages.add(iterator.next());
        }
        Collections.reverse(messages);

        return messages;
    }

    /**
     * Активные пользователи в канале
     *
     * @return stream активных пользователей в канале
     */
    public Stream<String> users() {
        return users.keySet().stream();
    }

    /**
     * Вывести пользователя из канала
     *
     * @param channel netty-канал пользователя
     * @param username имя пользователя
     *
     * @return покинул ли пользвоатель канал
     */
    public boolean leave(Channel channel, String username) {
        if (username == null || username.isEmpty()) throw new IllegalArgumentException("username is null or empty");
        if (channel == null) throw new IllegalArgumentException("channel can't be null");

        if (users.remove(username) != null) {
            usersLimitSemaphore.release();
            group.remove(channel);
            return true;
        }

        return false;
    }

    /**
     * Вывести сообщение пользователя в канал
     *
     * @param username имя пользователя
     * @param text сообщение
     */
    public void chat(String username, String text) {
        if (username == null || username.isEmpty()) throw new IllegalArgumentException("user is null or empty");
        if (text == null || text.isEmpty()) throw new IllegalArgumentException("text is null or empty");

        ChatMessage message = new ChatMessage(username, text);
        messagesDeque.addLast(message);
        messageHistorySize.incrementAndGet();
        group.writeAndFlush(message.toString()).addListener(future -> {
            while (true) {
                int currentValue = messageHistorySize.get();
                if (currentValue <= lastMessageCount) break;
                if (messageHistorySize.compareAndSet(currentValue, currentValue - 1)) {
                    messagesDeque.removeFirst();
                    break;
                }
            }
        });
    }
}
