package com.digitalsanctuary.spring.demo.event;

import java.util.List;
import java.util.Optional;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.digitalsanctuary.spring.demo.user.profile.DemoUserProfile;
import com.digitalsanctuary.spring.demo.user.profile.DemoUserProfileService;
import com.digitalsanctuary.spring.demo.user.profile.session.DemoSessionProfile;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/events")
public class EventAPIController {

    private final DemoSessionProfile demoSessionProfile;

    private final EventService eventService;

    private final DemoUserProfileService demoUserProfileService;

    @GetMapping
    public List<Event> getAllEvents() {
        return eventService.getAllEvents();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Event> getEventById(@PathVariable Long id) {
        Event event = eventService.getEventById(id).orElseThrow(() -> new RuntimeException("Event not found"));
        return ResponseEntity.ok(event);
    }

    @PostMapping
    @PreAuthorize("hasAuthority('CREATE_EVENT_PRIVILEGE')")
    public Event createEvent(@RequestBody Event event) {
        return eventService.createEvent(event);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('UPDATE_EVENT_PRIVILEGE')")
    public ResponseEntity<Event> updateEvent(@PathVariable Long id, @RequestBody Event eventDetails) {
        Event updatedEvent = eventService.updateEvent(id, eventDetails);
        return ResponseEntity.ok(updatedEvent);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('DELETE_EVENT_PRIVILEGE')")
    public ResponseEntity<Void> deleteEvent(@PathVariable Long id) {
        eventService.deleteEvent(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * This endpoint allows Users with the 'REGISTER_FOR_EVENT_PRIVILEGE' authority to register for an Event. This uses the DemoUserProfileService to
     * associate the User with the Event.
     *
     */
    @PostMapping("/{eventId}/register")
    @PreAuthorize("hasAuthority('REGISTER_FOR_EVENT_PRIVILEGE')")
    @Transactional
    public ResponseEntity<Event> registerForEvent(@PathVariable Long eventId) {
        Optional<Event> event = eventService.getEventById(eventId);
        if (event.isEmpty()) {
            log.info("Event not found with id: {}", eventId);
            return ResponseEntity.notFound().build();
        }
        DemoUserProfile userProfile = demoSessionProfile.getUserProfile();
        if (userProfile == null) {
            log.info("User not found in session");
            return ResponseEntity.badRequest().build();
        }
        demoUserProfileService.registerForEvent(userProfile, event.get());
        log.info("User registered for event: {}", eventId);
        return ResponseEntity.ok(event.get());
    }


    /**
     * This endpoint allows Users with the 'REGISTER_FOR_EVENT_PRIVILEGE' authority to unregister for an Event. This uses the DemoUserProfileService
     * to disassociate the User with the Event.
     *
     */
    @PostMapping("/{eventId}/unregister")
    @PreAuthorize("hasAuthority('REGISTER_FOR_EVENT_PRIVILEGE')")
    public ResponseEntity<Event> unregisterFromEvent(@PathVariable Long eventId) {
        Optional<Event> event = eventService.getEventById(eventId);
        if (event.isEmpty()) {
            log.info("Event not found with id: {}", eventId);
            return ResponseEntity.notFound().build();
        }
        DemoUserProfile userProfile = demoSessionProfile.getUserProfile();
        if (userProfile == null) {
            log.info("User not found in session");
            return ResponseEntity.badRequest().build();
        }

        demoUserProfileService.unregisterFromEvent(userProfile, event.get());
        log.info("User unregistered from event: {}", eventId);
        return ResponseEntity.ok(event.get());
    }

}
