package com.pubsub.model;

import javax.persistence.*;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "contents", indexes = {
    @Index(name = "idx_content_publisher", columnList = "publisherId"),
    @Index(name = "idx_content_created", columnList = "createdAt")
})
@Cacheable
public class Content {
    @Id
    private String id = UUID.randomUUID().toString();
    
    @Column(nullable = false, length = 255)
    private String title;
    
    @Column(columnDefinition = "TEXT", nullable = false)
    @Lob
    private String body;
    
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
        name = "content_keywords", 
        joinColumns = @JoinColumn(name = "content_id"),
        indexes = @Index(name = "idx_content_keyword", columnList = "keyword")
    )
    @Column(name = "keyword", nullable = false, length = 100)
    private Set<String> keywords;
    
    @Column(nullable = false)
    private String publisherId;
    
    @Column(nullable = false)
    private Instant createdAt;

    public Content(String title, String body, Set<String> keywords, String publisherId) {
        this.title = title;
        this.body = body;
        this.keywords = new HashSet<>(keywords);
        this.publisherId = publisherId;
        this.createdAt = Instant.now();
    }
    
    //constructeur pour deserialisation json
    public Content() {
        this.id = UUID.randomUUID().toString();
        this.keywords = new HashSet<>();
        this.createdAt = Instant.now();
    }   

    //getters
    public String getId() {
        return id;
    }
    public String getTitle() {
        return title;
    }
    public String getBody() {
        return body;
    }
    public Set<String> getKeywords() {
        return keywords;
    }
    public String getPublisherId() {
        return publisherId;
    }
    public Instant getCreatedAt() {
        return createdAt;
    }

    //setters
    public void setId(String id) {
        this.id = id;
    }
    public void setTitle(String title) {
        this.title = title;
    }
    public void setBody(String body) {
        this.body = body;
    }
    public void setKeywords(Set<String> keywords) {
        this.keywords = keywords;
    }
    public void setPublisherId(String publisherId) {
        this.publisherId = publisherId;
    }
    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    // méthodes d'ajout et de suppression de mot-clé
    public void addKeyword(String keyword) {
        this.keywords.add(keyword);
    }

    public void removeKeyword(String keyword) {
        this.keywords.remove(keyword);
    }

    @Override
    public String toString() {
        return "Content{" +
                "id='" + id + '\'' +
                ", title='" + title + '\'' +
                ", body='" + body + '\'' +
                ", keywords=" + keywords +
                ", publisherId='" + publisherId + '\'' +
                ", createdAt=" + createdAt +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Content content = (Content) o;
        return id.equals(content.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }
}
