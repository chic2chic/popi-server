package com.lgcns.dto.request;

import jakarta.validation.constraints.NotNull;

public record AuthCodeRequest(@NotNull(message = "인증코드는 필수입니다") String code) {}
