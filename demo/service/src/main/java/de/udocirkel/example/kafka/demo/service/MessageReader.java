package de.udocirkel.example.kafka.demo.service;

import java.time.Duration;

import java.util.*;

import org.apache.kafka.clients.consumer.*;
import org.apache.kafka.common.TopicPartition;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class MessageReader {

    private static final Logger LOG = LoggerFactory.getLogger(MessageReader.class);

    private static final String READER_GROUP_ID = "demo-rest-reader-group";

    private final Properties consumerProps;

    @Value("${app.topic.name}")
    private String topic;

    public MessageReader(@Value("${spring.kafka.bootstrap-servers}") String bootstrapServers) {
        this.consumerProps = createConsumerProperties(bootstrapServers);
    }

    private Properties createConsumerProperties(String bootstrapServers) {
        var props = new Properties();

        // Kafka bootstrap servers used to initially connect and discover the cluster
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);

        // Deserializer for the message key and value
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringDeserializer");
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringDeserializer");

        // Consumer group ID – ensures offset tracking and enables load balancing across consumers
        props.put(ConsumerConfig.GROUP_ID_CONFIG, READER_GROUP_ID);

        // Determines behavior when the requested offset does not exist, "earliest" starts from the beginning of the partition
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "none"); // enforce manual offset

        // Disable auto-commit so that offsets are not automatically marked as read, this allows precise control over which messages are considered processed
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "false");

        // Limits blocking if brokers are temporarily offline
        props.put(ConsumerConfig.MAX_POLL_INTERVAL_MS_CONFIG, "1000");  // Maximum time between calls to poll() before the consumer is considered dead
        props.put(ConsumerConfig.REQUEST_TIMEOUT_MS_CONFIG, "2000");    // Request timeout for broker communication – prevents long blocking if broker is unavailable

        return props;
    }

    public List<MessageRecord> readFromOffset(int partition, long offset, int limit) {
        LOG.debug("Message read STARTED [topic={}, partition={}, offset={}, limit={}]", topic, partition, offset, limit);
        try (var consumer = new KafkaConsumer<String, String>(consumerProps)) {

            var tp = new TopicPartition(topic, partition);
            if (!partitionExists(consumer, tp)) {
                return List.of();
            }

            seekPartition(consumer, tp, offset);

            var records = collectMessagesFromPartition(consumer, tp, limit, 5000);
            LOG.debug("Message read OK [topic={}, partition={}, offset={}, limit={}]", topic, partition, offset, limit);
            return records;

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Interrupted while reading messages", e);
        } catch (Exception e) {
            throw new RuntimeException(
                    "Error reading messages from topic: " + topic + ", partition: " + partition, e);
        }
    }

    private boolean partitionExists(KafkaConsumer<String, String> consumer, TopicPartition tp) {
        return consumer.partitionsFor(tp.topic()).stream()
                .anyMatch(info -> info.partition() == tp.partition());
    }

    private void seekPartition(KafkaConsumer<String, String> consumer, TopicPartition tp, long offset) {
        consumer.assign(List.of(tp));

        long beginning = consumer.beginningOffsets(List.of(tp)).get(tp);
        long end = consumer.endOffsets(List.of(tp)).get(tp);

        if (offset >= beginning && offset < end) {
            consumer.seek(tp, offset);
        } else if (offset < beginning) {
            consumer.seek(tp, beginning);
        } else {
            consumer.seek(tp, end); // offset zu groß → poll liefert leer
        }
    }

    private List<MessageRecord> collectMessagesFromPartition(KafkaConsumer<String, String> consumer, TopicPartition tp, int limit, long maxWaitMs) throws InterruptedException {
        var result = new ArrayList<MessageRecord>();
        long startTime = System.currentTimeMillis();
        while (result.size() < limit && System.currentTimeMillis() - startTime < maxWaitMs) {
            var records = consumer.poll(Duration.ofMillis(500));
            for (var r : records.records(tp)) {
                result.add(new MessageRecord(r.topic(), r.key(), r.partition(), r.leaderEpoch(), r.offset(), r.timestamp(), r.value()));
                if (result.size() >= limit) {
                    break;
                }
            }
            if (records.isEmpty()) {
                Thread.sleep(100);
            }
        }
        return result;
    }

}
