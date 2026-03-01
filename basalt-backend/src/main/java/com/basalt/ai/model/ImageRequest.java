package com.basalt.ai.model;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * Inbound image generation request.
 */
@Data
public class ImageRequest {

    /** Natural-language prompt to send to the image generation API. */
    @NotBlank(message = "prompt must not be blank")
    private String prompt;

    /** Desired image width in pixels (default: 512, max 728 for free tier). */
    private int width = 512;

    /** Desired image height in pixels (default: 512, max 728 for free tier). */
    private int height = 512;

    /**
     * Seed for deterministic generation. Use -1 for a random seed.
     */
    private long seed = -1L;
}

