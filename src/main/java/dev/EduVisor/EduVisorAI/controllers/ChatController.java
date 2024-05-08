package dev.EduVisor.EduVisorAI.controllers;

import org.springframework.web.bind.annotation.*;

import dev.EduVisor.EduVisorAI.models.chemical.ChemicalRequest;
import dev.EduVisor.EduVisorAI.models.chemical.ChemicalResponse;
import dev.EduVisor.EduVisorAI.services.ChemicalChatService;


import java.util.HashMap;
import java.util.Map;

@RestController
public class ChatController {

    private static final String HEALTH_STATUS = "UP";

    private final ChemicalChatService chatService;

    public ChatController(ChemicalChatService chatService) {
        this.chatService = chatService;
    }

    @GetMapping("/health")
    public Map<String, String> health() {
        Map<String, String> health = new HashMap<>();
        health.put("status", HEALTH_STATUS);
        return health;
    }

    @PostMapping("/api/chemical")
    public ChemicalResponse chemicalChat(@RequestBody ChemicalRequest request, @RequestHeader("userId") String userId) {
        return chatService.processChemicalChat(request, userId);
    }
}