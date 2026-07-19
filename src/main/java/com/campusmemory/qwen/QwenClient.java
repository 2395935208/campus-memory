package com.campusmemory.qwen;

import com.campusmemory.memory.MemoryDtos.ExtractedMemory;
import com.campusmemory.memory.MemoryDtos.MemoryView;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import java.time.LocalDate;
import java.time.ZoneOffset;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class QwenClient {
    private final RestClient restClient;
    private final ObjectMapper mapper;
    private final String apiKey;
    private final String model;

    public QwenClient(ObjectMapper mapper,
                      @Value("${qwen.base-url}") String baseUrl,
                      @Value("${qwen.api-key:}") String apiKey,
                      @Value("${qwen.model:qwen3.7-plus}") String model) {
        this.mapper = mapper;
        this.apiKey = apiKey;
        this.model = model;
        this.restClient = RestClient.builder().baseUrl(baseUrl).build();
    }

    public boolean live() { return apiKey != null && !apiKey.isBlank(); }

    public List<ExtractedMemory> extract(String message) {
        if (!live()) return heuristicExtract(message);
        String system = """
        You are the memory gate for Campus AI Agent.
        Current UTC date: %s.

        Return a JSON object with an array field named memories.
        Each memory must contain:
        type: GOAL, PREFERENCE, CONSTRAINT, SKILL, or MISTAKE
        key: a stable semantic key
        content: one concise fact explicitly stated by the user
        importance: a number from 0 to 1
        ttlDays: null or a positive integer

        Store only durable information that will probably matter in future sessions:
        long-term goals, stable preferences, recurring constraints,
        current skills, and recurring mistakes.

        Never store:
        questions, greetings, commands, requests to create a plan,
        one-time tasks, assistant-generated suggestions, or inferred facts.

        Never invent a date. If no durable memory exists,
        return exactly {"memories":[]}.

        Use the same stable key when a newer fact should replace an older fact.
        Do not store passwords, API keys, private identifiers, or other secrets.
        """.formatted(LocalDate.now(ZoneOffset.UTC));
        try {
            JsonNode result = call(system, message, true);
            return mapper.convertValue(mapper.readTree(result.path("content").asText()).path("memories"), new TypeReference<>() {});
        } catch (Exception ex) {
            return heuristicExtract(message);
        }
    }

    public String answer(String message, List<MemoryView> memories) {
        String context = memories.isEmpty() ? "No relevant durable memory." : memories.stream()
                .map(m -> "- [" + m.type() + "/" + m.key() + "] " + m.content())
                .reduce((a, b) -> a + "\n" + b).orElse("");
        if (!live()) return demoAnswer(message, memories);
        String system = "You are Campus AI Agent, a concise and supportive learning coach. Use only relevant memory, " +
                "never claim memory that is absent, and mention when the plan adapts to remembered constraints.\nMEMORY:\n" + context;
        try { return call(system, message, false).path("content").asText(); }
        catch (Exception ex) { return "Qwen Cloud is temporarily unavailable. Your memories are safe; please retry. (" + ex.getClass().getSimpleName() + ")"; }
    }

    private JsonNode call(String system, String user, boolean json) throws Exception {
        Map<String,Object> body = new LinkedHashMap<>();
        body.put("model", model);
        body.put("messages", List.of(Map.of("role","system","content",system), Map.of("role","user","content",user)));
        body.put("temperature", json ? 0.1 : 0.4);
        if (json) body.put("response_format", Map.of("type", "json_object"));
        JsonNode response = restClient.post().uri("/chat/completions")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                .contentType(MediaType.APPLICATION_JSON).body(body).retrieve().body(JsonNode.class);
        if (response == null) throw new IllegalStateException("Empty Qwen response");
        return response.path("choices").path(0).path("message");
    }

    private List<ExtractedMemory> heuristicExtract(String text) {
        List<ExtractedMemory> out = new ArrayList<>();
        String lower = text.toLowerCase(Locale.ROOT);
        if (lower.contains("java")) out.add(new ExtractedMemory("SKILL", "learning_stack", "The learner is studying Java backend development.", .82, null));
        if (lower.contains("实习") || lower.contains("intern")) out.add(new ExtractedMemory("GOAL", "career_goal", "The learner is preparing for a backend internship.", .95, null));
        Matcher hours = Pattern.compile("(\\d+(?:\\.\\d+)?)\\s*(?:个?小时|hours?)", Pattern.CASE_INSENSITIVE).matcher(text);
        if (hours.find()) out.add(new ExtractedMemory("CONSTRAINT", "daily_study_time", "The learner can study " + hours.group(1) + " hour(s) per day.", .9, null));
        if (lower.contains("mybatis")) out.add(new ExtractedMemory("SKILL", "current_topic", "The learner is currently learning MyBatis.", .75, 30));
        return out;
    }

    private String demoAnswer(String message, List<MemoryView> memories) {
        if (memories.isEmpty()) return "Demo mode: tell me your goal, current stack, and daily study time. I will remember durable facts across new sessions.";
        String facts = memories.stream().map(MemoryView::content).reduce((a,b) -> a + " " + b).orElse("");
        return "Demo mode personalized plan: 1) review one core concept, 2) implement one small endpoint, 3) write a five-line retrospective. I adapted this plan using: " + facts;
    }
}
