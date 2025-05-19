package com.lgcns.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

public record IdTokenRequest(
        @Schema(description = "Id Token") @NotBlank(message = "Id Token은 비워둘 수 없습니다.")
                String idToken) {}
