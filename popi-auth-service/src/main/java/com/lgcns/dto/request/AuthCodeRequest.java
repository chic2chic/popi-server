package com.lgcns.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

public record AuthCodeRequest(
        @NotNull(message = "인증코드는 필수입니다") @Schema(description = "구글, 카카오 로그인을 통한 인증코드")
                String code) {}
