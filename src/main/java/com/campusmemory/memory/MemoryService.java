package com.campusmemory.memory;

import com.campusmemory.memory.MemoryDtos.ExtractedMemory;
import com.campusmemory.memory.MemoryDtos.MemoryView;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class MemoryService {
    private static final Set<String> DAILY_STUDY_TIME_KEYS = Set.of(
            "daily_study_time",
            "daily_study_duration",
            "daily_learning_time",
            "daily_study_hours",
            "daily_study_minutes"
    );

    private final MemoryRepository repository;
    public MemoryService(MemoryRepository repository) { this.repository = repository; }

    @Transactional
    public List<MemoryView> remember(String userId, List<ExtractedMemory> candidates) {
        expireOld(userId);
        reconcileActiveAliases(userId);
        List<MemoryView> saved = new ArrayList<>();
        for (ExtractedMemory item : candidates) {
            if (item == null || blank(item.content()) || blank(item.key())) continue;
            String key = canonicalKey(item.key());
            var existing = repository.findFirstByUserIdAndMemoryKeyAndActiveTrue(userId, key);
            if (existing.isPresent() && existing.get().getContent().equalsIgnoreCase(item.content().trim())) continue;
            Memory memory = new Memory();
            memory.setUserId(userId);
            memory.setType(blank(item.type()) ? "FACT" : item.type().trim().toUpperCase(Locale.ROOT));
            memory.setMemoryKey(key);
            memory.setContent(item.content().trim());
            memory.setImportance(Math.max(0.1, Math.min(1.0, item.importance())));
            if (item.ttlDays() != null && item.ttlDays() > 0) memory.setExpiresAt(Instant.now().plus(item.ttlDays(), ChronoUnit.DAYS));
            memory = repository.save(memory);
            if (existing.isPresent()) {
                Memory old = existing.get();
                old.setActive(false);
                old.setReplacedBy(memory.getId());
                repository.save(old);
            }
            saved.add(MemoryView.of(memory, 1.0));
        }
        return saved;
    }

    @Transactional
    public List<MemoryView> retrieve(String userId, String query, int limit) {
        expireOld(userId);
        reconcileActiveAliases(userId);
        return repository.findByUserIdAndActiveTrueOrderByUpdatedAtDesc(userId).stream()
                .map(m -> Map.entry(m, score(m, query)))
                .sorted(Map.Entry.<Memory, Double>comparingByValue().reversed())
                .limit(limit)
                .map(e -> MemoryView.of(e.getKey(), round(e.getValue())))
                .toList();
    }

    @Transactional
    public List<MemoryView> list(String userId) {
        expireOld(userId);
        reconcileActiveAliases(userId);
        return repository.findByUserIdOrderByUpdatedAtDesc(userId).stream().map(m -> MemoryView.of(m, 0)).toList();
    }

    @Transactional
    public void forget(String userId, Long id) {
        Memory memory = repository.findById(id).orElseThrow();
        if (!memory.getUserId().equals(userId)) throw new NoSuchElementException();
        memory.setActive(false);
        repository.save(memory);
    }

    private void expireOld(String userId) {
        Instant now = Instant.now();
        repository.findByUserIdAndActiveTrueOrderByUpdatedAtDesc(userId).stream()
                .filter(m -> m.getExpiresAt() != null && !m.getExpiresAt().isAfter(now))
                .forEach(m -> { m.setActive(false); repository.save(m); });
    }

    private void reconcileActiveAliases(String userId) {
        Map<String, Memory> newestByKey = new HashMap<>();
        List<Memory> active = repository.findByUserIdAndActiveTrueOrderByUpdatedAtDesc(userId).stream()
                .sorted(Comparator.comparing(Memory::getUpdatedAt).thenComparing(Memory::getId).reversed())
                .toList();
        for (Memory memory : active) {
            String key = canonicalKey(memory.getMemoryKey());
            Memory newest = newestByKey.putIfAbsent(key, memory);
            if (newest == null) {
                if (!memory.getMemoryKey().equals(key)) {
                    memory.setMemoryKey(key);
                    repository.save(memory);
                }
                continue;
            }
            memory.setActive(false);
            memory.setReplacedBy(newest.getId());
            repository.save(memory);
        }
    }

    private double score(Memory m, String query) {
        double keyword = tokenOverlap((m.getMemoryKey() + " " + m.getContent()).toLowerCase(), query.toLowerCase());
        long ageHours = Math.max(0, Duration.between(m.getUpdatedAt(), Instant.now()).toHours());
        double recency = 1.0 / (1.0 + ageHours / 168.0);
        return 0.55 * m.getImportance() + 0.30 * keyword + 0.15 * recency;
    }

    private double tokenOverlap(String memory, String query) {
        Set<String> tokens = Arrays.stream(query.split("[^\\p{L}\\p{N}]+"))
                .filter(t -> t.length() > 1).collect(Collectors.toSet());
        if (tokens.isEmpty()) return 0;
        long matches = tokens.stream().filter(memory::contains).count();
        return (double) matches / tokens.size();
    }
    private static String canonicalKey(String value) {
        String key = normalize(value);
        boolean dailyStudyTime = DAILY_STUDY_TIME_KEYS.stream()
                .anyMatch(alias -> key.equals(alias) || key.startsWith(alias + "_"));
        return dailyStudyTime ? "daily_study_time" : key;
    }
    private static String normalize(String value) { return value.trim().toLowerCase(Locale.ROOT).replaceAll("[^\\p{L}\\p{N}_-]+", "_"); }
    private static boolean blank(String value) { return value == null || value.isBlank(); }
    private static double round(double v) { return Math.round(v * 1000.0) / 1000.0; }
}
