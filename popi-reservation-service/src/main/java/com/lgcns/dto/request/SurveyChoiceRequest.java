package com.lgcns.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;

public record SurveyChoiceRequest(
        @Schema(description = "설문지 ID", example = "24") Long surveyId,
        @Schema(description = "선지 ID", example = "130") Long choiceId) {}
