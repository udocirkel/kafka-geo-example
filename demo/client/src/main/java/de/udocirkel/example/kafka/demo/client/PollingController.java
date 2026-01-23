package de.udocirkel.example.kafka.demo.client;

import java.time.Duration;

import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/polling-jobs")
@RequiredArgsConstructor
class PollingController {

    private final PollingService pollingService;

    @PostMapping("/{mode}")
    public ResponseEntity<String> start(
            @PathVariable("mode") PollingService.Mode mode,
            @RequestParam("intervalMillis") long intervalMillis) {
        pollingService.start(mode, Duration.ofMillis(intervalMillis));
        return ResponseEntity.ok("Polling gestartet: Intervall=" + intervalMillis + "ms, Mode=" + mode);
    }

    @DeleteMapping("/{mode}")
    public ResponseEntity<String> stop(
            @PathVariable("mode") PollingService.Mode mode) {
        pollingService.stop(mode);
        return ResponseEntity.ok("Polling gestoppt");
    }

    @GetMapping("/{mode}")
    public ResponseEntity<String> status(
            @PathVariable("mode") PollingService.Mode mode) {
        return ResponseEntity.ok(pollingService.isRunning(mode) ? "RUNNING" : "STOPPED");
    }

}