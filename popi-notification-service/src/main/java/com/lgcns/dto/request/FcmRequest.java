package com.lgcns.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

public record FcmRequest(
        @NotBlank(message = "FCM 토큰은 필수입니다.")
                @Schema(description = "FCM 토큰", example = "fcmToken123")
                String fcmToken) {}
