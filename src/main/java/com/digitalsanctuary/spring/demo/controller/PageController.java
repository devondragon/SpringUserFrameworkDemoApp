package com.digitalsanctuary.spring.demo.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
@Controller
public class PageController {

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
     * About Page.
     *
     * @return the path to the about page
     */
    @GetMapping("/about.html")
    public String about() {
        return "about";
    }

    /**
     * Privacy Page.
     *
     * @return the path to the privacy page
     */
    @GetMapping("/privacy.html")
    public String privacy() {
        return "privacy";
    }

    /**
     * Terms Page.
     *
     * @return the path to the terms page
     */
    @GetMapping("/terms.html")
    public String terms() {
        return "terms";
    }


}
