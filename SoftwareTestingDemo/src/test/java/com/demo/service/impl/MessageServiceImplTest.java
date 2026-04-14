package com.demo.service.impl;

import com.demo.dao.MessageDao;
import com.demo.entity.Message;
import com.demo.service.MessageService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.anyInt;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MessageServiceImplTest {

    @Mock
    private MessageDao messageDao;

    @InjectMocks
    private MessageServiceImpl messageService;

    @Test
    void confirmMessageShouldUpdatePassState() {
        Message message = new Message();
        message.setMessageID(5);
        when(messageDao.findByMessageID(5)).thenReturn(message);

        messageService.confirmMessage(5);

        verify(messageDao).updateState(MessageService.STATE_PASS, 5);
    }

    @Test
    void confirmMessageShouldThrowWhenMessageMissing() {
        when(messageDao.findByMessageID(7)).thenReturn(null);

        RuntimeException ex = assertThrows(RuntimeException.class, () -> messageService.confirmMessage(7));

        assertEquals("留言不存在", ex.getMessage());
        verify(messageDao, never()).updateState(anyInt(), anyInt());
    }

    @Test
    void rejectMessageShouldUpdateRejectState() {
        Message message = new Message();
        message.setMessageID(11);
        when(messageDao.findByMessageID(11)).thenReturn(message);

        messageService.rejectMessage(11);

        verify(messageDao).updateState(MessageService.STATE_REJECT, 11);
    }
}
