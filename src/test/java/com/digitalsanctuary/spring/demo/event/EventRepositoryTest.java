package com.digitalsanctuary.spring.demo.event;

import static org.assertj.core.api.Assertions.assertThat;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

@ActiveProfiles("test")
@DataJpaTest
@EntityScan(basePackages = {"com.digitalsanctuary.spring.user.persistence.model", // Include User entity
        "com.digitalsanctuary.spring.demo.user.profile", // Include DemoUserProfile
        "com.digitalsanctuary.spring.demo.event" // Include Event
})
public class EventRepositoryTest {

    @Autowired
    private EventRepository eventRepository;

    @Test
    public void testSaveEvent() {
        Event event = new Event();
        event.setName("Concert");
        event.setDescription("Music concert");
        event.setLocation("Stadium");
        event.setDate(LocalDate.parse("2023-12-01"));
        event.setTime(LocalTime.parse("18:00"));

        Event savedEvent = eventRepository.save(event);

        assertThat(savedEvent).isNotNull();
        assertThat(savedEvent.getId()).isNotNull();
    }

    @Test
    public void testFindById() {
        Event event = new Event();
        event.setName("Concert");
        event.setDescription("Music concert");
        event.setLocation("Stadium");
        event.setDate(LocalDate.parse("2023-12-01"));
        event.setTime(LocalTime.parse("18:00"));

        Event savedEvent = eventRepository.save(event);
        Optional<Event> foundEvent = eventRepository.findById(savedEvent.getId());

        assertThat(foundEvent).isPresent();
        assertThat(foundEvent.get().getName()).isEqualTo("Concert");
    }

    @Test
    public void testFindAll() {
        Event event1 = new Event();
        event1.setName("Concert");
        event1.setDescription("Music concert");
        event1.setLocation("Stadium");
        event1.setDate(LocalDate.parse("2023-12-01"));
        event1.setTime(LocalTime.parse("18:00"));

        Event event2 = new Event();
        event2.setName("Meetup");
        event2.setDescription("Tech meetup");
        event2.setLocation("Conference Hall");
        event2.setDate(LocalDate.parse("2023-12-05"));
        event2.setTime(LocalTime.parse("10:00"));

        eventRepository.save(event1);
        eventRepository.save(event2);

        List<Event> events = eventRepository.findAll();

        assertThat(events).hasSize(2);
    }
}
