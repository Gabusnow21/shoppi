package com.shoppi.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;

/**
 * Endpoint de verificación de vida del backend.
 */
@RestController
@RequestMapping("/api")
public class PingController {

    @GetMapping("/ping")
    public PingResponse ping() {
        return new PingResponse("UP", "shoppi", Instant.now());
    }

    public record PingResponse(String status, String service, Instant timestamp) {
    }
}
