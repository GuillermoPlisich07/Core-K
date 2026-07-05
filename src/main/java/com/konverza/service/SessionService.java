package com.konverza.service;

import com.konverza.dto.*;
import com.konverza.entity.*;
import com.konverza.exception.SessionNotFoundException;
import com.konverza.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class SessionService {

    private final SessionRepository sessionRepository;
    private final ScenarioService scenarioService;
    private final TranscriptRepository transcriptRepository;
    private final BiometricSampleRepository biometricSampleRepository;
    private final PronunciationResultRepository pronunciationResultRepository;
    private final ReportGenerationService reportGenerationService;

    public Session createSession(SessionStartRequest req) {
        Scenario scenario = scenarioService.findById(req.getScenarioId());
        Session session = Session.builder()
                .scenario(scenario)
                .vendorName(req.getVendorName())
                .status(Session.Status.ACTIVE)
                .build();
        return sessionRepository.save(session);
    }

    public List<Session> findAll() {
        return sessionRepository.findAllByOrderByStartedAtDesc();
    }

    public Session findById(UUID id) {
        return sessionRepository.findById(id)
                .orElseThrow(() -> new SessionNotFoundException(id));
    }

    @Transactional
    public Session completeSession(UUID id, SessionCompleteRequest req) {
        Session session = findById(id);
        if (session.getStatus() == Session.Status.COMPLETED) {
            log.info("Sesion {} ya estaba completada — respondiendo idempotentemente", id);
            return session;
        }
        session.setStatus(Session.Status.COMPLETED);
        session.setEndedAt(LocalDateTime.now());
        session.setDurationSeconds(req.getDurationSeconds());
        session.setTotalTurns(req.getTotalTurns());
        sessionRepository.save(session);

        List<Transcript> transcripts = (req.getTranscript() != null ? req.getTranscript() : List.<TranscriptTurnDTO>of())
            .stream().map(t ->
            Transcript.builder().session(session).turnNumber(t.getTurnNumber())
                .speaker(t.getSpeaker()).text(t.getText())
                .timestampStartMs(t.getTimestampStartMs()).timestampEndMs(t.getTimestampEndMs())
                .wordCount(t.getWordCount()).speakingRateWpm(t.getSpeakingRateWpm())
                .pauseBeforeMs(t.getPauseBeforeMs())
                .acousticF0Mean(t.getAcousticF0Mean()).acousticF0Std(t.getAcousticF0Std())
                .acousticJitter(t.getAcousticJitter()).acousticShimmer(t.getAcousticShimmer())
                .acousticHnr(t.getAcousticHnr()).acousticRmsMean(t.getAcousticRmsMean())
                .acousticZcrMean(t.getAcousticZcrMean()).acousticStressScore(t.getAcousticStressScore())
                .fillerCount(t.getFillerCount()).fillerWords(t.getFillerWords())
                .questionCount(t.getQuestionCount())
                .build()
        ).collect(Collectors.toList());
        transcriptRepository.saveAll(transcripts);

        List<BiometricSample> biometrics = (req.getBiometricSamples() != null ? req.getBiometricSamples() : List.<BiometricSampleDTO>of())
            .stream().map(b ->
            BiometricSample.builder().session(session).timestampMs(b.getTimestampMs())
                .source(b.getSource()).emotions(b.getEmotions())
                .dominantEmotion(b.getDominantEmotion()).confidence(b.getConfidence())
                .eyeContact(b.getEyeContact()).headOrientation(b.getHeadOrientation())
                .headYaw(b.getHeadYaw()).headPitch(b.getHeadPitch()).headRoll(b.getHeadRoll())
                .eyeOpenness(b.getEyeOpenness()).blinkDetected(b.getBlinkDetected())
                .smileIntensity(b.getSmileIntensity()).browFurrow(b.getBrowFurrow())
                .mouthOpen(b.getMouthOpen()).faceDetected(b.getFaceDetected())
                .confidenceIndex(b.getConfidenceIndex()).stressIndex(b.getStressIndex())
                .engagementIndex(b.getEngagementIndex())
                .acousticFeatures(b.getAcousticFeatures())
                .acousticStressScore(b.getAcousticStressScore())
                .build()
        ).collect(Collectors.toList());
        biometricSampleRepository.saveAll(biometrics);

        PronunciationResult pronunciation = null;
        if (req.getPronunciation() != null) {
            PronunciationDTO p = req.getPronunciation();
            pronunciation = PronunciationResult.builder().session(session)
                    .accuracyScore(p.getAccuracyScore()).fluencyScore(p.getFluencyScore())
                    .completenessScore(p.getCompletenessScore()).prosodyScore(p.getProsodyScore())
                    .wordsDetail(p.getWordsDetail()).phonemesDetail(p.getPhonemesDetail()).build();
            pronunciationResultRepository.save(pronunciation);
        }

        log.info("Sesion {} completada. Generando reporte en background...", id);
        final PronunciationResult finalPron = pronunciation;
        final List<Transcript> savedTranscripts = transcripts;
        final List<BiometricSample> savedBiometrics = biometrics;
        final String verbalAnalysis = req.getVerbalAnalysis();
        final String biometricSummary = req.getBiometricSummary();
        final String vendorName = req.getVendorName();
        final String scenarioName = req.getScenarioName();
        Thread.ofVirtual().start(() ->
            reportGenerationService.generateReport(
                session, savedTranscripts, savedBiometrics, finalPron,
                verbalAnalysis, biometricSummary, vendorName, scenarioName
            )
        );
        return session;
    }

    public List<Transcript> getTranscript(UUID sessionId) {
        return transcriptRepository.findBySessionIdOrderByTurnNumber(sessionId);
    }
}
