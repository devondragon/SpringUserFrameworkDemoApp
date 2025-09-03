package com.digitalsanctuary.spring.demo.event;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import com.digitalsanctuary.spring.demo.SecurityTestUtils;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import com.digitalsanctuary.spring.demo.user.profile.DemoUserProfileService;
import com.digitalsanctuary.spring.demo.user.profile.session.DemoSessionProfile;
import com.fasterxml.jackson.databind.ObjectMapper;

@WebMvcTest(EventAPIController.class)
@ActiveProfiles("test")
public class EventAPIControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private EventService eventService;

    @MockitoBean
    private DemoUserProfileService demoUserProfileService;

    @MockitoBean
    private DemoSessionProfile demoSessionProfile;

    @Autowired
    private ObjectMapper objectMapper;

    private Event testEvent;
    private List<Event> testEvents;

    @BeforeEach
    public void setUp() {
        testEvent = new Event();
        testEvent.setId(1L);
        testEvent.setName("Test Event");
        testEvent.setDescription("Test Description");
        testEvent.setLocation("Test Location");
        testEvent.setDate(LocalDate.now().plusDays(1));
        testEvent.setTime(LocalTime.of(14, 30));

        Event event2 = new Event();
        event2.setId(2L);
        event2.setName("Another Event");
        event2.setDescription("Another Description");
        event2.setLocation("Another Location");
        event2.setDate(LocalDate.now().plusDays(2));
        event2.setTime(LocalTime.of(16, 0));

        testEvents = Arrays.asList(testEvent, event2);
    }

    @Test
    public void testGetAllEvents() throws Exception {
        when(eventService.getAllEvents()).thenReturn(testEvents);

        mockMvc.perform(get("/api/events").with(SecurityTestUtils.mockUserWithCsrf()))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].name", is("Test Event")))
                .andExpect(jsonPath("$[1].name", is("Another Event")));

        verify(eventService).getAllEvents();
    }

    @Test
    public void testGetEventById() throws Exception {
        when(eventService.getEventById(1L)).thenReturn(Optional.of(testEvent));

        mockMvc.perform(get("/api/events/1").with(SecurityTestUtils.mockUserWithCsrf()))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.name", is("Test Event")))
                .andExpect(jsonPath("$.description", is("Test Description")))
                .andExpect(jsonPath("$.location", is("Test Location")));

        verify(eventService).getEventById(1L);
    }

    @Test
    public void testGetEventByIdNotFound() throws Exception {
        when(eventService.getEventById(999L)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/events/999").with(SecurityTestUtils.mockUserWithCsrf()))
                .andExpect(status().isNotFound());

        verify(eventService).getEventById(999L);
    }

    @Test
    public void testCreateEventWithValidData() throws Exception {
        when(eventService.createEvent(any(Event.class))).thenReturn(testEvent);

        mockMvc.perform(post("/api/events")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(testEvent))
                .with(SecurityTestUtils.mockUserWithCsrf()))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.name", is("Test Event")));

        verify(eventService).createEvent(any(Event.class));
    }

    @Test
    public void testCreateEventWithoutAuth() throws Exception {
        mockMvc.perform(post("/api/events")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(testEvent)))
                .andExpect(status().isForbidden());
    }

    @Test
    public void testCreateEventWithInvalidData() throws Exception {
        Event invalidEvent = new Event();
        invalidEvent.setName(""); // Invalid - blank name
        invalidEvent.setDate(LocalDate.now().minusDays(1)); // Invalid - past date

        mockMvc.perform(post("/api/events")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidEvent))
                .with(SecurityTestUtils.mockUserWithCsrf()))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void testUpdateEvent() throws Exception {
        when(eventService.updateEvent(eq(1L), any(Event.class))).thenReturn(testEvent);

        mockMvc.perform(put("/api/events/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(testEvent))
                .with(SecurityTestUtils.mockUserWithCsrf()))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.name", is("Test Event")));

        verify(eventService).updateEvent(eq(1L), any(Event.class));
    }

    @Test
    public void testUpdateEventNotFound() throws Exception {
        when(eventService.updateEvent(eq(999L), any(Event.class)))
                .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Event not found"));

        mockMvc.perform(put("/api/events/999")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(testEvent))
                .with(SecurityTestUtils.mockUserWithCsrf()))
                .andExpect(status().isNotFound());

        verify(eventService).updateEvent(eq(999L), any(Event.class));
    }

    @Test
    public void testDeleteEvent() throws Exception {
        doNothing().when(eventService).deleteEvent(1L);

        mockMvc.perform(delete("/api/events/1").with(SecurityTestUtils.mockUserWithCsrf()))
                .andExpect(status().isNoContent());

        verify(eventService).deleteEvent(1L);
    }

    @Test
    public void testDeleteEventWithoutAuth() throws Exception {
        mockMvc.perform(delete("/api/events/1"))
                .andExpect(status().isForbidden());
    }
}