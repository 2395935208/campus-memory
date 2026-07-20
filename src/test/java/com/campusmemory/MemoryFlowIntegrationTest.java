package com.campusmemory;

import com.campusmemory.memory.MemoryDtos.ExtractedMemory;
import com.campusmemory.memory.Memory;
import com.campusmemory.memory.MemoryRepository;
import com.campusmemory.memory.MemoryService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:testdb")
class MemoryFlowIntegrationTest {
    @Autowired MemoryService service;
    @Autowired MemoryRepository repository;

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

    @Test void canonicalizesQwenAliasWhenReplacingDailyStudyTime() {
        String user = "alias-replacement-test";
        service.remember(user, List.of(new ExtractedMemory("CONSTRAINT", "daily_study_time", "60 minutes", .8, null)));
        service.remember(user, List.of(new ExtractedMemory("CONSTRAINT", "daily_study_duration", "90 minutes", .8, null)));

        var memories = service.list(user);
        assertThat(memories).hasSize(2);
        assertThat(memories).filteredOn(m -> m.active())
                .singleElement()
                .satisfies(m -> {
                    assertThat(m.key()).isEqualTo("daily_study_time");
                    assertThat(m.content()).isEqualTo("90 minutes");
                });
        assertThat(memories).filteredOn(m -> !m.active())
                .singleElement()
                .satisfies(m -> assertThat(m.replacedBy()).isNotNull());
    }

    @Test void canonicalizesValueSpecificSuffixesWhenReplacingDailyStudyTime() {
        String user = "value-suffix-replacement-test";
        service.remember(user, List.of(new ExtractedMemory(
                "CONSTRAINT", "daily_study_time_60_minutes", "Can study 60 minutes each day.", .8, null)));
        service.remember(user, List.of(new ExtractedMemory(
                "CONSTRAINT", "daily_study_time_90_minutes", "Can study 90 minutes each day.", .8, null)));

        var memories = service.list(user);
        assertThat(memories).hasSize(2);
        assertThat(memories).filteredOn(m -> m.active())
                .singleElement()
                .satisfies(m -> {
                    assertThat(m.key()).isEqualTo("daily_study_time");
                    assertThat(m.content()).isEqualTo("Can study 90 minutes each day.");
                });
        assertThat(memories).filteredOn(m -> !m.active())
                .singleElement()
                .satisfies(m -> assertThat(m.replacedBy()).isNotNull());
    }

    @Test void repairsAlreadyStoredActiveAliasesOnRead() {
        String user = "alias-reconciliation-test";
        service.remember(user, List.of(new ExtractedMemory("CONSTRAINT", "daily_study_time", "60 minutes", .8, null)));

        Memory alias = new Memory();
        alias.setUserId(user);
        alias.setType("CONSTRAINT");
        alias.setMemoryKey("daily_study_duration");
        alias.setContent("90 minutes");
        alias.setImportance(.8);
        alias = repository.saveAndFlush(alias);

        var memories = service.list(user);
        Long replacementId = alias.getId();
        assertThat(memories).filteredOn(m -> m.active())
                .singleElement()
                .satisfies(m -> {
                    assertThat(m.id()).isEqualTo(replacementId);
                    assertThat(m.key()).isEqualTo("daily_study_time");
                    assertThat(m.content()).isEqualTo("90 minutes");
                });
        assertThat(memories).filteredOn(m -> !m.active())
                .singleElement()
                .satisfies(m -> assertThat(m.replacedBy()).isEqualTo(replacementId));
    }

    @Test void repairsAlreadyStoredValueSpecificDailyStudyKeysOnRead() {
        String user = "value-suffix-reconciliation-test";

        Memory oldMemory = new Memory();
        oldMemory.setUserId(user);
        oldMemory.setType("CONSTRAINT");
        oldMemory.setMemoryKey("daily_study_time_60_minutes");
        oldMemory.setContent("Can study 60 minutes each day.");
        oldMemory.setImportance(.8);
        repository.saveAndFlush(oldMemory);

        Memory newMemory = new Memory();
        newMemory.setUserId(user);
        newMemory.setType("CONSTRAINT");
        newMemory.setMemoryKey("daily_study_time_90_minutes");
        newMemory.setContent("Can study 90 minutes each day.");
        newMemory.setImportance(.8);
        newMemory = repository.saveAndFlush(newMemory);

        var memories = service.list(user);
        Long replacementId = newMemory.getId();
        assertThat(memories).filteredOn(m -> m.active())
                .singleElement()
                .satisfies(m -> {
                    assertThat(m.id()).isEqualTo(replacementId);
                    assertThat(m.key()).isEqualTo("daily_study_time");
                    assertThat(m.content()).isEqualTo("Can study 90 minutes each day.");
                });
        assertThat(memories).filteredOn(m -> !m.active())
                .singleElement()
                .satisfies(m -> assertThat(m.replacedBy()).isEqualTo(replacementId));
    }
}
