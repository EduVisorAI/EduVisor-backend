package dev.EduVisor.EduVisorAI.models.chemical;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ChemicalResponse {
    private String CID;
    private String response;
}