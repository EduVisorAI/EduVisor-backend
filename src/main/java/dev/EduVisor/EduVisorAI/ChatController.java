package dev.EduVisor.EduVisorAI;

import dev.EduVisor.EduVisorAI.models.ChemicalResponse;
import org.springframework.ai.chat.ChatClient;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@RestController
public class ChatController {

    private final Map<String, List<Message>> conversationHistory;

    private final ChatClient chatClient;

    public ChatController(ChatClient chatClient) {
        this.chatClient = chatClient;
        this.conversationHistory = new HashMap<>();

    }


    // private static final String SYSTEM_MESSAGE = "Eres una IA de quimica para estudiantes universitarios que responderá solo preguntas de " +
    //     "quimica\n" + " [Importante] En caso se mencione un elemento quimico, debes colocar su CID al principio de la respuesta, " +
    //     "ejemplo CID=702, luego la respuesta, El CID debe estar SOLO al PRINCIPIO de la respuesta, si no se encuentra el CID no " +
    //     "lo menciones." + "En caso te pidan aguna imagen o gráfico debes dar el CID del componente quimico y luego la respuesta.";
    private static final String SYSTEM_MESSAGE = 
        "Eres una IA de química para estudiantes universitarios. " +
        "[IMPORTANTE] Solo puedes responder preguntas de química. " +
        "[IMPORTANTE] Cuando respondas, debes hacerlo en el formato 'CID={numero} {respuesta}'. " +
        "Por ejemplo, 'CID=702, El agua es una molécula compuesta por dos átomos de hidrógeno y uno de oxígeno.'";
        

    @PostMapping("/api/chemical")
    public ChemicalResponse jokes(@RequestBody Map<String, String> body, @RequestHeader("userId") String userId) {
        if (body == null || !body.containsKey("message")) {
            throw new IllegalArgumentException("Body must contain 'message' key");
        }

        String userChemicalRequest = body.get("message");
        var user = new UserMessage(userChemicalRequest);

        // Retrieve conversation history for this user
        List<Message> conversation = conversationHistory.getOrDefault(userId, new ArrayList<>());
        conversation.add(user);

        var system = new SystemMessage(SYSTEM_MESSAGE);
        conversation.add(system);
        Prompt prompt = new Prompt(conversation);

        String response;
        String cid;
        do {
            try {
                response = chatClient.call(prompt).getResult().getOutput().getContent();
                System.out.println(response);
            } catch (Exception e) {
                throw new RuntimeException("Failed to call chat client", e);
            }

            cid = extractCID(response);

            response = removeCID(response);

            // Store the system response in the conversation history
            var systemResponse = new SystemMessage(response);
            conversation.add(systemResponse);
            conversationHistory.put(userId, conversation);
        } while (cid.isEmpty());

        return new ChemicalResponse(cid, response);
    }

    private String extractCID(String response) {
        String cid = "";
        Pattern pattern = Pattern.compile("CID=(\\d+), ");
        Matcher matcher = pattern.matcher(response);
        if (matcher.find()) {
            cid = matcher.group(1);
            // Remove the CID from the response
            response = response.replaceFirst("CID=\\d+, ", "");
        }
        return cid;
    }

    private String removeCID(String response) {
        // Remove "CID={number}, " from the beginning of the response
        return response.replaceFirst("CID=\\d+, ", "");
    }
}