package com.basalt.ai.model;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * Inbound image generation request.
 */
@Data
public class ImageRequest {

    /** Natural-language prompt to send to Pollinations.ai. */
    @NotBlank(message = "prompt must not be blank")
    private String prompt;

    /** Desired image width in pixels (default: 1024). */
    private int width = 1024;

    /** Desired image height in pixels (default: 1024). */
    private int height = 1024;

    /**
     * Seed for deterministic generation. Use -1 for a random seed.
     */
    private long seed = -1L;
}

