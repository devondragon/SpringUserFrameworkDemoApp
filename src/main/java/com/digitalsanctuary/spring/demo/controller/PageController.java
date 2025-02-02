package com.digitalsanctuary.spring.demo.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.ui.Model;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import com.digitalsanctuary.spring.demo.event.EventService;

@Slf4j
@RequiredArgsConstructor
@Controller
public class PageController {

    private final EventService eventService;

    /**
     * Home Page.
     *
     * @return the path to the home page
     */
    @GetMapping({"/", "/index.html"})
    public String index() {
        log.info("PageController.index: called.");
        return "index";
    }

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
    @GetMapping("/event/details.html")
    public String eventDtails() {
        return "/event/details";
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

}
