package com.pubsub.model;

import javax.persistence.*;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "subscriptions", indexes = {
    @Index(name = "idx_subscription_subscriber", columnList = "subscriberId"),
    @Index(name = "idx_subscription_active", columnList = "active"),
    @Index(name = "idx_subscription_updated", columnList = "updatedAt")
})
@Cacheable
public class Subscription {
    @Id
    private String id;
    
    @Column(nullable = false)
    private String subscriberId;
    
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
        name = "subscription_keywords", 
        joinColumns = @JoinColumn(name = "subscription_id"),
        indexes = @Index(name = "idx_subscription_keyword", columnList = "keyword")
    )
    @Column(name = "keyword", length = 100)
    private Set<String> keywords;
    
    @Column(nullable = false)
    private boolean active;
    
    @Column(nullable = false)
    private Instant createdAt;
    
    @Column(nullable = false)
    private Instant updatedAt;

    public Subscription(String subscriberId, Set<String> keywords) {
        this.id = UUID.randomUUID().toString();
        this.subscriberId = subscriberId;
        this.keywords = new HashSet<>(keywords);
        this.active = true;
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    public Subscription() {
        this.id = UUID.randomUUID().toString();
        this.keywords = new HashSet<>();
        this.active = true;
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
        this.updatedAt = Instant.now();
    }

    public String getSubscriberId() {
        return subscriberId;
    }

    public void setSubscriberId(String subscriberId) {
        this.subscriberId = subscriberId;
        this.updatedAt = Instant.now();
    }

    public Set<String> getKeywords() {
        return new HashSet<>(keywords);
    }

    public void setKeywords(Set<String> keywords) {
        this.keywords = new HashSet<>(keywords);
        this.updatedAt = Instant.now();
    }

    public void addKeyword(String keyword) {
        this.keywords.add(keyword);
        this.updatedAt = Instant.now();
    }

    public void removeKeyword(String keyword) {
        this.keywords.remove(keyword);
        this.updatedAt = Instant.now();
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
        this.updatedAt = Instant.now();
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    @Override
    public String toString() {
        return String.format("Subscription{id='%s', subscriberId='%s', keywords=%s, active=%s}", 
            id, subscriberId, keywords, active);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Subscription that = (Subscription) o;
        return id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }
}