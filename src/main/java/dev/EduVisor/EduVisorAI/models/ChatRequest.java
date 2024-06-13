package dev.EduVisor.EduVisorAI.models;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class ChatRequest {
    private String message;
    private String userId;
    private String chatId;
}