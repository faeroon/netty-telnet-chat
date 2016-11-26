package com.example.telnetirc.auth;

/**
 * Результат попытки аутентификации пользователя
 *
 * @author Denis Pakhomov.
 * @version 1.0
 */
public enum AuthResult {
    INCORRECT_PASSWORD,
    ALREADY_AUTHENTICATED,
    AUTHENTICATED
}
