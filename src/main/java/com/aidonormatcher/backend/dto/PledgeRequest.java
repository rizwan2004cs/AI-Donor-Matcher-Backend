package com.aidonormatcher.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Payload for creating a donor pledge.")
public record PledgeRequest(
        @Schema(description = "Id of the need being pledged against.", example = "12")
        @NotNull Long needId,
        @Schema(description = "Quantity the donor commits to fulfill.", example = "10")
        @Min(1) int quantity
) {}
