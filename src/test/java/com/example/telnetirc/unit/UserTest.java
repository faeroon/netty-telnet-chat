package com.example.telnetirc.unit;

import com.example.telnetirc.auth.User;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Denis Pakhomov.
 * @version 1.0
 */
public class UserTest {

    /**
     * проверяем, что нельзя создать пользователя без логина
     */
    @Test(expected = IllegalArgumentException.class)
    public void testCreateUserWithNullUsernameThrowsException() {
        User user = new User(null, "password");
    }

    /**
     * проверяем, что нельзя создать пользователя с пустым логином
     */
    @Test(expected = IllegalArgumentException.class)
    public void testCreateUserWithEmptyUsernameThrowsException() {
        User user = new User("", "password");
    }

    /**
     * проверяем, что нельзя создать пользователя без пароля
     */
    @Test(expected = IllegalArgumentException.class)
    public void testCreateUserWithNullPasswordThrowsException() {
        User user = new User("vasya", null);
    }

    /**
     * проверяем, что нельзя создать пользователя с пустым паролем
     */
    @Test(expected = IllegalArgumentException.class)
    public void testCreateUserWithEmptyPasswordThrowsException() {
        User user = new User("vasya", "");
    }

    /**
     * проверяем, успешное создание пользовавтеля с неактивной сессией, если логин и пароль пользователя - непустые
     */
    @Test
    public void testCreateUserWithNotEmptyFieldsCreatesUserWithNotActiveSession() {
        User user = new User("vasya", "password");
        assertThat(user.getUsername()).isEqualTo("vasya");
        assertThat(user.getPassword()).isEqualTo("password");
        assertThat(user.getActive()).isFalse();
    }

    /**
     * проверяем, что при если предъявляемый пароль пользователя некорректен, то возвращается false
     */
    @Test
    public void testIsPasswordCorrectWhenPasswordAreNotEqualReturnsFalse() {
        User user = new User("vasya", "password");
        boolean result = user.isPasswordCorrect("123456789");
        assertThat(result).isFalse();
    }

    /**
     * проверяем, что при если предъявляемый пароль пользователя корректен, то возвращается true
     */
    @Test
    public void testIsPasswordCorrectWhenPasswordsAreEqualReturnsTrue() {
        User user = new User("vasya", "password");
        boolean result = user.isPasswordCorrect("password");
        assertThat(result).isTrue();
    }

    /**
     * проверяем, что можно начать сессию пользователя из неактивного состояния
     */
    @Test
    public void testStartSessionWhenUserIsNotActiveReturnsTrue() {
        User user = new User("vasya", "password");
        boolean result = user.startSession();
        assertThat(result).isTrue();
        assertThat(user.getActive()).isTrue();
    }

    /**
     * проверяем, что нельзя начать сессию пользователя, если он уже активен
     */
    @Test
    public void testStartSessionWhenUserIsActiveReturnsFalse() {
        User user = new User("vasya", "password");
        user.startSession();
        boolean result = user.startSession();
        assertThat(result).isFalse();
        assertThat(user.getActive()).isTrue();
    }

    /**
     * проверяем, что можно завершить сессию пользователя, если он активен
     */
    @Test
    public void testCloseSessionWhenUserIsActiveReturnsTrue() {
        User user = new User("vasya", "password");
        user.startSession();
        boolean result = user.closeSession();
        assertThat(result).isTrue();
        assertThat(user.getActive()).isFalse();
    }

    /**
     * проверяем, что нельзя завершить сессию пользователя, если он не активен
     */
    @Test
    public void testCloseSessionWhenUserIsNotActiveReturnsFalse() {
        User user = new User("vasya", "password");
        boolean result = user.closeSession();
        assertThat(result).isFalse();
        assertThat(user.getActive()).isFalse();

    }
}
