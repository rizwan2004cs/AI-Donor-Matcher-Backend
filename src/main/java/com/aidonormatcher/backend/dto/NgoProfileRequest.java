package com.aidonormatcher.backend.dto;

import com.aidonormatcher.backend.enums.NeedCategory;

public record NgoProfileRequest(
        String name,
        String address,
        String contactEmail,
        String contactPhone,
        String description,
        NeedCategory categoryOfWork
) {}
