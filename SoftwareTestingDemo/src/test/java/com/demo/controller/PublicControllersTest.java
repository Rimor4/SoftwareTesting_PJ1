package com.demo.controller;

import com.demo.controller.user.NewsController;
import com.demo.controller.user.VenueController;
import com.demo.entity.Message;
import com.demo.entity.News;
import com.demo.entity.Venue;
import com.demo.entity.vo.MessageVo;
import com.demo.service.MessageService;
import com.demo.service.MessageVoService;
import com.demo.service.NewsService;
import com.demo.service.VenueService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.ui.ConcurrentModel;
import org.springframework.ui.Model;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PublicControllersTest {

    @Mock
    private NewsService newsService;

    @Mock
    private VenueService venueService;

    @Mock
    private MessageVoService messageVoService;

    @Mock
    private MessageService messageService;

    @InjectMocks
    private IndexController indexController;

    @InjectMocks
    private NewsController newsController;

    @InjectMocks
    private VenueController venueController;

    @Test
    void indexShouldAggregateHomepageData() {
        when(venueService.findAll(any(PageRequest.class)))
                .thenReturn(new PageImpl<Venue>(Collections.singletonList(new Venue()), PageRequest.of(0, 5), 1));
        when(newsService.findAll(any(PageRequest.class)))
                .thenReturn(new PageImpl<News>(Collections.singletonList(new News()), PageRequest.of(0, 5), 1));
        when(messageService.findPassState(any(PageRequest.class)))
                .thenReturn(new PageImpl<Message>(Collections.singletonList(new Message()), PageRequest.of(0, 5), 1));
        when(messageVoService.returnVo(any())).thenReturn(Collections.singletonList(new MessageVo()));

        Model model = new ConcurrentModel();
        assertEquals("index", indexController.index(model));
        assertEquals("admin/admin_index", indexController.admin_index(model));
    }

    @Test
    void newsControllerShouldReturnPageAndDetails() {
        News news = new News();
        when(newsService.findById(8)).thenReturn(news);
        when(newsService.findAll(any(PageRequest.class)))
                .thenReturn(new PageImpl<News>(Collections.singletonList(news), PageRequest.of(0, 5), 1));

        Model model = new ConcurrentModel();
        assertEquals("news", newsController.news(model, 8));
        assertEquals(1, newsController.news_list(1).getContent().size());
        assertEquals("news_list", newsController.news_list(model));
    }

    @Test
    void venueControllerShouldReturnPageAndDetails() {
        Venue venue = new Venue();
        when(venueService.findByVenueID(5)).thenReturn(venue);
        when(venueService.findAll(any(PageRequest.class)))
                .thenReturn(new PageImpl<Venue>(Collections.singletonList(venue), PageRequest.of(0, 5), 1));

        Model model = new ConcurrentModel();
        assertEquals("venue", venueController.toGymPage(model, 5));
        assertEquals(1, venueController.venue_list(1).getContent().size());
        assertEquals("venue_list", venueController.venue_list(model));
    }
}
