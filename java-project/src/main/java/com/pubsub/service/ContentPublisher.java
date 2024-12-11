package com.pubsub.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.pubsub.config.KafkaConfig;
import com.pubsub.model.Content;
import com.pubsub.repository.ContentRepository;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
public class ContentPublisher implements AutoCloseable {
    private static final Logger logger = LoggerFactory.getLogger(ContentPublisher.class);
    private final KafkaProducer<String, String> producer;
    private final ObjectMapper objectMapper;
    private final ContentRepository contentRepository;
    private final KafkaConfig kafkaConfig;
    private boolean isActive;

    @Autowired
    public ContentPublisher(ContentRepository contentRepository, KafkaConfig kafkaConfig) {
        this.kafkaConfig = kafkaConfig;
        this.producer = new KafkaProducer<>(kafkaConfig.getProducerProps());
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
        this.contentRepository = contentRepository;
        this.isActive = true;
        
        logger.info("ContentPublisher initialized with Kafka producer");
    }

    public void publish(Content content) {
        if (!isActive || content == null) {
            return;
        }

        try {
            // Sauvegarder dans la base de données
            Content savedContent = contentRepository.save(content);
            logger.info("Contenue sauvegardée avec ID: {}", savedContent.getId());

            // Publier vers Kafka
            String contentJson = objectMapper.writeValueAsString(savedContent);
            producer.send(new ProducerRecord<>(kafkaConfig.getContentTopic(), savedContent.getId(), contentJson));
            logger.info("Contenue publiée vers Kafka: {}", savedContent.getTitle());
        } catch (Exception e) {
            logger.error("Error publishing content: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to publish content", e);
        }
    }

    public List<Content> getPublisherContent(String publisherId) {
        return contentRepository.findByPublisherId(publisherId);
    }

    public List<Content> getAllContent() {
        return contentRepository.findAll();
    }

    @Transactional
    public void deleteContent(String contentId) {
        contentRepository.deleteById(contentId);
    }

    @Transactional
    public boolean deleteContent(String contentId, String publisherId) {
        Optional<Content> contentOpt = contentRepository.findById(contentId);
        if (contentOpt.isPresent() && contentOpt.get().getPublisherId().equals(publisherId)) {
            contentRepository.deleteById(contentId);
            logger.info("contenue {} supprimée par le publisher {}", contentId, publisherId);
            return true;
        }
        logger.warn("contenue {} non supprimée par le publisher {}", contentId, publisherId);
        return false;
    }

    @Transactional
    public void deleteAllPublisherContent(String publisherId) {
        contentRepository.deleteByPublisherId(publisherId);
    }

    @Override
    public void close() {
        isActive = false;
        if (producer != null) {
            producer.close();
        }
    }
}
