package dev.EduVisor.EduVisorAI.services;

import org.springframework.ai.chat.ChatClient;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.stereotype.Service;

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

    private static final String SYSTEM_MESSAGE = 
        "Eres una IA de química para estudiantes universitarios. " +
        "[IMPORTANTE] Solo puedes responder preguntas de química. " +
        "[IMPORTANTE] Cuando respondas, debes hacerlo en el formato 'CID={numero} {respuesta}'. " +
        "[IMPORTANTE] El CID lo obtendrás de PUBChem." +
        "Por ejemplo, 'CID=702, El agua es una molécula compuesta por dos átomos de hidrógeno y uno de oxígeno.'";

    private static final String SYSTEM_RECORDATORY_MESSAGE = "No olvides responder en el siguiente formato: 'CID={numero} {respuesta}'.";
    private static final Pattern CID_PATTERN = Pattern.compile("CID=(\\d+)[, ]");
    private final Map<String, List<Message>> conversationHistory;
    private final ChatClient chatClient;

    public ChemicalChatService(ChatClient chatClient) {
        this.chatClient = chatClient;
        this.conversationHistory = new HashMap<>();
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
        String cid;
        int maxattempts = 2;
        int attempts = 0;
        do {
            try {
                response = chatClient.call(prompt).getResult().getOutput().getContent();
                System.out.println(response);
            } catch (Exception e) {
                throw new RuntimeException("Failed to call chat client", e);
            }

            cid = extractCID(response);

            response = removeCID(response);

            var systemResponse = new SystemMessage(response);
            conversation.add(systemResponse);

            // If CID is empty after the first attempt, add the reminder message
            if (cid.isEmpty() && attempts >= 1) {
                var reminder = new SystemMessage(SYSTEM_RECORDATORY_MESSAGE);
                conversation.add(reminder);
            }

            conversationHistory.put(userId, conversation);

            attempts++;
        } while (cid.isEmpty() && attempts < maxattempts);

        return new ChemicalResponse(cid, response);
    }

    private String extractCID(String response) {
        String cid = "";
        Matcher matcher = CID_PATTERN.matcher(response);
        if (matcher.find()) {
            cid = matcher.group(1);
        }
        return cid;
    }

    private String removeCID(String response) {
        // Remove "CID={number}, " or "CID={number} " from the beginning of the response
        return response.replaceFirst(CID_PATTERN.pattern(), "");
    }
}