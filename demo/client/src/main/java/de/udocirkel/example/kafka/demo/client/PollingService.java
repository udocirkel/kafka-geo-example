package de.udocirkel.example.kafka.demo.client;

import java.time.Duration;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.atomic.AtomicInteger;

import lombok.RequiredArgsConstructor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
@RequiredArgsConstructor
public class PollingService {

    private static final Logger LOG = LoggerFactory.getLogger(PollingService.class);

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final Map<Mode, ScheduledFuture<?>> scheduledTaskMap = new ConcurrentHashMap<>();
    private final Map<Mode, AtomicInteger> counters = new ConcurrentHashMap<>();

    private final RestTemplate restTemplate;

    private final TaskScheduler scheduler;

    @Value("${app.service.non-tx-url}")
    private String appServiceNonTxUrl;

    @Value("${app.service.tx-url}")
    private String appServiceTxUrl;

    public enum Mode {
        TX,
        NON_TX
    }

    public synchronized void start(Mode mode, Duration interval) {
        counters.putIfAbsent(mode, new AtomicInteger(0));
        if (scheduledTaskMap.containsKey(mode)) {
            throw new IllegalStateException("Mode " + mode + " is already running");
        }
        var task = createTask(mode);
        var scheduledTask = scheduler.scheduleAtFixedRate(task, interval);
        scheduledTaskMap.put(mode, scheduledTask);
    }

    public synchronized void stop(Mode mode) {
        var scheduledTask = scheduledTaskMap.remove(mode);
        if (scheduledTask != null) {
            scheduledTask.cancel(false);
        }
    }

    public synchronized boolean isRunning(Mode mode) {
        return scheduledTaskMap.get(mode) != null;
    }

    private Runnable createTask(Mode mode) {
        return () -> {

            int count = counters.get(mode).incrementAndGet();
            var timestamp = LocalDateTime.now().format(FORMATTER);
            var message = String.format(
                    "Hello from the Demo Client! [%s] Message number %d for mode %s.",
                    timestamp, count, mode);

            var headers = new HttpHeaders();
            headers.setContentType(MediaType.TEXT_PLAIN);

            var request = new HttpEntity<>(message, headers);
            var endpointUrl = (mode == Mode.TX) ? appServiceTxUrl : appServiceNonTxUrl;

            try {
                var result = restTemplate.postForEntity(endpointUrl, request, String.class);
                LOG.debug("Message send successful: [{}] Message number {} for mode {} with result {}.",
                        timestamp, count, mode, result);

            } catch (Exception e) {
                counters.get(mode).decrementAndGet();
                LOG.error("Message send successful: [{}] Message number {} for mode {}.", timestamp, count, mode, e);
            }
        };
    }

}
