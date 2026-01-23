package de.udocirkel.example.kafka.demo.service;

import java.util.Optional;

public record MessageRecord(
        String topic,
        String key,
        int partition,
        Optional<Integer> leaderEpoch,
        long offset,
        long timestamp,
        String value) {
}
