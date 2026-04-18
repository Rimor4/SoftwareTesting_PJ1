package com.demo.controller.user;

import com.demo.entity.User;
import com.demo.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockMultipartFile;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserControllerTest {

    @Mock
    private UserService userService;

    @InjectMocks
    private UserController userController;

    private MockHttpServletRequest request;
    private MockHttpServletResponse response;

    @BeforeEach
    void setUp() {
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
    }

    @Test
    void loginShouldStoreNormalUserInSession() throws Exception {
        User user = new User();
        user.setIsadmin(0);
        when(userService.checkLogin("u01", "pwd")).thenReturn(user);

        String view = userController.login("u01", "pwd", request);

        assertEquals("/index", view);
        assertEquals(user, request.getSession().getAttribute("user"));
    }

    @Test
    void loginShouldStoreAdminInSession() throws Exception {
        User user = new User();
        user.setIsadmin(1);
        when(userService.checkLogin("admin", "pwd")).thenReturn(user);

        String view = userController.login("admin", "pwd", request);

        assertEquals("/admin_index", view);
        assertEquals(user, request.getSession().getAttribute("admin"));
    }

    @Test
    void loginShouldReturnFalseWhenCredentialMismatch() throws Exception {
        when(userService.checkLogin("ghost", "bad")).thenReturn(null);

        String view = userController.login("ghost", "bad", request);

        assertEquals("false", view);
    }

    @Test
    void registerShouldCreateUserAndRedirect() throws Exception {
        userController.register("u02", "Tom", "123456", "a@test.com", "13800000000", response);

        verify(userService).create(any(User.class));
        assertTrue(response.getRedirectedUrl().endsWith("login"));
    }

    @Test
    void updateUserShouldKeepOldPictureWhenNoFileSelected() throws Exception {
        User user = new User();
        user.setUserID("u01");
        user.setPassword("oldPwd");
        user.setPicture("avatar.png");
        when(userService.findByUserID("u01")).thenReturn(user);

        MockMultipartFile file = new MockMultipartFile("picture", "", "application/octet-stream", new byte[0]);
        userController.updateUser("Jerry", "u01", "", "b@test.com", "13900000000", file, request, response);

        assertEquals("Jerry", user.getUserName());
        assertEquals("oldPwd", user.getPassword());
        assertEquals("avatar.png", user.getPicture());
        assertEquals(user, request.getSession().getAttribute("user"));
        assertTrue(response.getRedirectedUrl().endsWith("user_info"));
    }

    @Test
    void logoutShouldClearUserSessionAndRedirectHome() throws Exception {
        request.getSession().setAttribute("user", new User());

        userController.logout(request, response);

        assertNull(request.getSession().getAttribute("user"));
        assertEquals("/index", response.getRedirectedUrl());
    }

    @Test
    void checkPasswordShouldReturnComparisonResult() {
        User user = new User();
        user.setPassword("secret");
        when(userService.findByUserID("u01")).thenReturn(user);

        assertTrue(userController.checkPassword("u01", "secret"));
    }

    @Test
    void registerShouldHandleUserIDAtBoundaryLength254() throws Exception {
        userController.register("A".repeat(254), "Tom", "123456", "a@test.com", "13800000000", response);

        verify(userService).create(any(User.class));
        assertTrue(response.getRedirectedUrl().endsWith("login"));
    }

    @Test
    void registerShouldHandleUserIDAtBoundaryLength255() throws Exception {
        userController.register("A".repeat(255), "Tom", "123456", "a@test.com", "13800000000", response);

        verify(userService).create(any(User.class));
        assertTrue(response.getRedirectedUrl().endsWith("login"));
    }

    @Test
    void registerShouldHandleUserIDExceedingBoundaryLength256() {
        RuntimeException ex = assertThrows(RuntimeException.class, () ->
            userController.register("A".repeat(256), "Tom", "123456", "a@test.com", "13800000000", response));

        assertEquals("用户ID超出长度上限", ex.getMessage());
        verify(userService, never()).create(any(User.class));
    }

    @Test
    void registerShouldHandlePasswordAtBoundaryLength254() throws Exception {
        userController.register("u01", "Tom", "P".repeat(254), "a@test.com", "13800000000", response);

        verify(userService).create(any(User.class));
        assertTrue(response.getRedirectedUrl().endsWith("login"));
    }

    @Test
    void registerShouldHandlePasswordAtBoundaryLength255() throws Exception {
        userController.register("u01", "Tom", "P".repeat(255), "a@test.com", "13800000000", response);

        verify(userService).create(any(User.class));
        assertTrue(response.getRedirectedUrl().endsWith("login"));
    }

    @Test
    void registerShouldHandlePasswordExceedingBoundaryLength256() {
        RuntimeException ex = assertThrows(RuntimeException.class, () ->
            userController.register("u01", "Tom", "P".repeat(256), "a@test.com", "13800000000", response));

        assertEquals("密码超出长度上限", ex.getMessage());
        verify(userService, never()).create(any(User.class));
    }

    @Test
    void registerShouldHandleEmptyUserName() throws Exception {
        userController.register("u01", "", "123456", "a@test.com", "13800000000", response);

        RuntimeException ex = assertThrows(RuntimeException.class, () ->
            verify(userService).create(any(User.class)));
        
        assertEquals("用户名不能为空", ex.getMessage());
        verify(userService, never()).create(any(User.class));
    }

    @Test
    void registerShouldHandleEmptyPassword() {
        RuntimeException ex = assertThrows(RuntimeException.class, () ->
            userController.register("u01", "Tom", "", "a@test.com", "13800000000", response));

        assertEquals("密码不能为空", ex.getMessage());
        verify(userService, never()).create(any(User.class));
    }

    @Test
    void registerShouldRejectDuplicateUserID() {
        when(userService.findByUserID("existingUser")).thenReturn(new User());

        RuntimeException ex = assertThrows(RuntimeException.class, () ->
            userController.register("existingUser", "Tom", "123456", "a@test.com", "13800000000", response));

        assertEquals("用户ID已存在", ex.getMessage());
        verify(userService, never()).create(any(User.class));
    }
}