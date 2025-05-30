package com.lgcns.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

public record SurveyChoiceRequest(
        @Schema(description = "설문지 ID", example = "24") @NotNull Long surveyId,
        @Schema(description = "선지 ID", example = "130") @NotNull Long choiceId) {}
