package com.demo.controller.user;

import com.demo.entity.Order;
import com.demo.entity.User;
import com.demo.entity.Venue;
import com.demo.entity.vo.OrderVo;
import com.demo.entity.vo.VenueOrder;
import com.demo.exception.LoginException;
import com.demo.service.OrderService;
import com.demo.service.OrderVoService;
import com.demo.service.VenueService;
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

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderControllerTest {

    @Mock
    private OrderService orderService;

    @Mock
    private OrderVoService orderVoService;

    @Mock
    private VenueService venueService;

    @InjectMocks
    private OrderController orderController;

    private MockHttpServletRequest request;
    private MockHttpServletResponse response;

    @BeforeEach
    void setUp() {
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
    }

    @Test
    void orderManageShouldRejectAnonymousAccess() {
        Model model = new ConcurrentModel();

        assertThrows(LoginException.class, () -> orderController.order_manage(model, request));
    }

    @Test
    void orderManageShouldExposePageCountForLoggedInUser() {
        User user = new User();
        user.setUserID("u01");
        request.getSession().setAttribute("user", user);
        when(orderService.findUserOrder(eq("u01"), any(PageRequest.class)))
                .thenReturn(new PageImpl<Order>(Collections.singletonList(new Order()), PageRequest.of(0, 5), 6));

        Model model = new ConcurrentModel();
        String view = orderController.order_manage(model, request);

        assertEquals("order_manage", view);
        assertEquals(2, model.getAttribute("total"));
    }

    @Test
    void addOrderShouldParseTimeAndRedirect() throws Exception {
        User user = new User();
        user.setUserID("u01");
        request.getSession().setAttribute("user", user);

        orderController.addOrder("羽毛球馆", "2026-04-20", "2026-04-20 09:00", 2, request, response);

        verify(orderService).submit(eq("羽毛球馆"), eq(LocalDateTime.of(2026, 4, 20, 9, 0)), eq(2), eq("u01"));
        assertEquals("order_manage", response.getRedirectedUrl());
    }

    @Test
    void modifyOrderShouldReuseSessionUserAndRedirect() throws Exception {
        User user = new User();
        user.setUserID("u01");
        request.getSession().setAttribute("user", user);

        boolean result = orderController.modifyOrder("羽毛球馆", "2026-04-20", "2026-04-20 11:00", 3, 88, request, response);

        assertTrue(result);
        verify(orderService).updateOrder(eq(88), eq("羽毛球馆"), eq(LocalDateTime.of(2026, 4, 20, 11, 0)), eq(3), eq("u01"));
        assertEquals("order_manage", response.getRedirectedUrl());
    }

    @Test
    void getOrderListShouldReturnConvertedVos() {
        User user = new User();
        user.setUserID("u01");
        request.getSession().setAttribute("user", user);
        OrderVo vo = new OrderVo();
        vo.setOrderID(1);
        when(orderService.findUserOrder(eq("u01"), any(PageRequest.class)))
                .thenReturn(new PageImpl<Order>(Collections.singletonList(new Order()), PageRequest.of(0, 5), 1));
        when(orderVoService.returnVo(any())).thenReturn(Collections.singletonList(vo));

        assertEquals(1, orderController.order_list(1, request).size());
    }

    @Test
    void getVenueOrderShouldPackageVenueAndDailyOrders() {
        Venue venue = new Venue();
        venue.setVenueID(4);
        when(venueService.findByVenueName("篮球馆")).thenReturn(venue);
        when(orderService.findDateOrder(eq(4), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(Arrays.asList(new Order(), new Order()));

        VenueOrder result = orderController.getOrder("篮球馆", "2026-04-21");

        assertEquals(venue, result.getVenue());
        assertEquals(2, result.getOrders().size());
    }

    @Test
    void deleteOrderShouldDelegateAndReturnTrue() {
        assertTrue(orderController.delOrder(5));
        verify(orderService).delOrder(5);
    }

    @Test
    void addOrderShouldHandleUserIDAtBoundaryLength24() throws Exception {
        User user = new User();
        user.setUserID("A".repeat(24));
        request.getSession().setAttribute("user", user);

        orderController.addOrder("羽毛球馆", "2026-04-20", "2026-04-20 09:00", 2, request, response);

        verify(orderService).submit(eq("羽毛球馆"), eq(LocalDateTime.of(2026, 4, 20, 9, 0)), eq(2), eq("A".repeat(24)));
        assertEquals("order_manage", response.getRedirectedUrl());
    }

    @Test
    void addOrderShouldHandleUserIDAtBoundaryLength25() throws Exception {
        User user = new User();
        user.setUserID("A".repeat(25));
        request.getSession().setAttribute("user", user);

        orderController.addOrder("羽毛球馆", "2026-04-20", "2026-04-20 09:00", 2, request, response);

        verify(orderService).submit(eq("羽毛球馆"), eq(LocalDateTime.of(2026, 4, 20, 9, 0)), eq(2), eq("A".repeat(25)));
        assertEquals("order_manage", response.getRedirectedUrl());
    }

    @Test
    void addOrderShouldHandleUserIDExceedingBoundaryLength26() {
        User user = new User();
        user.setUserID("A".repeat(26));
        request.getSession().setAttribute("user", user);

        RuntimeException ex = assertThrows(RuntimeException.class, () ->
            orderController.addOrder("羽毛球馆", "2026-04-20", "2026-04-20 09:00", 2, request, response));

        assertEquals("用户ID超出长度上限", ex.getMessage());
    }

    @Test
    void addOrderShouldHandleEmptyUserID() {
        User user = new User();
        user.setUserID("");
        request.getSession().setAttribute("user", user);

        RuntimeException ex = assertThrows(RuntimeException.class, () ->
            orderController.addOrder("羽毛球馆", "2026-04-20", "2026-04-20 09:00", 2, request, response));

        assertEquals("用户ID不能为空", ex.getMessage());
    }
}
