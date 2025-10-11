package com.demo.producer;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class KafkaTopicConfig {
    @Bean
    public NewTopic createMyTopic() {
        return new NewTopic("my-new-topic-2",
            3,              // number of partitions (the more, the more available parallelism)
            (short) 1       // replication factor (the more, the more fault-tolerant)
        ); //
    }
}
