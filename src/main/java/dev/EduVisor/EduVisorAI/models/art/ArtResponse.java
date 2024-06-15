package dev.EduVisor.EduVisorAI.models.art;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class ArtResponse {
    private String title;
    private String answer;
    private List<String> imageUrl;
}
