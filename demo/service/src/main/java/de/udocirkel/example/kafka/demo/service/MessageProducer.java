package de.udocirkel.example.kafka.demo.service;

import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.kafka.core.KafkaOperations;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.ErrorResponseException;

@Service
public class MessageProducer {

    private static final Logger LOG = LoggerFactory.getLogger(MessageProducer.class);

    private final AtomicInteger counterNonTx = new AtomicInteger(0);
    private final AtomicInteger counterTx = new AtomicInteger(0);

    private final KafkaTemplate<String, String> kafkaTemplate;

    private final KafkaTemplate<String, String> txKafkaTemplate;

    @Value("${app.topic.name}")
    private String topic;

    @Value("${app.topic.name-tx}")
    private String topicTx;

    public MessageProducer(
            @Qualifier("kafkaTemplate") KafkaTemplate<String, String> kafkaTemplate,
            @Qualifier("txKafkaTemplate") KafkaTemplate<String, String> txKafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
        this.txKafkaTemplate = txKafkaTemplate;
    }

    public void send(String message) {
        LOG.debug("Message send STARTED [topic={}, tx=false, value={}]", topic, message);
        var id = counterNonTx.incrementAndGet();
        var messageWithId = message + " (" + id + ")";
        sendMessage(kafkaTemplate, topic, messageWithId);
        if (message.contains("fail-fast")) {
            throw new RuntimeException("Simulated producer error");
        }
        LOG.debug("Message send OK [topic={}, tx=false, value={}]", topic, message);
    }

    @Transactional("kafkaTransactionManager")
    public void sendTransactional(String message) throws ErrorResponseException {
        try {
            LOG.debug("Message send STARTED in transaction [topic={}, tx=true, value={}]", topicTx, message);
            var id = counterTx.incrementAndGet();
            var messageWithId1 = message + " (" + id + ".1)";
            var messageWithId2 = message + " (" + id + ".2)";
            var messageWithId3 = message + " (" + id + ".3)";
            txKafkaTemplate
                    .executeInTransaction(ops -> {
                        sendMessage(ops, topicTx, messageWithId1);
                        sendMessage(ops, topicTx, messageWithId2);
                        sendMessage(ops, topicTx, messageWithId3);
                        if (message.contains("fail-fast")) {
                            throw new RuntimeException("Simulated producer error");
                        }
                        LOG.debug("Message transaction COMMITTED [topic={}, tx=true]", topicTx);
                        return null;
                    });
        } catch (RuntimeException e) {
            LOG.error("Message transaction ROLLED BACK [topic={}, tx=true, reason={}]", topicTx, e.getMessage());
            LOG.error("Message transaction ROLLED BACK", e); // FIXME
            throw createErrorResponseException(e, topicTx);
        }
    }

    private void sendMessage(KafkaOperations<String, String> ops, String topic, String message) {
        ops.send(topic, message, message)
                .whenComplete((result, ex) -> {
                    logSendResult(result, ex, topic, message);
                });
    }

    private void logSendResult(SendResult<String, String> result, Throwable ex, String topic, String message) {
        if (ex != null) {
            LOG.error("Message send FAILED [topic={}, value={}]", topic, message, ex);
        } else {
            var pm = result.getProducerRecord();
            var rm = result.getRecordMetadata();
            LOG.debug("Message send OK [topic={}, partition={}, keySize={}, valueSize={}, value={}]",
                    rm.topic(), rm.partition(), rm.serializedKeySize(), rm.serializedValueSize(), pm.value());
        }
    }

    private ErrorResponseException createErrorResponseException(Throwable throwable, String topic) {
        return new ErrorResponseException(
                HttpStatus.INTERNAL_SERVER_ERROR,
                createProblemDetail(throwable.getMessage(), topic),
                throwable
        );
    }

    private static ProblemDetail createProblemDetail(String message, String topic) {
        var problem = ProblemDetail.forStatus(HttpStatus.INTERNAL_SERVER_ERROR);
        problem.setTitle("Error sending Kafka message [TX rolled back]");
        problem.setDetail(message);
        problem.setProperty("topic", topic);
        return problem;
    }

}
