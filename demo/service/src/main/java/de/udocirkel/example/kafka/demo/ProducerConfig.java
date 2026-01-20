package de.udocirkel.example.kafka.demo;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;

@Configuration
public class TxConfig {

    @Bean
    public ProducerFactory<String, String> producerFactory(KafkaProperties properties) {
        return new DefaultKafkaProducerFactory<>(properties.buildProducerProperties());
    }

    @Bean
    public ProducerFactory<String, String> txProducerFactory(KafkaProperties properties) {
        var factory = new DefaultKafkaProducerFactory<String, String>(properties.buildProducerProperties());
        factory.setTransactionIdPrefix("demo-tx-");
        return factory;
    }

    @Bean
    public KafkaTemplate<String, String> kafkaTemplate(ProducerFactory<String, String> producerFactory) {
        return new KafkaTemplate<>(producerFactory);
    }

    @Bean
    public KafkaTemplate<String, String> txKafkaTemplate(@Qualifier("txProducerFactory") ProducerFactory<String, String> factory) {
        return new KafkaTemplate<>(factory);
    }

}
