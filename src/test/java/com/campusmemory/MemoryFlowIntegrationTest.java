package com.campusmemory;

import com.campusmemory.memory.MemoryDtos.ExtractedMemory;
import com.campusmemory.memory.MemoryService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:testdb")
class MemoryFlowIntegrationTest {
    @Autowired MemoryService service;

    @Test void replacesConflictingMemoryAndKeepsAuditTrail() {
        String user = "replacement-test";
        service.remember(user, List.of(new ExtractedMemory("CONSTRAINT", "daily_study_time", "1 hour", .9, null)));
        service.remember(user, List.of(new ExtractedMemory("CONSTRAINT", "daily_study_time", "2 hours", .9, null)));
        assertThat(service.retrieve(user, "plan my study", 5)).singleElement().extracting(m -> m.content()).isEqualTo("2 hours");
        assertThat(service.list(user)).hasSize(2).anyMatch(m -> !m.active() && m.replacedBy() != null);
    }

    @Test void manualForgetRemovesMemoryFromRecall() {
        String user = "forget-test";
        var stored = service.remember(user, List.of(new ExtractedMemory("GOAL", "career_goal", "Backend internship", .95, null))).get(0);
        service.forget(user, stored.id());
        assertThat(service.retrieve(user, "career", 5)).isEmpty();
    }
}
