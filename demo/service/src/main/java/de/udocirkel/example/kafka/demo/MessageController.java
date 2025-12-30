package de.udocirkel.example.kafka.demo;

import lombok.RequiredArgsConstructor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

import java.util.Objects;

@RestController
@RequestMapping("/api/messages")
@RequiredArgsConstructor
public class MessageController {

    private static final Logger LOG = LoggerFactory.getLogger(MessageController.class);

    private final MessageProducer producer;

    @Value("${app.topic}")
    private String topic;

    @PostMapping(
            produces = {"text/plain"},
            consumes = {"text/plain"})
    public String send(@RequestBody String message) {
        Objects.requireNonNull(message, "Argument message is null");
        LOG.error("#### Send called with message: {}", message);
        try {
            producer.send(topic, message);
            return "ok";
        } catch (Exception e) {
            LOG.error("#### Error while calling message producer", e);
            throw new RuntimeException(e);
        }
    }

}
