package com.example.telnetirc.chat;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Сущность сообщения пользователя в канале
 *
 * @author Denis Pakhomov.
 * @version 1.0
 */
public class ChatMessage {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss");

    private final String username;
    private final LocalDateTime time;
    private final String text;

    /**
     *
     * @param username имя пользователя
     * @param text сообщение
     */
    public ChatMessage(String username, String text) {
        this(username, LocalDateTime.now(), text);
    }

    /**
     *
     * @param username имя пользователя
     * @param time время публикации
     * @param text сообщение
     */
    public ChatMessage(String username, LocalDateTime time, String text) {
        this.username = username;
        this.time = time;
        this.text = text;
    }

    @Override
    public String toString() {
        return String.format("%s (%s):\r\n %s\r\n", username, DATE_FORMATTER.format(time), text);
    }
}
