package com.aidonormatcher.backend.dto;

import com.aidonormatcher.backend.enums.NeedCategory;
import com.aidonormatcher.backend.enums.UrgencyLevel;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record NeedRequest(
        @NotNull NeedCategory category,
        @NotBlank String itemName,
        String description,
        @Min(1) int quantityRequired,
        @NotNull UrgencyLevel urgency,
        LocalDate expiryDate
) {}
