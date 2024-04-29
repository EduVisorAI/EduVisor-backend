package dev.EduVisor.EduVisorAI;

import dev.EduVisor.EduVisorAI.models.ChemicalResponse;
import org.springframework.ai.chat.ChatClient;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@RestController
public class ChatController {

    private final ChatClient chatClient;

    public ChatController(ChatClient chatClient) {
        this.chatClient = chatClient;
    }


    @PostMapping("/api/chemical")
    public ChemicalResponse jokes(@RequestBody Map<String, String> body) {
        String userChemicalRequest = body.getOrDefault("chemicalRequest", "Cuál es el compuesto del hidrogeno?");
        var system = new SystemMessage("Eres una IA de quimica para estudiantes universitarios que responderá solo preguntas de " +
                "quimica\n" + " [Importante] En caso se mencione un elemento quimico, debes colocar su CID al principio de la respuesta, " +
                "ejemplo CID=702, luego la respuesta, El CID debe estar SOLO al PRINCIPIO de la respuesta, si no se encuentra el CID no " +
                "lo menciones");
        var user = new UserMessage(userChemicalRequest);
        Prompt prompt = new Prompt(List.of(system, user));
        String response = chatClient.call(prompt).getResult().getOutput().getContent();

        // Extract CID from the response
        String cid = extractCID(response);

        // Remove CID from the response
        response = removeCID(response);

        return new ChemicalResponse(cid, response);
    }

    private String extractCID(String response) {
        String cid = "";
        Pattern pattern = Pattern.compile("CID=(\\d+)");
        Matcher matcher = pattern.matcher(response);
        if (matcher.find()) {
            cid = matcher.group(1);
        }
        return cid;
    }

    private String removeCID(String response) {
        // Remove CID from the beginning of the response
        return response.replaceFirst("CID=\\d+\n?", "");
    }


}
