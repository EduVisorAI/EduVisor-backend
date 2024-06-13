package dev.EduVisor.EduVisorAI.models.art;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ArtResponse {
    private String answer;
    private String imageUrl;
}
