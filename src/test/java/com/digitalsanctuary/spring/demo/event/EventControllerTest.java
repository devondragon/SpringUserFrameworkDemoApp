package com.digitalsanctuary.spring.demo.event;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import com.digitalsanctuary.spring.demo.SecurityTestUtils;
import com.fasterxml.jackson.databind.ObjectMapper;

@ActiveProfiles("test")
@WebMvcTest(EventController.class) // Focus ONLY on the EventController
public class EventControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private EventService eventService; // Mock the service dependency

    @Autowired
    private ObjectMapper objectMapper;

    private Event event;

    @BeforeEach
    public void setUp() {
        event = new Event();
        event.setId(1L);
        event.setName("Concert");
        event.setDescription("Music concert");
        event.setLocation("Stadium");
        event.setDate(LocalDate.parse("2023-12-01"));
        event.setTime(LocalTime.parse("18:00"));
    }

    @Test
    public void testGetAllEvents() throws Exception {
        when(eventService.getAllEvents()).thenReturn(List.of(event));

        mockMvc.perform(get("/api/events").with(SecurityTestUtils.mockUserWithCsrf())).andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Concert"));
    }


    @Test
    public void testGetEventById() throws Exception {
        when(eventService.getEventById(1L)).thenReturn(Optional.of(event));

        mockMvc.perform(get("/api/events/1").with(SecurityTestUtils.mockUserWithCsrf())).andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Concert"));
    }

    @Test
    public void testCreateEvent() throws Exception {
        when(eventService.createEvent(event)).thenReturn(event);

        mockMvc.perform(post("/api/events").contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(event))
                .with(SecurityTestUtils.mockUserWithCsrf())).andExpect(status().isOk()).andExpect(jsonPath("$.name").value("Concert"));
    }

    @Test
    public void testUpdateEvent() throws Exception {
        when(eventService.updateEvent(1L, event)).thenReturn(event);

        mockMvc.perform(put("/api/events/1").contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(event))
                .with(SecurityTestUtils.mockUserWithCsrf())).andExpect(status().isOk()).andExpect(jsonPath("$.name").value("Concert"));
    }

    @Test
    public void testDeleteEvent() throws Exception {
        mockMvc.perform(delete("/api/events/1").with(SecurityTestUtils.mockUserWithCsrf())).andExpect(status().isNoContent());
    }

}
