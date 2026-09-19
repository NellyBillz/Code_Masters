package za.codemaster.backend.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import za.codemaster.backend.dto.common.HealthResponse;

import java.time.OffsetDateTime;

/**
 * Operational health check endpoint.
 * <p>
 * Public, unauthenticated (see {@link za.codemaster.backend.config.SecurityConfig}).
 * Matches the {@code /health} path in {@code codemasters-api-spec.yaml} v2.1.
 */
@RestController
public class HealthController {

    @GetMapping("/api/v1/health")
    public HealthResponse health() {
        return new HealthResponse("UP", OffsetDateTime.now());
    }
}
