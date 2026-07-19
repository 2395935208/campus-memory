package com.campusmemory.chat;

import com.campusmemory.memory.MemoryDtos.MemoryView;
import com.campusmemory.memory.MemoryService;
import com.campusmemory.qwen.QwenClient;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
public class ChatController {
    private final MemoryService memories;
    private final QwenClient qwen;
    public ChatController(MemoryService memories, QwenClient qwen) { this.memories = memories; this.qwen = qwen; }

    public record ChatRequest(@NotBlank String userId, @NotBlank String sessionId, @NotBlank String message) {}
    public record ChatResponse(String answer, List<MemoryView> usedMemories, List<MemoryView> learnedMemories, String mode) {}

    @PostMapping("/chat")
    public ChatResponse chat(@Valid @RequestBody ChatRequest request) {
        List<MemoryView> learned = memories.remember(request.userId(), qwen.extract(request.message()));
        List<MemoryView> recalled = memories.retrieve(request.userId(), request.message(), 5);
        return new ChatResponse(qwen.answer(request.message(), recalled), recalled, learned, qwen.live() ? "QWEN_CLOUD" : "DEMO");
    }

    @GetMapping("/memories")
    public List<MemoryView> list(@RequestParam String userId) { return memories.list(userId); }

    @DeleteMapping("/memories/{id}")
    public void forget(@PathVariable Long id, @RequestParam String userId) { memories.forget(userId, id); }

    @GetMapping("/health")
    public Object health() { return java.util.Map.of("status", "UP", "qwen", qwen.live() ? "configured" : "demo"); }
}
