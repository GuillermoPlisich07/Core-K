package com.konverza.sessions.service;

import com.konverza.auth.entity.User;
import com.konverza.auth.repository.UserRepository;
import com.konverza.auth.security.CurrentUser;
import com.konverza.auth.security.ServiceTokenService;
import com.konverza.reports.service.ReportGenerationService;
import com.konverza.scenarios.entity.Scenario;
import com.konverza.scenarios.service.ScenarioService;
import com.konverza.sessions.dto.BiometricSampleDTO;
import com.konverza.sessions.dto.PronunciationDTO;
import com.konverza.sessions.dto.SessionCompleteRequest;
import com.konverza.sessions.dto.SessionStartRequest;
import com.konverza.sessions.dto.TranscriptTurnDTO;
import com.konverza.sessions.entity.BiometricSample;
import com.konverza.sessions.entity.PronunciationResult;
import com.konverza.sessions.entity.Session;
import com.konverza.sessions.entity.Transcript;
import com.konverza.sessions.exception.SessionNotFoundException;
import com.konverza.sessions.repository.BiometricSampleRepository;
import com.konverza.sessions.repository.PronunciationResultRepository;
import com.konverza.sessions.repository.SessionRepository;
import com.konverza.sessions.repository.TranscriptRepository;

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
    private final ServiceTokenService serviceTokenService;
    private final UserRepository userRepository;

    public Session createSession(SessionStartRequest req) {
        Scenario scenario = scenarioService.findById(req.getScenarioId());
        UUID currentUserId = CurrentUser.id();
        Session session = Session.builder()
                .scenario(scenario)
                .vendorName(req.getVendorName())
                .user(userRepository.findById(currentUserId).orElse(null))
                .status(Session.Status.ACTIVE)
                .build();
        session = sessionRepository.save(session);

        session.setDelegatedToken(mintDelegatedToken(currentUserId, scenario.getId(), session.getId()));
        return session;
    }

    /**
     * Mints a delegated token scoped to the current authenticated user (set by
     * JwtAuthenticationFilter) so Web-k can present it directly to AI-Service-k.
     * The user is already authorized to reach this endpoint at all — Core-k
     * requires a valid session for every non-/api/auth route — so no further
     * per-scenario authorization check exists yet beyond the scenario existing.
     */
    private String mintDelegatedToken(UUID userId, UUID scenarioId, UUID sessionId) {
        return serviceTokenService.mintDelegatedToken(userId, CurrentUser.role(), scenarioId, sessionId);
    }

    /**
     * EMPLOYEE sees only their own sessions; ADMIN/EXEC see all
     * (add-rbac-permission-matrix: Analytics/Reportes — Personal | Completo | Completo).
     */
    public List<Session> findAll() {
        if ("EMPLOYEE".equals(CurrentUser.role())) {
            return sessionRepository.findAllByUserIdOrderByStartedAtDesc(CurrentUser.id());
        }
        return sessionRepository.findAllByOrderByStartedAtDesc();
    }

    /**
     * Enforces the same own-vs-all rule as {@link #findAll()} on individual
     * record access: an EMPLOYEE requesting a session they don't own gets the
     * same 404 as a nonexistent session, rather than a 403 that would confirm
     * the record exists.
     */
    public Session findById(UUID id) {
        Session session = sessionRepository.findById(id)
                .orElseThrow(() -> new SessionNotFoundException(id));
        if ("EMPLOYEE".equals(CurrentUser.role())) {
            UUID ownerId = session.getUser() != null ? session.getUser().getId() : null;
            if (!CurrentUser.id().equals(ownerId)) {
                throw new SessionNotFoundException(id);
            }
        }
        return session;
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
        findById(sessionId); // enforces own-vs-all access before returning the transcript
        return transcriptRepository.findBySessionIdOrderByTurnNumber(sessionId);
    }
}
