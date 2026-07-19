package com.campusmemory.memory;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface MemoryRepository extends JpaRepository<Memory, Long> {
    List<Memory> findByUserIdOrderByUpdatedAtDesc(String userId);
    List<Memory> findByUserIdAndActiveTrueOrderByUpdatedAtDesc(String userId);
    Optional<Memory> findFirstByUserIdAndMemoryKeyAndActiveTrue(String userId, String memoryKey);
}
