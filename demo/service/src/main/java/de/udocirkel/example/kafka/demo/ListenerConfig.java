package de.udocirkel.example.kafka.demo;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;

@Configuration
public class ListenerConfig {

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, String> txKafkaListenerContainerFactory(
            ConsumerFactory<String, String> consumerFactory, @Qualifier("txKafkaTemplate") KafkaTemplate<String, String> txKafkaTemplate) {

        var errorHandler = new DefaultErrorHandler(
                new DeadLetterPublishingRecoverer(txKafkaTemplate),
                new FixedBackOff(1000L, 3)); // 1s interval, 3 retries

        var factory = new ConcurrentKafkaListenerContainerFactory<String, String>();
        factory.setConsumerFactory(consumerFactory);
        factory.setBatchListener(false);
        factory.setCommonErrorHandler(errorHandler);
        return factory;
    }

}
