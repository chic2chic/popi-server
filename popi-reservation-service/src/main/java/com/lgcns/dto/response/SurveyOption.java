package com.lgcns.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

public record SurveyOption(
        @Schema(description = "선지 번호", example = "1") Long choiceId,
        @Schema(description = "선지 내용", example = "포스터") String content) {
    public static SurveyOption of(Long choiceId, String content) {
        return new SurveyOption(choiceId, content);
    }
}
