package com.konverza.sessions.dto;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class PronunciationDTO {
    private BigDecimal accuracyScore;
    private BigDecimal fluencyScore;
    private BigDecimal completenessScore;
    private BigDecimal prosodyScore;
    private String wordsDetail;
    private String phonemesDetail;
}
