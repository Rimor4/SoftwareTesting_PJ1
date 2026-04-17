package com.demo.controller.user;

import com.demo.entity.Message;
import com.demo.entity.User;
import com.demo.entity.vo.MessageVo;
import com.demo.exception.LoginException;
import com.demo.service.MessageService;
import com.demo.service.MessageVoService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.ui.ConcurrentModel;
import org.springframework.ui.Model;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MessageControllerTest {

    @Mock
    private MessageService messageService;

    @Mock
    private MessageVoService messageVoService;

    @InjectMocks
    private MessageController messageController;

    private MockHttpServletRequest request;
    private MockHttpServletResponse response;

    @BeforeEach
    void setUp() {
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
    }

    @Test
    void messageListPageShouldRejectAnonymousAccess() {
        when(messageService.findPassState(any(PageRequest.class)))
                .thenReturn(new PageImpl<Message>(Collections.singletonList(new Message()), PageRequest.of(0, 5), 1));
        when(messageVoService.returnVo(any())).thenReturn(Collections.singletonList(new MessageVo()));

        Model model = new ConcurrentModel();
        assertThrows(LoginException.class, () -> messageController.message_list(model, request));
    }

    @Test
    void messageListPageShouldExposeTotalsForLoggedInUser() throws Exception {
        User user = new User();
        user.setUserID("u01");
        request.getSession().setAttribute("user", user);
        when(messageService.findPassState(any(PageRequest.class)))
                .thenReturn(new PageImpl<Message>(Collections.singletonList(new Message()), PageRequest.of(0, 5), 12));
        when(messageService.findByUser(eq("u01"), any(PageRequest.class)))
                .thenReturn(new PageImpl<Message>(Collections.singletonList(new Message()), PageRequest.of(0, 5), 6));
        when(messageVoService.returnVo(any())).thenReturn(Collections.singletonList(new MessageVo()));

        Model model = new ConcurrentModel();
        String view = messageController.message_list(model, request);

        assertEquals("message_list", view);
        assertEquals(3, model.getAttribute("total"));
        assertEquals(2, model.getAttribute("user_total"));
    }

    @Test
    void userMessageListShouldRequireLogin() {
        assertThrows(LoginException.class, () -> messageController.user_message_list(1, request));
    }

    @Test
    void sendMessageShouldPersistAndRedirect() throws Exception {
        messageController.sendMessage("u01", "测试留言", response);

        verify(messageService).create(any(Message.class));
        assertEquals("/message_list", response.getRedirectedUrl());
    }

    @Test
    void modifyMessageShouldResetStateAndReturnTrue() throws Exception {
        Message message = new Message();
        message.setMessageID(3);
        when(messageService.findById(3)).thenReturn(message);

        assertTrue(messageController.modifyMessage(3, "新内容", response));
        assertEquals("新内容", message.getContent());
        assertEquals(1, message.getState());
        verify(messageService).update(message);
    }

    @Test
    void sendMessageShouldHandleEmptyUserID() {
        RuntimeException ex = assertThrows(RuntimeException.class, () ->
            messageController.sendMessage("", "测试留言", response));

        assertEquals("用户ID不能为空", ex.getMessage());
    }

    @Test
    void sendMessageShouldHandleEmptyContent() {
        RuntimeException ex = assertThrows(RuntimeException.class, () ->
            messageController.sendMessage("u01", "", response));

        assertEquals("留言内容不能为空", ex.getMessage());
    }

    @Test
    void sendMessageShouldHandleUserIDAtBoundaryLength24() throws Exception {
        messageController.sendMessage("A".repeat(24), "测试留言", response);

        verify(messageService).create(any(Message.class));
        assertEquals("/message_list", response.getRedirectedUrl());
    }

    @Test
    void sendMessageShouldHandleUserIDAtBoundaryLength25() throws Exception {
        messageController.sendMessage("A".repeat(25), "测试留言", response);

        verify(messageService).create(any(Message.class));
        assertEquals("/message_list", response.getRedirectedUrl());
    }

    @Test
    void sendMessageShouldHandleUserIDExceedingBoundaryLength26() {
        RuntimeException ex = assertThrows(RuntimeException.class, () ->
            messageController.sendMessage("A".repeat(26), "测试留言", response));

        assertEquals("用户ID超出长度上限", ex.getMessage());
    }

    @Test
    void sendMessageShouldHandleContentAtBoundaryLength4999() throws Exception {
        messageController.sendMessage("u01", "A".repeat(4999), response);

        verify(messageService).create(any(Message.class));
        assertEquals("/message_list", response.getRedirectedUrl());
    }

    @Test
    void sendMessageShouldHandleContentAtBoundaryLength5000() throws Exception {
        messageController.sendMessage("u01", "A".repeat(5000), response);

        verify(messageService).create(any(Message.class));
        assertEquals("/message_list", response.getRedirectedUrl());
    }

    @Test
    void sendMessageShouldHandleContentExceedingBoundaryLength5001() {
        RuntimeException ex = assertThrows(RuntimeException.class, () ->
            messageController.sendMessage("u01", "A".repeat(5001), response));

        assertEquals("留言超出长度超过上限", ex.getMessage());
    }
}
