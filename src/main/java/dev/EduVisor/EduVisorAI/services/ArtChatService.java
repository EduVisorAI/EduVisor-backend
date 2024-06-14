package dev.EduVisor.EduVisorAI.services;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;

import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.ai.chat.ChatClient;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.stereotype.Service;

import dev.EduVisor.EduVisorAI.models.ChatRequest;
import dev.EduVisor.EduVisorAI.models.art.ArtResponse;
import io.github.cdimascio.dotenv.Dotenv;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class ArtChatService {

    private static final String SYSTEM_MESSAGE = "Eres una IA de Arte diseñada específicamente para estudiantes universitarios. "
            +
            "1. **[IMPORTANTE]** Solo puedes responder preguntas relacionadas con arte. " +
            "2. **[IMPORTANTE]** ABSOLUTAMENTE Todas tus respuestas deben seguir el formato: `Answer={respuesta}`. "
            +
            "Ejemplo de respuesta: " +
            "- Pregunta: \"¿Quien pintó la Mona Lisa?\" " +
            "- Respuesta: `Answer={La Mona Lisa fue pintada por Leonardo da Vinci, un artista renacentista italiano, entre los años 1503 y 1506. Es una de las obras de arte más famosas y reconocibles en el mundo, conocida por la enigmática expresión de la mujer retratada y por su compleja técnica de sombreado llamada sfumato. La pintura se encuentra actualmente en el Museo del Louvre en París, Francia.}`\n";

    private static final Pattern ANSWER_PATTERN = Pattern.compile("Answer=\\{(.*?)\\}", Pattern.DOTALL);
    private static final Dotenv dotenv = Dotenv.load();
    private static final String SERPAPI_API_KEY = dotenv.get("SERPAPI_API_KEY");

    private final Map<String, List<Message>> conversationHistory;
    private final ChatClient chatClient;

    public ArtChatService(ChatClient chatClient) {
        this.chatClient = chatClient;
        this.conversationHistory = new HashMap<>();
    }

    public ArtResponse processArtChat(ChatRequest request, String userId, String chatId) {
        String userArtRequest = request.getMessage();
        var user = new UserMessage(userArtRequest);

        // Use a combination of userId and chatId as the key
        String userChatKey = userId + "-" + chatId;

        // Retrieve conversation history for this user and chat
        List<Message> conversation = conversationHistory.getOrDefault(userChatKey, new ArrayList<>());
        conversation.add(user);

        var system = new SystemMessage(SYSTEM_MESSAGE);
        conversation.add(system);
        Prompt prompt = new Prompt(conversation);

        String response;
        String answer;
        try {
            response = chatClient.call(prompt).getResult().getOutput().getContent();
            System.out.println(response);
        } catch (Exception e) {
            throw new RuntimeException("Failed to call chat client", e);
        }

        answer = extractAnswer(response);

        String imageUrl = fetchImageUrl(userArtRequest);

        var systemResponse = new SystemMessage(response);
        conversation.add(systemResponse);

        // Store the conversation history using the userChatKey
        conversationHistory.put(userChatKey, conversation);

        return new ArtResponse(answer, imageUrl);
    }

    private String extractAnswer(String response) {
        String answer = "";
        Matcher matcher = ANSWER_PATTERN.matcher(response);
        if (matcher.find()) {
            answer = matcher.group(1);
        }
        return answer;
    }

    private String fetchImageUrl(String query) {
        String encodedQuery = URLEncoder.encode(query, StandardCharsets.UTF_8);
        String url = "https://serpapi.com/search.json?q=" + encodedQuery + "&tbm=isch&api_key=" + SERPAPI_API_KEY;
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .build();

        try {
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            String responseBody = response.body();
            JSONObject json = new JSONObject(responseBody);
            JSONArray imagesResults = json.getJSONArray("images_results");

            if (imagesResults.length() > 0) {
                JSONObject image = imagesResults.getJSONObject(0);
                return image.getString("original");
            }
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }

        return "";
    }
}
