package dev.EduVisor.EduVisorAI.services;

import org.springframework.ai.chat.ChatClient;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import dev.EduVisor.EduVisorAI.models.ChatRequest;
import dev.EduVisor.EduVisorAI.models.chemical.ChemicalResponse;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.cloud.translate.Translate;
import com.google.cloud.translate.TranslateOptions;
import com.google.cloud.translate.Translation;

@Service
public class ChemicalChatService {

    private String translateToSpanish(String text) {
        Translate translate = TranslateOptions.getDefaultInstance().getService();
        Translation translation = translate.translate(text, Translate.TranslateOption.targetLanguage("es"));
        return translation.getTranslatedText();
    }

    private static final String SYSTEM_MESSAGE = "Eres una IA de química diseñada específicamente para estudiantes universitarios. "
            +
            "1. **[IMPORTANTE]** Solo puedes responder preguntas relacionadas con química. " +
            "2. **[IMPORTANTE]** Todas tus respuestas deben seguir el formato: `Component={Nombre del componente en inglés} Answer={respuesta}`. "
            +
            "Ejemplo de respuesta: " +
            "- Pregunta: \"¿Qué es el agua?\" " +
            "- Respuesta: `Component={Water} Answer={El agua es una molécula compuesta por dos átomos de hidrógeno y uno de oxígeno.}`\n"
            +
            "Ejemplo de respuesta2: " +
            "- Pregunta: \"dame una lista de quimicos que se combinan con el hidrogeno\" " +
            "- Respuesta: `Component={Hidrogen} Answer={Claro, aquí tienes una lista de algunos químicos que se combinan con el hidrógeno para formar diferentes compuestos:\n\n"
            +
            "Oxígeno (O₂): Forma agua (H₂O).\n" +
            "Cloro (Cl₂): Forma ácido clorhídrico (HCl).\n" +
            "Azufre (S): Forma ácido sulfhídrico (H₂S).\n" +
            "Nitrógeno (N₂): Forma amoníaco (NH₃).\n" +
            "Carbono (C): Forma metano (CH₄).\n" +
            "Flúor (F₂): Forma ácido fluorhídrico (HF).\n" +
            "Bromo (Br₂): Forma ácido bromhídrico (HBr).\n" +
            "Yodo (I₂): Forma ácido yodhídrico (HI).\n" +
            "Fósforo (P): Forma fosfina (PH₃).\n" +
            "Silicio (Si): Forma silano (SiH₄).}`"
            +
            "Ejemplo de respuesta3: " +
            "- Pregunta: \"Quien es el presidente de Perú\" " +
            "- Respuesta: `Component={Perú} Answer={Lo siento, solo puedo responder preguntas relacionadas a química.}`";
    private static final Pattern COMPONENT_PATTERN = Pattern.compile("Component=\\{(.*?)\\}");
    private static final Pattern ANSWER_PATTERN = Pattern.compile("Answer=\\{(.*?)\\}", Pattern.DOTALL);

    private final Map<String, List<Message>> conversationHistory;
    private final ChatClient chatClient;

    private final RestTemplate restTemplate;

    public ChemicalChatService(ChatClient chatClient, RestTemplate restTemplate) {
        this.chatClient = chatClient;
        this.conversationHistory = new HashMap<>();
        this.restTemplate = restTemplate;
    }

    public ChemicalResponse processChemicalChat(ChatRequest request, String userId, String chatId) {
        String userChemicalRequest = request.getMessage();
        var user = new UserMessage(userChemicalRequest);

        // Use a combination of userId and chatId as the key
        String userChatKey = userId + "-" + chatId;

        // Retrieve conversation history for this user and chat
        List<Message> conversation = conversationHistory.getOrDefault(userChatKey, new ArrayList<>());
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

        if (!cid.isEmpty()) {
            component = translateToSpanish(component);
        }

        response = removeComponentAndAnswer(response);

        var systemResponse = new SystemMessage(response);
        conversation.add(systemResponse);

        // Store the conversation history using the userChatKey
        conversationHistory.put(userChatKey, conversation);

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
        String cid = "";
        try {
            String result = restTemplate.getForObject(url, String.class);
            cid = extractCid(result);
        } catch (HttpClientErrorException.NotFound e) {
            // Log the error or handle it as you see fit
        }
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