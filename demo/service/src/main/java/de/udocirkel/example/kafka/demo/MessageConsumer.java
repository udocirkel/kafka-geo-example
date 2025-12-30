package de.udocirkel.example.kafka.demo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
public class MessageConsumer {

    private static final Logger LOG = LoggerFactory.getLogger(MessageConsumer.class);

    @Value("${app.topic}")
    private String topic;

    @KafkaListener(topics = "${app.topic}")
    public void receive(String message) {
        LOG.error("#### Received message '{}' from topic '{}'", message, topic);
    }

}
