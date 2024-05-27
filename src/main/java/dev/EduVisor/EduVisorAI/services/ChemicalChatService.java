package dev.EduVisor.EduVisorAI.services;

import org.springframework.ai.chat.ChatClient;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import dev.EduVisor.EduVisorAI.models.chemical.ChemicalRequest;
import dev.EduVisor.EduVisorAI.models.chemical.ChemicalResponse;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class ChemicalChatService {

    private static final String SYSTEM_MESSAGE = "Eres una IA de química diseñada específicamente para estudiantes universitarios. "
            +
            "1. **[IMPORTANTE]** Solo puedes responder preguntas relacionadas con química. " +
            "2. **[IMPORTANTE]** Todas tus respuestas deben seguir el formato: `Component={Nombre del componente en inglés} Answer={respuesta}`. "
            +
            "Ejemplo de respuesta: " +
            "- Pregunta: \"¿Qué es el agua?\" " +
            "- Respuesta: `Component=Water Answer=El agua es una molécula compuesta por dos átomos de hidrógeno y uno de oxígeno.`";

    private static final Pattern COMPONENT_PATTERN = Pattern.compile("Component=([^ ]+)");
    private static final Pattern ANSWER_PATTERN = Pattern.compile("Answer=(.+)");

    private final Map<String, List<Message>> conversationHistory;
    private final ChatClient chatClient;

    private final RestTemplate restTemplate;

    public ChemicalChatService(ChatClient chatClient, RestTemplate restTemplate) {
        this.chatClient = chatClient;
        this.conversationHistory = new HashMap<>();
        this.restTemplate = restTemplate;
    }

    public ChemicalResponse processChemicalChat(ChemicalRequest request, String userId) {
        String userChemicalRequest = request.getMessage();
        var user = new UserMessage(userChemicalRequest);

        // Retrieve conversation history for this user
        List<Message> conversation = conversationHistory.getOrDefault(userId, new ArrayList<>());
        conversation.add(user);

        var system = new SystemMessage(SYSTEM_MESSAGE);
        conversation.add(system);
        Prompt prompt = new Prompt(conversation);

        String response;
        String component;
        String answer;
        try {
            response = chatClient.call(prompt).getResult().getOutput().getContent();
            System.out.println(response);
        } catch (Exception e) {
            throw new RuntimeException("Failed to call chat client", e);
        }

        component = extractComponent(response);
        answer = extractAnswer(response);
        String cid = getCidFromPubChem(component);

        response = removeComponentAndAnswer(response);

        var systemResponse = new SystemMessage(response);
        conversation.add(systemResponse);

        conversationHistory.put(userId, conversation);

        return new ChemicalResponse(component, answer, cid);
    }

    private String extractComponent(String response) {
        String component = "";
        Matcher matcher = COMPONENT_PATTERN.matcher(response);
        if (matcher.find()) {
            component = matcher.group(1);
        }
        return component;
    }

    private String extractAnswer(String response) {
        String answer = "";
        Matcher matcher = ANSWER_PATTERN.matcher(response);
        if (matcher.find()) {
            answer = matcher.group(1);
        }
        return answer;
    }

    private String getCidFromPubChem(String component) {
        String url = "https://pubchem.ncbi.nlm.nih.gov/rest/pug/compound/name/" + component + "/cids/JSON";
        String result = restTemplate.getForObject(url, String.class);
        String cid = extractCid(result);
        return cid;
    }

    private String extractCid(String response) {
        String cid = "";
        Pattern cidPattern = Pattern.compile("\"CID\":\\s*\\[\\s*(\\d+)\\s*\\]");
        Matcher matcher = cidPattern.matcher(response);
        if (matcher.find()) {
            cid = matcher.group(1);
        }
        return cid;
    }

    private String removeComponentAndAnswer(String response) {
        // Remove "Component={component} " and "Answer={answer}" from the response
        return response.replaceFirst(COMPONENT_PATTERN.pattern(), "").replaceFirst(ANSWER_PATTERN.pattern(), "").trim();
    }
}