package de.udocirkel.example.kafka.demo;

import lombok.RequiredArgsConstructor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MessageProducer {

    private static final Logger LOG = LoggerFactory.getLogger(MessageProducer.class);

    private final KafkaTemplate<String, String> kafka;

    public void send(String topic, String message) {
        try {
            kafka.send(topic, message);
            LOG.error("#### Sent message '{}' to topic '{}'", message, topic);
        } catch (Exception e) {
            LOG.error("#### Error while sending message", e);
            throw new RuntimeException(e);
        }
    }

}
