package dev.EduVisor.EduVisorAI.controllers;

import dev.EduVisor.EduVisorAI.models.art.ArtResponse;
import dev.EduVisor.EduVisorAI.services.ArtChatService;
import org.springframework.web.bind.annotation.*;

import dev.EduVisor.EduVisorAI.models.ChatRequest;
import dev.EduVisor.EduVisorAI.models.chemical.ChemicalResponse;
import dev.EduVisor.EduVisorAI.services.ChemicalChatService;

import java.util.HashMap;
import java.util.Map;

@RestController
public class ChatController {

    private static final String HEALTH_STATUS = "UP";

    private final ChemicalChatService chemicalChatService;
    private final ArtChatService artChatService;

    public ChatController(ChemicalChatService chemicalChatService, ArtChatService artChatService) {
        this.chemicalChatService = chemicalChatService;
        this.artChatService = artChatService;
    }

    @GetMapping("/health")
    public Map<String, String> health() {
        Map<String, String> health = new HashMap<>();
        health.put("status", HEALTH_STATUS);
        return health;
    }

    @PostMapping("/api/chemical")
    public ChemicalResponse chemicalChat(@RequestBody ChatRequest request) {
        return chemicalChatService.processChemicalChat(request, request.getUserId(), request.getChatId());
    }

    @PostMapping("/api/art")
    public ArtResponse artChat(@RequestBody ChatRequest request) {
        return artChatService.processArtChat(request, request.getUserId(), request.getChatId());
    }
}