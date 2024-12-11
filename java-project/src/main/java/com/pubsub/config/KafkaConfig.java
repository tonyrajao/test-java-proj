package com.pubsub.config;

import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.ListTopicsResult;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

import javax.annotation.PostConstruct;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

@Configuration
public class KafkaConfig {
    private static final Logger logger = LoggerFactory.getLogger(KafkaConfig.class);
    
    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    @Value("${spring.kafka.producer.retries:3}")
    private int producerRetries;

    @Value("${spring.kafka.consumer.auto-offset-reset:earliest}")
    private String autoOffsetReset;

    private final String contentTopic = "content-topic";
    private final String notificationTopic = "notification-topic";
    private final String emailTopic = "email-topic";

    public String getContentTopic() {
        return contentTopic;
    }

    public String getNotificationTopic() {
        return notificationTopic;
    }

    public String getEmailTopic() {
        return emailTopic;
    }

    @PostConstruct
    public void init() {
        logger.info("********************************************************");
        logger.info("Initializing Kafka Configuration");
        logger.info("Bootstrap Servers: {}", bootstrapServers);
        logger.info("Content Topic: {}", contentTopic);
        logger.info("Notification Topic: {}", notificationTopic);
        logger.info("Email Topic: {}", emailTopic);
        logger.info("********************************************************");
        
        verifyKafkaConnection();
    }

    @Bean
    public NewTopic contentTopic() {
        return TopicBuilder.name(contentTopic)
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic notificationTopic() {
        return TopicBuilder.name(notificationTopic)
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic emailTopic() {
        return TopicBuilder.name(emailTopic)
                .partitions(3)
                .replicas(1)
                .build();
    }

    private void verifyKafkaConnection() {
        Properties props = new Properties();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ProducerConfig.REQUEST_TIMEOUT_MS_CONFIG, "20000"); // Increased timeout for Docker environment
        props.put(ProducerConfig.RETRY_BACKOFF_MS_CONFIG, "1000");

        try (AdminClient adminClient = AdminClient.create(props)) {
            ListTopicsResult topics = adminClient.listTopics();
            topics.names().get(20, TimeUnit.SECONDS); // Increased timeout
            logger.info("Successfully connected to Kafka cluster");
        } catch (Exception e) {
            logger.error("Failed to connect to Kafka cluster: {}", e.getMessage());
            logger.error("Make sure to start Docker containers with 'docker-compose up -d'", e);
            throw new RuntimeException("Failed to connect to Kafka cluster", e);
        }
    }

    public Properties getProducerProps() {
        logger.info("Creating new Kafka producer connection to cluster: {}", bootstrapServers);
        Properties props = new Properties();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        props.put(ProducerConfig.ACKS_CONFIG, "all");
        props.put(ProducerConfig.RETRIES_CONFIG, producerRetries);
        props.put(ProducerConfig.MAX_BLOCK_MS_CONFIG, "20000");
        props.put(ProducerConfig.DELIVERY_TIMEOUT_MS_CONFIG, "30000");
        props.put(ProducerConfig.REQUEST_TIMEOUT_MS_CONFIG, "20000");
        props.put(ProducerConfig.RETRY_BACKOFF_MS_CONFIG, "1000");
        props.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, "true");
        logger.info("Producer configuration complete with acks=all and retries={}", producerRetries);
        return props;
    }

    public Properties getConsumerProps(String groupId) {
        logger.info("Creating new Kafka consumer connection. Group: {}, Servers: {}", groupId, bootstrapServers);
        Properties props = new Properties();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, autoOffsetReset);
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "false");
        props.put(ConsumerConfig.SESSION_TIMEOUT_MS_CONFIG, "30000");
        props.put(ConsumerConfig.HEARTBEAT_INTERVAL_MS_CONFIG, "10000");
        props.put(ConsumerConfig.MAX_POLL_INTERVAL_MS_CONFIG, "300000");
        props.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, "500");
        props.put(ConsumerConfig.FETCH_MAX_WAIT_MS_CONFIG, "500");
        logger.info("Consumer configuration complete for group: {}", groupId);
        return props;
    }
}
