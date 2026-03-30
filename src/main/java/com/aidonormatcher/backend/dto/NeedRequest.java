package com.aidonormatcher.backend.dto;

import com.aidonormatcher.backend.enums.NeedCategory;
import com.aidonormatcher.backend.enums.UrgencyLevel;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

@Schema(description = "Payload for creating or editing an NGO need.")
public record NeedRequest(
        @Schema(description = "Category of the requested item.", example = "FOOD")
        @NotNull NeedCategory category,
        @Schema(description = "Display name of the requested item.", example = "Rice packs")
        @NotBlank String itemName,
        @Schema(description = "Optional details about the request.", example = "5kg rice packs for 50 families")
        String description,
        @Schema(description = "Total quantity required.", example = "50")
        @Min(1) int quantityRequired,
        @Schema(description = "Urgency level for the need.", example = "HIGH")
        @NotNull UrgencyLevel urgency,
        @Schema(description = "Optional date after which the need expires.", example = "2026-04-15")
        LocalDate expiryDate
) {}
