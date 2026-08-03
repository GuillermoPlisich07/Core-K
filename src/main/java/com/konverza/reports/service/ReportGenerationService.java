package com.konverza.reports.service;

import com.konverza.reports.entity.SessionReport;
import com.konverza.reports.repository.SessionReportRepository;
import com.konverza.sessions.entity.BiometricSample;
import com.konverza.sessions.entity.PronunciationResult;
import com.konverza.sessions.entity.Session;
import com.konverza.sessions.entity.Transcript;
import com.konverza.sessions.repository.SessionRepository;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReportGenerationService {

    private final WebClient groqClient;
    private final SessionReportRepository reportRepository;
    private final SessionRepository sessionRepository;
    private final ObjectMapper objectMapper;

    public void generateReport(
            Session session,
            List<Transcript> transcripts,
            List<BiometricSample> biometrics,
            PronunciationResult pronunciation,
            String verbalAnalysisJson,
            String biometricSummaryJson,
            String vendorName,
            String scenarioName,
            String simulationEvents) {
        try {
            String prompt = buildPrompt(session, transcripts, biometrics, pronunciation,
                    verbalAnalysisJson, biometricSummaryJson, vendorName, scenarioName);
            String rawJson = callGroq(prompt);

            // Segunda llamada: sugerencias de mejora por turno del vendedor
            String turnSuggestionsJson = null;
            try {
                String suggestionsRaw = buildTurnSuggestionsJson(transcripts, session);
                if (suggestionsRaw != null) {
                    Map<?, ?> parsed = objectMapper.readValue(suggestionsRaw, Map.class);
                    if (parsed.get("suggestions") != null)
                        turnSuggestionsJson = objectMapper.writeValueAsString(parsed.get("suggestions"));
                }
            } catch (Exception e) {
                log.warn("Error generando sugerencias por turno: {}", e.getMessage());
            }

            SessionReport report = parseAndSaveReport(session, rawJson,
                    verbalAnalysisJson, biometricSummaryJson, vendorName, scenarioName, turnSuggestionsJson, simulationEvents);
            updateOverallScore(session, report);
        } catch (Exception e) {
            log.error("Error generando reporte para sesion {}: {}", session.getId(), e.getMessage());
        }
    }

    private String buildTurnSuggestionsJson(List<Transcript> transcripts, Session session) {
        List<Transcript> vendorTurns = transcripts.stream()
                .filter(t -> t.getSpeaker() == Transcript.Speaker.VENDOR)
                .limit(12)
                .collect(java.util.stream.Collectors.toList());
        if (vendorTurns.isEmpty()) return null;

        java.util.Set<Integer> vendorNums = new java.util.HashSet<>();
        vendorTurns.forEach(t -> vendorNums.add(t.getTurnNumber()));

        StringBuilder sb = new StringBuilder();
        sb.append("Sos un coach de ventas experto en español rioplatense (Uruguay/Argentina).\n");
        sb.append("Para cada turno del VENDEDOR, sugerí brevemente qué podría haber dicho mejor.\n\n");
        sb.append("Escenario: ").append(session.getScenario().getName()).append("\n");
        sb.append("Cliente: ").append(session.getScenario().getClientPersona()).append("\n\n");
        sb.append("Transcripción relevante:\n");

        for (Transcript t : transcripts) {
            boolean isVendor = t.getSpeaker() == Transcript.Speaker.VENDOR;
            boolean isPreContext = !isVendor && vendorNums.contains(t.getTurnNumber() + 1);
            if (!isVendor && !isPreContext) continue;
            String text = t.getText() != null && t.getText().length() > 160
                    ? t.getText().substring(0, 160) + "..." : t.getText();
            sb.append("T").append(t.getTurnNumber()).append(" [")
              .append(t.getSpeaker()).append("]: ").append(text).append("\n");
        }

        sb.append("\nPor cada turno del VENDEDOR, da UNA sugerencia concisa (1-2 oraciones).\n");
        sb.append("Empezá con 'Podrías decir...' o 'En vez de eso...'.\n");
        sb.append("Devolvé SOLO JSON válido:\n");
        sb.append("{\"suggestions\":[{\"turn\":2,\"suggestion\":\"...\"}]}\n");
        return callGroq(sb.toString());
    }

    @SuppressWarnings("unchecked")
    private String buildPrompt(
            Session session, List<Transcript> transcripts,
            List<BiometricSample> biometrics, PronunciationResult pronunciation,
            String verbalAnalysisJson, String biometricSummaryJson,
            String vendorNameOverride, String scenarioNameOverride) {

        String vName = vendorNameOverride != null ? vendorNameOverride : session.getVendorName();
        String sName = scenarioNameOverride != null ? scenarioNameOverride : session.getScenario().getName();

        StringBuilder sb = new StringBuilder();
        sb.append("Sos un coach de ventas experto. Analizá esta sesión y devolvé SOLO un JSON válido:\n");
        sb.append("{\"feedback_text\":\"...\",\"strengths\":[\"...\"],\"weaknesses\":[\"...\"],");
        sb.append("\"suggestions\":[\"...\"],");
        sb.append("\"score_persuasion\":7.5,\"score_confidence\":8.0,\"score_product_knowledge\":6.5,");
        sb.append("\"score_objection_handling\":7.0,\"score_pronunciation\":8.5}\n\n");

        sb.append("=== SESIÓN ===\n");
        sb.append("Vendedor: ").append(vName).append("\n");
        sb.append("Escenario: ").append(sName).append("\n");
        sb.append("Cliente: ").append(session.getScenario().getClientPersona()).append("\n");
        sb.append("Duración: ").append(session.getDurationSeconds()).append("s");
        sb.append(", Turnos: ").append(session.getTotalTurns()).append("\n\n");

        sb.append("=== TRANSCRIPCIÓN ===\n");
        for (Transcript t : transcripts) {
            sb.append(t.getSpeaker()).append(": ").append(t.getText()).append("\n");
        }

        if (verbalAnalysisJson != null && !verbalAnalysisJson.isEmpty()) {
            try {
                Map<?, ?> va = objectMapper.readValue(verbalAnalysisJson, Map.class);
                sb.append("\n=== COMPORTAMIENTO VERBAL ===\n");
                sb.append("Velocidad: ").append(va.get("avg_wpm")).append(" palabras/min");
                if (va.get("rhythm_label") != null) sb.append(" — ").append(va.get("rhythm_label"));
                sb.append("\n");
                if (va.get("filler_ratio_per_100_words") != null) {
                    sb.append("Muletillas por 100 palabras: ").append(va.get("filler_ratio_per_100_words")).append("\n");
                }
                Object fillers = va.get("top_fillers");
                if (fillers instanceof List<?> fl && !fl.isEmpty()) {
                    sb.append("Muletillas detectadas:\n");
                    for (Object f : fl) {
                        if (f instanceof Map<?, ?> fm) {
                            sb.append("  - \"").append(fm.get("word")).append("\": ")
                              .append(fm.get("count")).append("x (").append(fm.get("category")).append(")\n");
                        }
                    }
                }
                Object jargon = va.get("jargon_used");
                if (jargon instanceof List<?> jl && !jl.isEmpty()) {
                    sb.append("Jerga corporativa: ").append(jl).append("\n");
                }
                if (va.get("long_pauses") != null) {
                    sb.append("Silencios largos (>3s): ").append(va.get("long_pauses")).append("\n");
                }
            } catch (Exception e) {
                log.warn("No se pudo parsear verbal_analysis: {}", e.getMessage());
            }
        }

        if (biometricSummaryJson != null && !biometricSummaryJson.isEmpty()) {
            try {
                Map<?, ?> bs = objectMapper.readValue(biometricSummaryJson, Map.class);
                sb.append("\n=== ANÁLISIS BIOMÉTRICO ===\n");
                if (bs.get("avg_eye_contact_pct") != null) {
                    sb.append("Contacto visual promedio: ").append(bs.get("avg_eye_contact_pct")).append("%\n");
                }
                if (bs.get("head_stability_pct") != null) {
                    sb.append("Estabilidad de cabeza: ").append(bs.get("head_stability_pct")).append("%\n");
                }
                Object lowTurns = bs.get("low_eye_contact_turns");
                if (lowTurns instanceof List<?> lt && !lt.isEmpty()) {
                    sb.append("Turnos con bajo contacto visual: ").append(lt).append("\n");
                }
                Object faceEm = bs.get("face_emotion_distribution");
                if (faceEm instanceof Map<?, ?> fem && !fem.isEmpty()) {
                    sb.append("Emociones faciales: ").append(faceEm).append("\n");
                }
                Object voiceEm = bs.get("voice_emotion_distribution");
                if (voiceEm instanceof Map<?, ?> vem && !vem.isEmpty()) {
                    sb.append("Emociones vocales (Hume): ").append(vem).append("\n");
                }
            } catch (Exception e) {
                log.warn("No se pudo parsear biometric_summary: {}", e.getMessage());
            }
        }

        if (pronunciation != null) {
            sb.append("\n=== PRONUNCIACIÓN ===\n");
            sb.append("Accuracy: ").append(pronunciation.getAccuracyScore());
            sb.append(", Fluency: ").append(pronunciation.getFluencyScore());
            sb.append(", Prosody: ").append(pronunciation.getProsodyScore()).append("\n");
        }

        // Métricas conversacionales
        if (verbalAnalysisJson != null && !verbalAnalysisJson.isEmpty()) {
            try {
                Map<?, ?> va2 = objectMapper.readValue(verbalAnalysisJson, Map.class);
                sb.append("\n=== MÉTRICAS DE HABLA ===\n");
                if (va2.get("talk_ratio") != null) sb.append("Talk ratio: ").append(va2.get("talk_ratio")).append("% (óptimo: 43%)\n");
                if (va2.get("longest_monologue_sec") != null) sb.append("Monólogo más largo: ").append(va2.get("longest_monologue_sec")).append("s (máx recomendado: 90s)\n");
                if (va2.get("total_questions") != null) sb.append("Preguntas al cliente: ").append(va2.get("total_questions")).append("\n");
                if (va2.get("filler_ratio_per_100_words") != null) sb.append("Muletillas: ").append(va2.get("filler_ratio_per_100_words")).append("/100 palabras (óptimo <3)\n");
                Object topF = va2.get("top_fillers");
                if (topF instanceof List<?> tfl && !tfl.isEmpty()) {
                    sb.append("Top muletillas: ");
                    for (Object f : tfl) {
                        if (f instanceof Map<?, ?> fm)
                            sb.append("\"").append(fm.get("word")).append("\" (").append(fm.get("count")).append("x) ");
                    }
                    sb.append("\n");
                }
                Object negP = va2.get("negative_phrases");
                if (negP instanceof List<?> npl && !npl.isEmpty()) sb.append("Frases negativas: ").append(npl).append("\n");
                if (va2.get("lexical_diversity") != null) sb.append("Diversidad léxica (TTR): ").append(va2.get("lexical_diversity")).append(" (óptimo 0.50-0.70)\n");
            } catch (Exception e) { log.warn("verbal_analysis extendido: {}", e.getMessage()); }
        }

        // Métricas faciales
        if (biometricSummaryJson != null && !biometricSummaryJson.isEmpty()) {
            try {
                Map<?, ?> bs2 = objectMapper.readValue(biometricSummaryJson, Map.class);
                sb.append("\n=== MÉTRICAS FACIALES ===\n");
                if (bs2.get("avg_confidence_index") != null) sb.append("Confianza promedio: ").append(bs2.get("avg_confidence_index")).append("/10\n");
                if (bs2.get("avg_stress_index") != null) sb.append("Estrés promedio: ").append(bs2.get("avg_stress_index")).append("/10\n");
                if (bs2.get("smile_ratio") != null) sb.append("Sonrisa: ").append(bs2.get("smile_ratio")).append("% del tiempo\n");
                if (bs2.get("brow_furrow_ratio") != null) sb.append("Ceño fruncido: ").append(bs2.get("brow_furrow_ratio")).append("% del tiempo\n");
                if (bs2.get("looking_away_events") != null) sb.append("Veces que miró fuera de cámara: ").append(bs2.get("looking_away_events")).append("\n");
            } catch (Exception e) { log.warn("biometric_summary extendido: {}", e.getMessage()); }
        }

        sb.append("\n=== INSTRUCCIONES ===\n");
        sb.append("- Mencioná muletillas específicas detectadas si hay datos.\n");
        sb.append("- Correlacioná comportamiento verbal con emociones biométricas cuando sea relevante.\n");
        sb.append("- Si stress_index > 6 y jitter alto: mencioná nerviosismo consistente voz+cara.\n");
        sb.append("- feedback_text: 3-4 párrafos en español rioplatense.\n");
        sb.append("- Strengths/weaknesses/suggestions: 3-5 ítems cada uno.\n");
        sb.append("- Scores del 0 al 10 con un decimal. Solo JSON, sin markdown.\n");

        return sb.toString();
    }

    private String callGroq(String prompt) {
        Map<String, Object> body = Map.of(
            "model", "llama-3.3-70b-versatile",
            "messages", List.of(Map.of("role", "user", "content", prompt)),
            "temperature", 0.7,
            "max_tokens", 1500,
            "response_format", Map.of("type", "json_object")
        );
        int[] delays = {2000, 4000, 8000};
        Exception lastError = null;
        for (int attempt = 0; attempt <= delays.length; attempt++) {
            try {
                Map<?, ?> response = groqClient.post()
                        .uri("/chat/completions")
                        .bodyValue(body)
                        .retrieve()
                        .bodyToMono(Map.class)
                        .block();
                List<?> choices = (List<?>) response.get("choices");
                Map<?, ?> message = (Map<?, ?>) ((Map<?, ?>) choices.get(0)).get("message");
                return (String) message.get("content");
            } catch (Exception e) {
                lastError = e;
                if (attempt < delays.length) {
                    log.warn("Groq intento {} fallido. Reintentando en {}ms...", attempt + 1, delays[attempt]);
                    try { Thread.sleep(delays[attempt]); } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
        }
        throw new RuntimeException("Groq no disponible: " + (lastError != null ? lastError.getMessage() : "unknown"));
    }

    private SessionReport parseAndSaveReport(
            Session session, String rawJson,
            String verbalAnalysis, String biometricSummary,
            String vendorName, String scenarioName,
            String turnSuggestionsJson, String simulationEvents) throws Exception {
        Map<?, ?> d = objectMapper.readValue(rawJson, Map.class);

        // Extraer métricas verbales del JSON de análisis
        BigDecimal talkRatio = null, avgWpm = null, fillerRate = null, lexDiv = null, consistScore = null;
        BigDecimal avgF0Mean = null, avgJitter = null, avgHnr = null, acousticStress = null;
        BigDecimal openQuestionsRatio = null, productTermsCov = null;
        Integer longestMono = null, totalQ = null;
        String topFillersJson = null, negPhrasesJson = null;
        if (verbalAnalysis != null && !verbalAnalysis.isEmpty()) {
            try {
                Map<?, ?> va = objectMapper.readValue(verbalAnalysis, Map.class);
                talkRatio    = toBD(va.get("talk_ratio"));
                avgWpm       = toBD(va.get("avg_wpm"));
                fillerRate   = toBD(va.get("filler_ratio_per_100_words"));
                lexDiv       = toBD(va.get("lexical_diversity"));
                consistScore = toBD(va.get("consistency_score"));
                longestMono  = va.get("longest_monologue_sec") != null ? ((Number) va.get("longest_monologue_sec")).intValue() : null;
                totalQ       = va.get("total_questions") != null ? ((Number) va.get("total_questions")).intValue() : null;
                if (va.get("top_fillers") != null) topFillersJson = objectMapper.writeValueAsString(va.get("top_fillers"));
                if (va.get("negative_phrases") != null) negPhrasesJson = objectMapper.writeValueAsString(va.get("negative_phrases"));
                avgF0Mean          = toBD(va.get("avg_f0_mean"));
                avgJitter          = toBD(va.get("avg_jitter"));
                avgHnr             = toBD(va.get("avg_hnr"));
                acousticStress     = toBD(va.get("avg_acoustic_stress_score"));
                openQuestionsRatio = toBD(va.get("open_questions_ratio"));
                productTermsCov    = toBD(va.get("product_terms_coverage"));
            } catch (Exception e) { log.warn("verbal parse: {}", e.getMessage()); }
        }

        // Extraer métricas biométricas del JSON de resumen
        BigDecimal avgConf = null, avgStress = null, avgEngage = null, eyeRatio = null, smileR = null, browR = null;
        BigDecimal avgBlinkRate = null;
        Integer lookAway = null, peakStressTurn = null, peakConfidenceTurn = null;
        if (biometricSummary != null && !biometricSummary.isEmpty()) {
            try {
                Map<?, ?> bs = objectMapper.readValue(biometricSummary, Map.class);
                avgConf   = toBD(bs.get("avg_confidence_index"));
                avgStress = toBD(bs.get("avg_stress_index"));
                avgEngage = toBD(bs.get("avg_engagement_index"));
                eyeRatio  = toBD(bs.get("avg_eye_contact_pct"));
                smileR    = toBD(bs.get("smile_ratio"));
                browR     = toBD(bs.get("brow_furrow_ratio"));
                lookAway  = bs.get("looking_away_events") != null ? ((Number) bs.get("looking_away_events")).intValue() : null;
                avgBlinkRate       = toBD(bs.get("avg_blink_rate"));
                peakStressTurn     = bs.get("peak_stress_turn") != null ? ((Number) bs.get("peak_stress_turn")).intValue() : null;
                peakConfidenceTurn = bs.get("peak_confidence_turn") != null ? ((Number) bs.get("peak_confidence_turn")).intValue() : null;
            } catch (Exception e) { log.warn("biometric parse: {}", e.getMessage()); }
        }

        SessionReport report = SessionReport.builder()
                .session(session)
                .vendorName(vendorName != null ? vendorName : session.getVendorName())
                .scenarioName(scenarioName != null ? scenarioName : session.getScenario().getName())
                .verbalAnalysis(verbalAnalysis)
                .biometricSummary(biometricSummary)
                .feedbackText((String) d.get("feedback_text"))
                .strengths(objectMapper.writeValueAsString(d.get("strengths")))
                .weaknesses(objectMapper.writeValueAsString(d.get("weaknesses")))
                .suggestions(objectMapper.writeValueAsString(d.get("suggestions")))
                .scorePersuasion(toBD(d.get("score_persuasion")))
                .scoreConfidence(toBD(d.get("score_confidence")))
                .scoreProductKnowledge(toBD(d.get("score_product_knowledge")))
                .scoreObjectionHandling(toBD(d.get("score_objection_handling")))
                .scorePronunciation(toBD(d.get("score_pronunciation")))
                // Métricas verbales
                .talkRatio(talkRatio).avgSpeakingRateWpm(avgWpm)
                .longestMonologueSec(longestMono).totalQuestions(totalQ)
                .fillerRate(fillerRate).topFillers(topFillersJson)
                .lexicalDiversity(lexDiv).consistencyScore(consistScore)
                .negativePhrases(negPhrasesJson)
                // Métricas faciales
                .avgConfidenceIndex(avgConf).avgStressIndex(avgStress)
                .avgEngagementIndex(avgEngage).eyeContactRatio(eyeRatio)
                .smileRatio(smileR).browFurrowRatio(browR)
                .lookingAwayEvents(lookAway)
                .avgBlinkRate(avgBlinkRate)
                .peakStressTurn(peakStressTurn)
                .peakConfidenceTurn(peakConfidenceTurn)
                // Acústico agregado
                .avgF0Mean(avgF0Mean).avgJitter(avgJitter)
                .avgHnr(avgHnr).acousticStressScore(acousticStress)
                // Open questions + product coverage
                .openQuestionsRatio(openQuestionsRatio)
                .productTermsCoverage(productTermsCov)
                // Sugerencias por turno
                .turnSuggestions(turnSuggestionsJson)
                .simulationEvents(simulationEvents)
                .build();
        return reportRepository.save(report);
    }

    private void updateOverallScore(Session session, SessionReport r) {
        var scores = List.of(
                r.getScorePersuasion(), r.getScoreConfidence(),
                r.getScoreProductKnowledge(), r.getScoreObjectionHandling(), r.getScorePronunciation())
                .stream().filter(Objects::nonNull).toList();
        if (scores.isEmpty()) return;
        BigDecimal avg = scores.stream().reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(scores.size()), 2, RoundingMode.HALF_UP);
        session.setOverallScore(avg);
        sessionRepository.save(session);
    }

    private BigDecimal toBD(Object v) {
        if (v == null) return BigDecimal.ZERO;
        return new BigDecimal(v.toString()).setScale(2, RoundingMode.HALF_UP);
    }
}
