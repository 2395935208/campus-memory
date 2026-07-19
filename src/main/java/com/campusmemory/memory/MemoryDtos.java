package com.campusmemory.memory;

import java.time.Instant;

public final class MemoryDtos {
    private MemoryDtos() {}
    public record ExtractedMemory(String type, String key, String content, double importance, Integer ttlDays) {}
    public record MemoryView(Long id, String type, String key, String content, double importance,
                             boolean active, Instant updatedAt, Instant expiresAt, Long replacedBy, double relevance) {
        public static MemoryView of(Memory memory, double relevance) {
            return new MemoryView(memory.getId(), memory.getType(), memory.getMemoryKey(), memory.getContent(),
                    memory.getImportance(), memory.isActive(), memory.getUpdatedAt(), memory.getExpiresAt(),
                    memory.getReplacedBy(), relevance);
        }
    }
}
