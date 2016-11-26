package com.example.telnetirc.auth;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Сущность пользователя
 *
 * @author Denis Pakhomov.
 * @version 1.0
 */
public class User {

    private final String username;
    private final String password;
    private final AtomicBoolean active;

    /**
     *
     * @param username имя пользователя
     * @param password пароль
     */
    public User(String username, String password) {
        if (username == null || username.isEmpty()) throw new IllegalArgumentException("user is null or empty");
        if (password == null || password.isEmpty()) throw new IllegalArgumentException("password is null or empty");

        this.username = username;
        this.password = password;
        this.active = new AtomicBoolean(false);
    }

    /**
     * проверка пароля пользователя
     *
     * @param expectedPassword пароль для проверки
     * @return является ли пароль правильным
     */
    public boolean isPasswordCorrect(String expectedPassword) {
        return this.password.equals(expectedPassword);
    }

    /**
     * начать сессию пользователя в чате
     *
     * @return начата ли сессия
     */
    public boolean startSession() {
        return active.compareAndSet(false, true);
    }

    /**
     * завершить сессию пользователя в чате
     *
     * @return завершилась ли сессия
     */
    public boolean closeSession() {
        return active.compareAndSet(true, false);
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public boolean getActive() {
        return active.get();
    }
}
