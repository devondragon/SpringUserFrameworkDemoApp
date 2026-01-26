package com.digitalsanctuary.spring.demo.controller;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Controller for admin pages. All endpoints in this controller require ADMIN_PRIVILEGE.
 */
@Slf4j
@RequiredArgsConstructor
@Controller
@RequestMapping("/admin")
public class AdminController {

    /**
     * Admin Actions Page.
     *
     * @return the path to the admin actions page
     */
    @GetMapping("/actions.html")
    @PreAuthorize("hasAuthority('ADMIN_PRIVILEGE')")
    public String adminActions() {
        log.debug("AdminController.adminActions: called.");
        return "admin/actions";
    }
}
