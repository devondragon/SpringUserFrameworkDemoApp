package com.digitalsanctuary.spring.demo.event;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import com.digitalsanctuary.spring.demo.user.profile.DemoUserProfile;
import com.digitalsanctuary.spring.demo.user.profile.EventRegistration;
import com.digitalsanctuary.spring.demo.user.profile.session.DemoSessionProfile;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
@Controller
public class EventPageController {

    private final EventService eventService;
    private final DemoSessionProfile demoSessionProfile;

    /**
     * Event Listing page
     *
     * @return the path to the event listing page
     */
    @GetMapping({"/event/", "/event/list.html"})
    public String eventList(Model model) {
        log.info("PageController.eventList: called.");
        try {
            log.debug("events: {}", eventService.getAllEvents());
            model.addAttribute("events", eventService.getAllEvents());
        } catch (Exception e) {
            log.error("Error getting events", e);
        }
        return "/event/list";
    }

    /**
     * Event Details Page
     *
     * @return the path to the event details page
     */
    @GetMapping("/event/{eventId}/details.html")
    public String eventDetails(@PathVariable("eventId") String eventId, Model model) {
        try {
            long id = Long.parseLong(eventId);
            Optional<Event> eventOpt = eventService.getEventById(id);

            if (eventOpt.isEmpty()) {
                log.warn("Event not found for eventId: {}", id);
                return "redirect:/event/list.html";
            }

            Event event = eventOpt.get();
            model.addAttribute("event", event);
            // Add registration flag for authenticated users
            DemoUserProfile profile = demoSessionProfile.getUserProfile();
            if (profile != null) {
                // Refresh profile to get latest registration status
                profile = demoSessionProfile.refreshProfile();
                if (profile != null) {
                    boolean isRegistered = profile.isRegisteredForEvent(event);
                    model.addAttribute("isRegistered", isRegistered);
                }
            }
            return "/event/details";

        } catch (NumberFormatException e) {
            log.warn("Invalid eventId format: {}", eventId);
            return "redirect:/event/list.html";
        } catch (Exception e) {
            log.error("Unexpected error retrieving event details for eventId: {}", eventId, e);
            return "redirect:/error"; // Redirect to a generic error page
        }
    }

    /**
     * Event Creation Page
     *
     * @return the path to the event creation page
     */
    @GetMapping("/event/create.html")
    public String eventCreate() {
        return "/event/create";
    }

    /**
     * My Events Page
     *
     * @return the path to the my events page
     */
    @GetMapping("/event/my-events.html")
    public String myEvents(Model model) {
        DemoUserProfile profile = demoSessionProfile.getUserProfile();
        List<Event> myEvents = new ArrayList<>();
        if (profile != null) {
            // Refresh the profile to ensure we have latest event registrations
            profile = demoSessionProfile.refreshProfile();
            if (profile != null) {
                myEvents = profile.getEventRegistrations().stream().map(EventRegistration::getEvent).collect(Collectors.toList());
            }
        }
        model.addAttribute("myEvents", myEvents);
        return "/event/my-events";
    }
}
