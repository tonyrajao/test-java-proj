package com.pubsub.model;

import javax.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "users", indexes = {
    @Index(name = "idx_user_username", columnList = "username", unique = true),
    @Index(name = "idx_user_email", columnList = "email", unique = true),
    @Index(name = "idx_user_type", columnList = "type")
})
@Cacheable
public class User {
    @Id
    private String id;

    @Column(unique = true, nullable = false, length = 50)
    private String username;

    @Column(unique = true, nullable = false, length = 100)
    private String email;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private UserType type;

    @Column(nullable = false, length = 60)
    private String password;

    @Column(nullable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant lastUpdatedAt;

    public enum UserType {
        PUBLISHER("Publisher"),
        SUBSCRIBER("Subscriber");

        private final String label;

        UserType(String label) {
            this.label = label;
        }

        public String getLabel() {
            return label;
        }
    }

    // Constructeur par défaut requis par JPA
    protected User() {}

    public User(String username, String email, String password, UserType type) {
        this.id = UUID.randomUUID().toString();
        this.username = username;
        this.email = email;
        this.password = password;
        this.type = type;
        this.createdAt = Instant.now();
        this.lastUpdatedAt = Instant.now();
    }

    // Getters
    public String getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public String getEmail() {
        return email;
    }

    public UserType getType() {
        return type;
    }

    public String getPassword() {
        return password;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getLastUpdatedAt() {
        return lastUpdatedAt;
    }

    // Setters
    public void setUsername(String username) {
        this.username = username;
        this.lastUpdatedAt = Instant.now();
    }

    public void setEmail(String email) {
        this.email = email;
        this.lastUpdatedAt = Instant.now();
    }

    public void setType(UserType type) {
        this.type = type;
        this.lastUpdatedAt = Instant.now();
    }

    public void setPassword(String password) {
        this.password = password;
        this.lastUpdatedAt = Instant.now();
    }

    @PreUpdate
    protected void onUpdate() {
        lastUpdatedAt = Instant.now();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        User user = (User) o;
        return id.equals(user.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public String toString() {
        return "User{" +
                "id='" + id + '\'' +
                ", username='" + username + '\'' +
                ", email='" + email + '\'' +
                ", type=" + type +
                ", password='" + password + '\'' +
                ", createdAt=" + createdAt +
                ", lastUpdatedAt=" + lastUpdatedAt +
                '}';
    }
}
