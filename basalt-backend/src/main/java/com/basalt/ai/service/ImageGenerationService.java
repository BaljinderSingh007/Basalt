package com.basalt.ai.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * Image generation service using AI Horde — a free, open-source,
 * crowd-sourced Stable Diffusion API that requires no authentication.
 *
 * <h3>Flow:</h3>
 * <ol>
 *   <li>Submit a generation request → receive a job ID.</li>
 *   <li>Poll the status endpoint until {@code done == true}.</li>
 *   <li>Extract the image URL from the completed generation.</li>
 * </ol>
 *
 * @see <a href="https://aihorde.net/api">AI Horde API docs</a>
 */
@Slf4j
@Service
public class ImageGenerationService {

    private static final String HORDE_BASE = "https://aihorde.net/api/v2";
    private static final String ANONYMOUS_API_KEY = "0000000000";
    private static final int MAX_POLL_ATTEMPTS = 60;         // ~2 minutes max
    private static final Duration POLL_INTERVAL = Duration.ofSeconds(2);
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(15);

    private static final String CLIENT_AGENT = "basalt:1.0:unknown";

    private final WebClient webClient;

    public ImageGenerationService(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder
                .baseUrl(HORDE_BASE)
                .defaultHeader("apikey", ANONYMOUS_API_KEY)
                .defaultHeader("Client-Agent", CLIENT_AGENT)
                .defaultHeader("User-Agent", CLIENT_AGENT)
                .build();
    }

    /**
     * Generates an AI image from the given prompt.
     *
     * @param prompt user's text prompt
     * @param width  desired width (clamped to nearest multiple of 64, max 1024)
     * @param height desired height (clamped to nearest multiple of 64, max 1024)
     * @return Mono containing the public image URL
     */
    public Mono<String> generateImage(String prompt, int width, int height) {
        // AI Horde requires dimensions as multiples of 64, max 1024
        int w = clampDimension(width);
        int h = clampDimension(height);

        log.info("Submitting to AI Horde — prompt=\"{}\", {}x{}", prompt, w, h);

        Map<String, Object> body = Map.of(
                "prompt", prompt + ", detailed, high quality",
                "params", Map.of(
                        "width", w,
                        "height", h,
                        "steps", 25,
                        "cfg_scale", 7.5,
                        "n", 1
                ),
                "nsfw", false,
                "censor_nsfw", true
        );

        return submitJob(body)
                .flatMap(this::pollUntilDone)
                .timeout(Duration.ofMinutes(3))
                .doOnSuccess(url -> log.info("Image generated: {}", url))
                .doOnError(ex -> log.error("Image generation failed: {}", ex.getMessage()));
    }

    // ── Submit ─────────────────────────────────────────────────────────────

    private Mono<String> submitJob(Map<String, Object> body) {
        return webClient.post()
                .uri("/generate/async")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .retrieve()
                .bodyToMono(Map.class)
                .timeout(REQUEST_TIMEOUT)
                .map(resp -> {
                    String jobId = (String) resp.get("id");
                    log.debug("AI Horde job submitted — id={}", jobId);
                    return jobId;
                })
                .onErrorResume(WebClientResponseException.class, ex -> {
                    log.error("AI Horde submit failed — {} — body: {}",
                            ex.getStatusCode(), ex.getResponseBodyAsString());
                    return Mono.error(new RuntimeException(
                            "AI Horde returned " + ex.getStatusCode()
                                    + ": " + ex.getResponseBodyAsString()));
                });
    }

    // ── Poll ───────────────────────────────────────────────────────────────

    @SuppressWarnings("unchecked")
    private Mono<String> pollUntilDone(String jobId) {
        return Mono.defer(() -> checkStatus(jobId))
                .flatMap(status -> {
                    Boolean done = (Boolean) status.get("done");
                    if (Boolean.TRUE.equals(done)) {
                        List<Map<String, Object>> gens =
                                (List<Map<String, Object>>) status.get("generations");
                        if (gens != null && !gens.isEmpty()) {
                            return Mono.just((String) gens.get(0).get("img"));
                        }
                        return Mono.error(new RuntimeException("No generations in completed job"));
                    }
                    Integer waiting = (Integer) status.getOrDefault("waiting", 0);
                    Integer processing = (Integer) status.getOrDefault("processing", 0);
                    log.debug("AI Horde job {} — waiting={}, processing={}", jobId, waiting, processing);
                    return Mono.empty();
                })
                .repeatWhenEmpty(MAX_POLL_ATTEMPTS, flux -> flux.delayElements(POLL_INTERVAL))
                .switchIfEmpty(Mono.error(new RuntimeException(
                        "Image generation timed out after " + MAX_POLL_ATTEMPTS + " poll attempts")))
                .cast(String.class);
    }

    @SuppressWarnings("rawtypes")
    private Mono<Map> checkStatus(String jobId) {
        return webClient.get()
                .uri("/generate/status/{id}", jobId)
                .retrieve()
                .bodyToMono(Map.class)
                .timeout(REQUEST_TIMEOUT);
    }

    // ── Helpers ────────────────────────────────────────────────────────────

    private static int clampDimension(int value) {
        int clamped = Math.max(64, Math.min(value, 1024));
        return (clamped / 64) * 64;  // round down to nearest 64
    }
}
