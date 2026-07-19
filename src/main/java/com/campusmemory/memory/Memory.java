package com.campusmemory.memory;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "memories", indexes = @Index(name = "idx_memory_user_active", columnList = "userId,active"))
public class Memory {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false) private String userId;
    @Column(nullable = false) private String type;
    @Column(nullable = false) private String memoryKey;
    @Column(nullable = false, length = 1200) private String content;
    private double importance;
    @Column(nullable = false) private boolean active = true;
    @Column(nullable = false) private Instant createdAt;
    @Column(nullable = false) private Instant updatedAt;
    private Instant expiresAt;
    private Long replacedBy;

    @PrePersist void onCreate() { var now = Instant.now(); createdAt = now; updatedAt = now; }
    @PreUpdate void onUpdate() { updatedAt = Instant.now(); }

    public Long getId() { return id; }
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public String getMemoryKey() { return memoryKey; }
    public void setMemoryKey(String memoryKey) { this.memoryKey = memoryKey; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public double getImportance() { return importance; }
    public void setImportance(double importance) { this.importance = importance; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public Instant getExpiresAt() { return expiresAt; }
    public void setExpiresAt(Instant expiresAt) { this.expiresAt = expiresAt; }
    public Long getReplacedBy() { return replacedBy; }
    public void setReplacedBy(Long replacedBy) { this.replacedBy = replacedBy; }
}
