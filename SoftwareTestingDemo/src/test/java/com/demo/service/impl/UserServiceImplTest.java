package com.demo.service.impl;

import com.demo.dao.UserDao;
import com.demo.entity.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock
    private UserDao userDao;

    @InjectMocks
    private UserServiceImpl userService;

    @Test
    void checkLoginShouldReturnMatchingUser() {
        User user = new User();
        user.setUserID("alice");
        when(userDao.findByUserIDAndPassword("alice", "pwd123")).thenReturn(user);

        User result = userService.checkLogin("alice", "pwd123");

        assertEquals("alice", result.getUserID());
    }

    @Test
    void createShouldReturnCurrentUserCountAfterSave() {
        User user = new User();
        when(userDao.findAll()).thenReturn(Arrays.asList(new User(), new User(), new User()));

        int count = userService.create(user);

        verify(userDao).save(user);
        assertEquals(3, count);
    }

    @Test
    void countUserIdShouldDelegateToDao() {
        when(userDao.countByUserID("u01")).thenReturn(1);

        int count = userService.countUserID("u01");

        assertEquals(1, count);
        verify(userDao).countByUserID("u01");
    }
}
