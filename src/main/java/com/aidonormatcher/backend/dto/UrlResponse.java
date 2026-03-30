package com.aidonormatcher.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Response body containing a URL.")
public record UrlResponse(
        @Schema(description = "Uploaded asset URL.", example = "https://res.cloudinary.com/demo/image/upload/sample.jpg")
        String url) {
}
