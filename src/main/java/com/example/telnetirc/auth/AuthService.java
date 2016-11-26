package com.example.telnetirc.auth;

/**
 * @author Denis Pakhomov.
 * @version 1.0
 */
public interface AuthService {

    /**
     * <p>Аутентификация пользователя в чате.</p>
     * <p>Если пользователя с таким именем не существует - создается новый пользователь с таким именем.
     * В обратном случае проверяется корректность пароля и не присутствует ли уже в чате пользователь
     * с таким именем</p>
     * <p>При успешной аутентификации стартуем сессию пользователя</p>
     *
     *
     * @param username имя пользователя
     * @param password пароль
     * @return аутентифицирован пользователь или нет (если нет, то по какой причине)
     */
    AuthResult authenticate(String username, String password);

    /**
     * Выйти из чата
     *
     * @param username имя пользователя
     * @return вышел ли пользователь из чата
     */
    boolean logout(String username);
}
