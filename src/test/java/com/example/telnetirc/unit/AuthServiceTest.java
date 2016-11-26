package com.example.telnetirc.unit;

import com.example.telnetirc.auth.AuthResult;
import com.example.telnetirc.auth.InMemoryAuthService;
import org.junit.Test;

import static com.example.telnetirc.auth.AuthResult.ALREADY_AUTHENTICATED;
import static com.example.telnetirc.auth.AuthResult.AUTHENTICATED;
import static com.example.telnetirc.auth.AuthResult.INCORRECT_PASSWORD;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Тесты аутентификации пользователей
 *
 * @author Denis Pakhomov.
 * @version 1.0
 */
public class AuthServiceTest {

    //region login tests


    /**
     * проверяем, что если пользователь аутентифицируется под несуществующим логином, то создается новый аккаунт
     * и пользователь проходит аутентификацию
     */
    @Test
    public void testLoginWhenUserDoesNotExistThenItCreatesAndAuthPasses() {
        InMemoryAuthService authService = new InMemoryAuthService();

        AuthResult result = authService.authenticate("vasya", "password");

        assertThat(result).isEqualTo(AUTHENTICATED);
    }

    /**
     * проверяем, что если пользователь аутентифицируется под некорректным паролем, то возвращается соответсвующий
     * статус
     */
    @Test
    public void testLoginWhenPasswordIsIncorrectThenAuthFails() {
        InMemoryAuthService authService = new InMemoryAuthService();
        authService.authenticate("vasya", "password");
        authService.logout("vasya");

        AuthResult result = authService.authenticate("vasya", "123456789");

        assertThat(result).isEqualTo(INCORRECT_PASSWORD);
    }

    /**
     * проверяем, что если пользователь пытается аутентифицироваться под аккаунтом, который уже активен на сайте, то
     * аутентификация не проходит и возвращается соответсвующий статус
     */
    @Test
    public void testLoginWhenUserHasActiveSessionThenAuthFails() {
        InMemoryAuthService authService = new InMemoryAuthService();
        authService.authenticate("vasya", "password");

        AuthResult result = authService.authenticate("vasya", "password");

        assertThat(result).isEqualTo(ALREADY_AUTHENTICATED);

    }

    /**
     * проверяем, что если пользователь аутентифицируется под существующим аккаунтом, у которого на данный момент нет
     * активной сессии на сайте, то аутентификация проходит успешно
     */
    @Test
    public void testLoginWhenPasswordIsCorrectAndNoActiveSessionThenAuthPasses() {
        InMemoryAuthService authService = new InMemoryAuthService();
        authService.authenticate("vasya", "password");
        authService.logout("vasya");

        AuthResult result = authService.authenticate("vasya", "password");

        assertThat(result).isEqualTo(AUTHENTICATED);

    }

    //endregion

    //region logout tests

    /**
     * проверяем, что аутентификация без указания логина вовращает исключение
     */
    @Test(expected = IllegalArgumentException.class)
    public void testLogoutWhenUsernameIsNullThrowsException() {
        InMemoryAuthService authService = new InMemoryAuthService();

        authService.logout(null);
    }

    /**
     * проверяем, что при попытке разлогиниться под несуществующим аккаунтом возващается false
     */
    @Test
    public void testLogoutWhenUserNotExistsWhenReturnsFalse() {
        InMemoryAuthService authService = new InMemoryAuthService();

        boolean result = authService.logout("vasya");

        assertThat(result).isFalse();
    }

    /**
     * проверяем, что попытка разлогиниться для неактивного аккаунта возвращает false
     */
    @Test
    public void testLogoutWhenUserSessionIsNotActiveReturnsFalse() {
        InMemoryAuthService authService = new InMemoryAuthService();
        authService.authenticate("vasya", "password");
        authService.logout("vasya");

        boolean result = authService.logout("vasya");

        assertThat(result).isFalse();
    }

    /**
     * проверяем, что попытка разлогиниться для активного пользователя проходит успешно
     */
    @Test
    public void testLogoutWhenUserSessionIsActiveReturnsTrue() {
        InMemoryAuthService authService = new InMemoryAuthService();
        authService.authenticate("vasya", "password");

        boolean result = authService.logout("vasya");

        assertThat(result).isTrue();
    }

    //endregion
}
