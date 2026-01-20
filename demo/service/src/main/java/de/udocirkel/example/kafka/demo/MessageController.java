package de.udocirkel.example.kafka.demo;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.http.MediaType.TEXT_PLAIN_VALUE;

import java.util.List;

import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/messages")
@RequiredArgsConstructor
public class MessageController {

    private final MessageProducer producer;

    private final MessageConsumer consumer;

    private final MessageReader reader;

    @PostMapping(
            produces = TEXT_PLAIN_VALUE,
            consumes = TEXT_PLAIN_VALUE)
    @ResponseStatus(HttpStatus.ACCEPTED)
    public String send(@RequestBody String message) {
        producer.send(message);
        return "OK";
    }

    @PostMapping(
            path = "/tx",
            produces = TEXT_PLAIN_VALUE,
            consumes = TEXT_PLAIN_VALUE)
    @ResponseStatus(HttpStatus.ACCEPTED)
    public String sendTransactional(@RequestBody String message) {
        producer.sendTransactional(message);
        return "OK";
    }

    @GetMapping(
            produces = APPLICATION_JSON_VALUE)
    public List<MessageRecord> readFromOffset(
            @RequestParam(name = "partition") int partition,
            @RequestParam(name = "offset") long offset,
            @RequestParam(name = "limit", defaultValue = "100") int limit) {
        return reader.readFromOffset(partition, offset, limit);
    }

    @PostMapping("/replay-all")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public String replayAll() {
        consumer.replayAll();
        return "OK";
    }

}
