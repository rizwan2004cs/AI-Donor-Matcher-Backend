package com.aidonormatcher.backend.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record PledgeRequest(
        @NotNull Long needId,
        @Min(1) int quantity
) {}
