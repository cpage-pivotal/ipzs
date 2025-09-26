package org.tanzu.ipzs.legislation.model.entity;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "chat_sessions")
public class ChatSession {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_identifier")
    private String userIdentifier;

    @Column(name = "date_context", nullable = false)
    private LocalDate dateContext;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "last_interaction")
    private LocalDateTime lastInteraction;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        lastInteraction = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        lastInteraction = LocalDateTime.now();
    }

    // Getters and Setters
    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getUserIdentifier() {
        return userIdentifier;
    }

    public void setUserIdentifier(String userIdentifier) {
        this.userIdentifier = userIdentifier;
    }

    public LocalDate getDateContext() {
        return dateContext;
    }

    public void setDateContext(LocalDate dateContext) {
        this.dateContext = dateContext;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getLastInteraction() {
        return lastInteraction;
    }

    public void setLastInteraction(LocalDateTime lastInteraction) {
        this.lastInteraction = lastInteraction;
    }
}