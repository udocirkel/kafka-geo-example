package de.udocirkel.example.kafka.demo;

import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.common.TopicPartition;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.listener.ConsumerSeekAware;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

@Service
public class MessageConsumer implements ConsumerSeekAware {

    private static final Logger LOG = LoggerFactory.getLogger(MessageConsumer.class);

    private ConsumerSeekCallback seekCallback;

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    @Value("${app.topic.name}")
    private String topic;

    @Value("${app.topic.name-tx}")
    private String topicTx;

    @KafkaListener(topics = "${app.topic.name}")
    public void receive(String message) {
        LOG.debug("Message receive STARTED [topic={}, tx=false, value={}]", topic, message);
        if (message.contains("fail-late")) {
            throw new RuntimeException("Simulated consumer error");
        }
        LOG.debug("Message receive OK [topic={}, tx=false, value={}]", topic, message);
    }

    @KafkaListener(topics = "${app.topic.name-tx}", containerFactory = "txKafkaListenerContainerFactory")
    public void receiveTx(String message) {
        LOG.debug("Message receive STARTED in transaction [topic={}, tx=true, value={}]", topicTx, message);
        if (message.contains("fail-late")) {
            throw new RuntimeException("Simulated consumer error");
        }
        LOG.debug("Message receive OK [topic={}, tx=true, value={}]", topicTx, message);
    }

    @Override
    public void registerSeekCallback(ConsumerSeekCallback callback) {
        this.seekCallback = callback;
    }

    public void replayAll() {
        LOG.debug("Message replay STARTED [topic={}]", topic);
        if (seekCallback == null) {
            LOG.warn("Seek callback not registered yet");
            return;
        }
        var partitions = getPartitions(topic);
        seekCallback.seekToBeginning(partitions);
        LOG.debug("Message replay OK [topic={}]", topic);
    }

    private List<TopicPartition> getPartitions(String topic) {
        try (AdminClient admin = AdminClient.create(Map.of(
                "bootstrap.servers", bootstrapServers
        ))) {
            var partitions = new ArrayList<TopicPartition>();
            admin
                    .describeTopics(List.of(topic))
                    .allTopicNames()
                    .get()
                    .get(topic)
                    .partitions()
                    .forEach(p ->
                            partitions.add(new TopicPartition(topic, p.partition()))
                    );
            return partitions;
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException("Error fetching partitions", e);
        }
    }

}
