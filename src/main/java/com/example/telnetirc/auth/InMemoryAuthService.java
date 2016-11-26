package com.example.telnetirc.auth;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import static com.example.telnetirc.auth.AuthResult.ALREADY_AUTHENTICATED;
import static com.example.telnetirc.auth.AuthResult.AUTHENTICATED;
import static com.example.telnetirc.auth.AuthResult.INCORRECT_PASSWORD;

/**
 * Сервис для аутентификации пользователей чата
 *
 * @author Denis Pakhomov.
 * @version 1.0
 */
public class InMemoryAuthService implements AuthService {

    private final ConcurrentHashMap<String, User> userMap = new ConcurrentHashMap<>();

    @Override
    public AuthResult authenticate(String username, String password) {
        userMap.putIfAbsent(username, new User(username, password));

        User user = userMap.get(username);
        return !user.isPasswordCorrect(password) ? INCORRECT_PASSWORD  :
                user.startSession() ? AUTHENTICATED : ALREADY_AUTHENTICATED;

    }

    @Override
    public boolean logout(String username) {
        if (username == null) throw new IllegalArgumentException("username can't be null");
        return Optional.ofNullable(userMap.get(username)).map(User::closeSession).orElse(false);
    }
}
