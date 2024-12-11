package com.pubsub.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.pubsub.config.KafkaConfig;
import com.pubsub.model.Content;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;

@Service
public class ContentSubscriber implements AutoCloseable {
    private static final Logger logger = LoggerFactory.getLogger(ContentSubscriber.class);
    private final String username;
    private final SubscriptionService subscriptionService;
    private final Map<String, List<Content>> notifications;
    private final KafkaConsumer<String, String> consumer;
    private final ObjectMapper objectMapper;
    private final ExecutorService consumerThread;
    private boolean isActive;
    private final EmailService emailService;
    private final UserService userService;
    private final KafkaConfig kafkaConfig;

    @Autowired
    public ContentSubscriber(@Value("${app.subscriber.username:default-user}") String username, 
                           SubscriptionService subscriptionService,
                           EmailService emailService,
                           UserService userService,
                           KafkaConfig kafkaConfig) {
        this.username = username;
        this.subscriptionService = subscriptionService;
        this.emailService = emailService;
        this.userService = userService;
        this.kafkaConfig = kafkaConfig;
        this.notifications = new ConcurrentHashMap<>();
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
        
        String groupId = "subscriber-" + username + "-" + UUID.randomUUID();
        this.consumer = new KafkaConsumer<>(kafkaConfig.getConsumerProps(groupId));
        this.consumerThread = Executors.newSingleThreadExecutor();
        this.isActive = true;

        startConsuming();
        logger.info("ContentSubscriber initialized for user: {}", username);
    }

    private void startConsuming() {
        // Subscribe to content and notification topics
        consumer.subscribe(Arrays.asList(kafkaConfig.getContentTopic(), kafkaConfig.getNotificationTopic()));
        logger.info("Subscribed to topics: {}, {}", kafkaConfig.getContentTopic(), kafkaConfig.getNotificationTopic());
        
        consumerThread.submit(() -> {
            try {
                while (isActive) {
                    ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(100));
                    for (ConsumerRecord<String, String> record : records) {
                        try {
                            Content content = objectMapper.readValue(record.value(), Content.class);
                            processContent(content);
                            displayNotification(content);
                            logger.debug("Processed content: {}", content.getTitle());
                        } catch (Exception e) {
                            logger.error("Error processing message: {}", e.getMessage(), e);
                        }
                    }
                }
            } catch (Exception e) {
                logger.error("Error in consumer thread: {}", e.getMessage(), e);
            } finally {
                try {
                    consumer.close();
                    logger.info("Kafka consumer closed successfully");
                } catch (Exception e) {
                    logger.error("Error closing consumer: {}", e.getMessage(), e);
                }
            }
        });
    }

    private void processContent(Content content) {
        if (!isActive || content == null) {
            return;
        }

        try {
            Set<String> activeKeywords = subscriptionService.getSubscriberKeywords(username);
            Set<String> matchedKeywords = new HashSet<>();

            for (String subscribedKeyword : activeKeywords) {
                String[] keywordParts = subscribedKeyword.split("\\s+");
                boolean matches = false;

                // Vérifier les mots-clés du contenu
                for (String contentKeyword : content.getKeywords()) {
                    if (matchesKeyword(contentKeyword.toLowerCase(), keywordParts)) {
                        matches = true;
                        matchedKeywords.add(subscribedKeyword);
                        break;
                    }
                }

                // Vérifier le titre
                if (!matches && matchesKeyword(content.getTitle().toLowerCase(), keywordParts)) {
                    matches = true;
                    matchedKeywords.add(subscribedKeyword);
                }

                // Vérifier le corps
                if (!matches && matchesKeyword(content.getBody().toLowerCase(), keywordParts)) {
                    matches = true;
                    matchedKeywords.add(subscribedKeyword);
                }

                if (matches) {
                    notifications.computeIfAbsent(subscribedKeyword, k -> new CopyOnWriteArrayList<>()).add(content);
                    logger.info("Added notification for keyword '{}': {}", subscribedKeyword, content.getTitle());
                }
            }

            // Envoyer une notification par email s'il y a des correspondances
            if (!matchedKeywords.isEmpty()) {
                userService.findByUsername(username).ifPresent(user -> {
                    emailService.sendContentNotification(user.getEmail(), content, matchedKeywords);
                });
            }
        } catch (Exception e) {
            logger.error("Error processing content: {}", e.getMessage(), e);
        }
    }

    private void displayNotification(Content content) {
        System.out.println("\n=== New Content Notification for " + username + " ===");
        System.out.println("Title: " + content.getTitle());
        System.out.println("Publisher: " + content.getPublisherId());
        System.out.println("Content: " + content.getBody());
        System.out.println("Keywords: " + String.join(", ", content.getKeywords()));
        System.out.println("Published at: " + content.getCreatedAt());
        System.out.println("=======================================");
        System.out.println("Press Enter to continue...");
    }

    private boolean matchesKeyword(String text, String[] keywordParts) {
        for (String part : keywordParts) {
            if (!text.contains(part.toLowerCase())) {
                return false;
            }
        }
        return true;
    }

    public boolean addSubscription(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return false;
        }

        String[] words = keyword.trim().toLowerCase().split("\\s+");
        if (words.length > 3) {
            return false;
        }

        try {
            subscriptionService.createSubscription(username, keyword.trim().toLowerCase());
            notifications.putIfAbsent(keyword.trim().toLowerCase(), new CopyOnWriteArrayList<>());
            logger.info("User {} subscribed to keyword: {}", username, keyword);
            return true;
        } catch (Exception e) {
            logger.error("Error adding subscription: {}", e.getMessage(), e);
            return false;
        }
    }

    public boolean removeSubscription(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return false;
        }

        boolean removed = subscriptionService.removeSubscription(username, keyword.trim().toLowerCase());
        if (removed) {
            notifications.remove(keyword.trim().toLowerCase());
            logger.info("User {} unsubscribed from keyword: {}", username, keyword);
        }
        return removed;
    }

    public Set<String> getActiveKeywords() {
        return subscriptionService.getSubscriberKeywords(username);
    }

    public Map<String, List<Content>> getNotifications() {
        return new HashMap<>(notifications);
    }

    @Override
    public void close() {
        isActive = false;
        consumer.wakeup();
        consumerThread.shutdown();
        try {
            if (!consumerThread.awaitTermination(5, TimeUnit.SECONDS)) {
                consumerThread.shutdownNow();
            }
        } catch (InterruptedException e) {
            consumerThread.shutdownNow();
        }
        logger.info("ContentSubscriber closed for user: {}", username);
    }
}
