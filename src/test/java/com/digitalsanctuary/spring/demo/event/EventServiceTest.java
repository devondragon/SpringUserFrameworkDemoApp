package com.digitalsanctuary.spring.demo.event;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.test.context.ActiveProfiles;

@ActiveProfiles("test")
public class EventServiceTest {

    @Mock
    private EventRepository eventRepository;

    @InjectMocks
    private EventService eventService;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    public void testGetAllEvents() {
        Event event1 = new Event();
        event1.setName("Concert");

        Event event2 = new Event();
        event2.setName("Meetup");

        when(eventRepository.findAll()).thenReturn(Arrays.asList(event1, event2));

        List<Event> events = eventService.getAllEvents();

        assertThat(events).hasSize(2);
    }

    @Test
    public void testGetEventById() {
        Event event = new Event();
        event.setName("Concert");

        when(eventRepository.findById(1L)).thenReturn(Optional.of(event));

        Optional<Event> foundEvent = eventService.getEventById(1L);

        assertThat(foundEvent).isPresent();
        assertThat(foundEvent.get().getName()).isEqualTo("Concert");
    }

    @Test
    public void testCreateEvent() {
        Event event = new Event();
        event.setName("Concert");

        when(eventRepository.save(event)).thenReturn(event);

        Event createdEvent = eventService.createEvent(event);

        assertThat(createdEvent).isNotNull();
        assertThat(createdEvent.getName()).isEqualTo("Concert");
    }

    @Test
    public void testUpdateEvent() {
        Event event = new Event();
        event.setName("Concert");

        when(eventRepository.findById(1L)).thenReturn(Optional.of(event));
        when(eventRepository.save(event)).thenReturn(event);

        Event updatedEvent = eventService.updateEvent(1L, event);

        assertThat(updatedEvent).isNotNull();
        assertThat(updatedEvent.getName()).isEqualTo("Concert");
    }

    @Test
    public void testDeleteEvent() {
        Event event = new Event();
        event.setName("Concert");

        when(eventRepository.findById(1L)).thenReturn(Optional.of(event));

        // Call the method being tested
        eventService.deleteEvent(1L);

        // Verify the repository's deleteById method was called with the correct ID
        verify(eventRepository, times(1)).deleteById(1L);

        // Optionally, verify that findById was not called again after the deletion
        verify(eventRepository, never()).findById(1L);
    }
}
