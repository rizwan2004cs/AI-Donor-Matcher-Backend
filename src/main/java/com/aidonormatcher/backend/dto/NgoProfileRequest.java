package com.aidonormatcher.backend.dto;

import com.aidonormatcher.backend.enums.NeedCategory;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Payload for creating or updating an NGO profile.")
public record NgoProfileRequest(
        @Schema(description = "Public NGO name.", example = "Helping Hands")
        String name,
        @Schema(description = "Public NGO address. Saving this field triggers automatic backend geocoding and stores lat/lng on the NGO. The backend tries the full address first and then broader area fallbacks if the address is not specific enough.", example = "123 Main Street, Colombo")
        String address,
        @Schema(description = "Contact email displayed to donors.", example = "contact@helpinghands.org")
        String contactEmail,
        @Schema(description = "Contact phone number displayed to donors.", example = "+94 77 123 4567")
        String contactPhone,
        @Schema(description = "Short profile description.", example = "Supporting families with essential supplies.")
        String description,
        @Schema(description = "Primary category of work.", example = "FOOD")
        NeedCategory categoryOfWork
) {}
