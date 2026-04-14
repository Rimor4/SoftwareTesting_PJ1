package com.demo.service.impl;

import com.demo.dao.OrderDao;
import com.demo.dao.VenueDao;
import com.demo.entity.Order;
import com.demo.entity.Venue;
import com.demo.service.OrderService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderServiceImplTest {

    @Mock
    private OrderDao orderDao;

    @Mock
    private VenueDao venueDao;

    @InjectMocks
    private OrderServiceImpl orderService;

    @Test
    void submitShouldCreatePendingOrderAndCalculateTotal() {
        Venue venue = new Venue();
        venue.setVenueID(9);
        venue.setPrice(120);
        when(venueDao.findByVenueName("羽毛球馆")).thenReturn(venue);

        orderService.submit("羽毛球馆", LocalDateTime.of(2026, 4, 15, 10, 0), 2, "u1001");

        verify(orderDao).save(any(Order.class));
        verify(venueDao).findByVenueName("羽毛球馆");
    }

    @Test
    void updateOrderShouldResetStateAndOverwriteCoreFields() {
        Venue venue = new Venue();
        venue.setVenueID(3);
        venue.setPrice(80);
        Order order = new Order();
        order.setOrderID(12);
        order.setState(OrderService.STATE_FINISH);
        when(venueDao.findByVenueName("篮球馆")).thenReturn(venue);
        when(orderDao.findByOrderID(12)).thenReturn(order);

        LocalDateTime startTime = LocalDateTime.of(2026, 4, 16, 14, 0);
        orderService.updateOrder(12, "篮球馆", startTime, 3, "user-2");

        assertEquals(OrderService.STATE_NO_AUDIT, order.getState());
        assertEquals(3, order.getVenueID());
        assertEquals(3, order.getHours());
        assertEquals(240, order.getTotal());
        assertEquals("user-2", order.getUserID());
        assertEquals(startTime, order.getStartTime());
        verify(orderDao).save(order);
    }

    @Test
    void confirmOrderShouldUpdateStateWhenOrderExists() {
        Order order = new Order();
        order.setOrderID(22);
        when(orderDao.findByOrderID(22)).thenReturn(order);

        orderService.confirmOrder(22);

        verify(orderDao).updateState(OrderService.STATE_WAIT, 22);
    }

    @Test
    void confirmOrderShouldThrowWhenOrderMissing() {
        when(orderDao.findByOrderID(99)).thenReturn(null);

        RuntimeException ex = assertThrows(RuntimeException.class, () -> orderService.confirmOrder(99));

        assertEquals("订单不存在", ex.getMessage());
        verify(orderDao, never()).updateState(anyInt(), anyInt());
    }

    @Test
    void finishOrderShouldUpdateFinishState() {
        Order order = new Order();
        order.setOrderID(17);
        when(orderDao.findByOrderID(17)).thenReturn(order);

        orderService.finishOrder(17);

        verify(orderDao).updateState(OrderService.STATE_FINISH, 17);
    }

    @Test
    void rejectOrderShouldUpdateRejectState() {
        Order order = new Order();
        order.setOrderID(31);
        when(orderDao.findByOrderID(31)).thenReturn(order);

        orderService.rejectOrder(31);

        verify(orderDao).updateState(OrderService.STATE_REJECT, 31);
    }
}
