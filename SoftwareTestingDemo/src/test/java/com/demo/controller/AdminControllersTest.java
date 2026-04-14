package com.demo.controller;

import com.demo.controller.admin.AdminMessageController;
import com.demo.controller.admin.AdminNewsController;
import com.demo.controller.admin.AdminOrderController;
import com.demo.controller.admin.AdminUserController;
import com.demo.controller.admin.AdminVenueController;
import com.demo.entity.Message;
import com.demo.entity.News;
import com.demo.entity.Order;
import com.demo.entity.User;
import com.demo.entity.Venue;
import com.demo.entity.vo.MessageVo;
import com.demo.entity.vo.OrderVo;
import com.demo.service.MessageService;
import com.demo.service.MessageVoService;
import com.demo.service.NewsService;
import com.demo.service.OrderService;
import com.demo.service.OrderVoService;
import com.demo.service.UserService;
import com.demo.service.VenueService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.ui.ConcurrentModel;
import org.springframework.ui.Model;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminControllersTest {

    @Mock
    private MessageService messageService;

    @Mock
    private MessageVoService messageVoService;

    @Mock
    private NewsService newsService;

    @Mock
    private OrderService orderService;

    @Mock
    private OrderVoService orderVoService;

    @Mock
    private UserService userService;

    @Mock
    private VenueService venueService;

    @InjectMocks
    private AdminMessageController adminMessageController;

    @InjectMocks
    private AdminNewsController adminNewsController;

    @InjectMocks
    private AdminOrderController adminOrderController;

    @InjectMocks
    private AdminUserController adminUserController;

    @InjectMocks
    private AdminVenueController adminVenueController;

    @Test
    void adminMessageControllerShouldReturnWaitMessages() {
        when(messageService.findWaitState(any(PageRequest.class)))
                .thenReturn(new PageImpl<Message>(Collections.singletonList(new Message()), PageRequest.of(0, 10), 1));
        when(messageVoService.returnVo(any())).thenReturn(Collections.singletonList(new MessageVo()));

        Model model = new ConcurrentModel();
        String view = adminMessageController.message_manage(model);

        assertEquals("admin/message_manage", view);
        assertEquals(1, adminMessageController.messageList(1).size());
        assertTrue(adminMessageController.passMessage(1));
        assertTrue(adminMessageController.rejectMessage(2));
    }

    @Test
    void adminNewsControllerShouldSupportCrudViewFlow() throws Exception {
        when(newsService.findAll(any(PageRequest.class)))
                .thenReturn(new PageImpl<News>(Collections.singletonList(new News()), PageRequest.of(0, 10), 1));
        News news = new News();
        news.setNewsID(7);
        when(newsService.findById(7)).thenReturn(news);
        MockHttpServletResponse response = new MockHttpServletResponse();

        Model model = new ConcurrentModel();
        assertEquals("admin/news_manage", adminNewsController.news_manage(model));
        assertEquals("/admin/news_edit", adminNewsController.news_edit(7, model));
        assertEquals(1, adminNewsController.newsList(1).size());
        assertTrue(adminNewsController.delNews(7));

        adminNewsController.modifyNews(7, "标题", "内容", response);
        verify(newsService).update(news);
        assertEquals("news_manage", response.getRedirectedUrl());
    }

    @Test
    void adminOrderControllerShouldReturnPendingOrdersAndUpdateState() {
        when(orderService.findAuditOrder()).thenReturn(Collections.singletonList(new Order()));
        when(orderVoService.returnVo(any())).thenReturn(Collections.singletonList(new OrderVo()));
        when(orderService.findNoAuditOrder(any(PageRequest.class)))
                .thenReturn(new PageImpl<Order>(Collections.singletonList(new Order()), PageRequest.of(0, 10), 1));

        Model model = new ConcurrentModel();
        assertEquals("admin/reservation_manage", adminOrderController.reservation_manage(model));
        assertEquals(1, adminOrderController.getNoAuditOrder(1).size());
        assertTrue(adminOrderController.confirmOrder(4));
        assertTrue(adminOrderController.rejectOrder(4));
    }

    @Test
    void adminUserControllerShouldSupportListAndUniquenessCheck() throws Exception {
        when(userService.findByUserID(any(PageRequest.class)))
                .thenReturn(new PageImpl<User>(Collections.singletonList(new User()), PageRequest.of(0, 10), 1));
        User user = new User();
        user.setUserID("old");
        when(userService.findById(3)).thenReturn(user);
        when(userService.findByUserID("old")).thenReturn(user);
        when(userService.countUserID("free")).thenReturn(0);
        MockHttpServletResponse response = new MockHttpServletResponse();

        Model model = new ConcurrentModel();
        assertEquals("admin/user_manage", adminUserController.user_manage(model));
        assertEquals(1, adminUserController.userList(1).size());
        assertEquals("admin/user_edit", adminUserController.user_edit(model, 3));
        adminUserController.modifyUser("new", "old", "Tom", "pwd", "a@test.com", "130", null, response);
        assertEquals("user_manage", response.getRedirectedUrl());
        assertTrue(adminUserController.checkUserID("free"));
        assertTrue(adminUserController.delUser(9));
    }

    @Test
    void adminVenueControllerShouldSupportListModifyAndUniquenessCheck() throws Exception {
        when(venueService.findAll(any(PageRequest.class)))
                .thenReturn(new PageImpl<Venue>(Collections.singletonList(new Venue()), PageRequest.of(0, 10), 1));
        Venue venue = new Venue();
        venue.setVenueID(6);
        when(venueService.findByVenueID(6)).thenReturn(venue);
        when(venueService.create(any(Venue.class))).thenReturn(6);
        when(venueService.countVenueName("羽毛球馆")).thenReturn(0);
        MockMultipartFile file = new MockMultipartFile("picture", "", "application/octet-stream", new byte[0]);
        MockHttpServletResponse response = new MockHttpServletResponse();

        Model model = new ConcurrentModel();
        assertEquals("admin/venue_manage", adminVenueController.venue_manage(model));
        assertEquals("/admin/venue_edit", adminVenueController.editVenue(model, 6));
        assertEquals(1, adminVenueController.getVenueList(1).size());
        adminVenueController.addVenue("羽毛球馆", "一号楼", "描述", 100, file, "08:00", "22:00", null, response);
        assertEquals("venue_manage", response.getRedirectedUrl());

        MockHttpServletResponse modifyResponse = new MockHttpServletResponse();
        adminVenueController.modifyVenue(6, "篮球馆", "二号楼", "新描述", 120, file, "09:00", "21:00", null, modifyResponse);
        verify(venueService).update(eq(venue));
        assertEquals("venue_manage", modifyResponse.getRedirectedUrl());
        assertTrue(adminVenueController.checkVenueName("羽毛球馆"));
        assertTrue(adminVenueController.delVenue(6));
    }
}
