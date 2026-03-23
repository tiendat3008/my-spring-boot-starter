package com.spring.starter.user.controller;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HelloController {

    @GetMapping("/user")
    @PreAuthorize("hasRole('USER')")
    public String hello(Authentication authentication) {
        return "Hello user, " + authentication.getName() + "!";
    }

    @GetMapping("/admin")
    @PreAuthorize("hasRole('ADMIN')")
    public String admin(Authentication authentication) {
        return "Hello admin, " + authentication.getName() + "!";
    }

    @GetMapping("/admin/filter")
    public String adminFilter(Authentication authentication) {
        return "Hello admin, " + authentication.getName() + "!";
    }
}
