package com.lgcns.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

public record SurveyChoiceResponse(
        @Schema(description = "DB에 등록된 설문지 번호", example = "2") Long surveyId,
        @Schema(description = "선지 번호 및 내용", implementation = SurveyOption.class)
                List<SurveyOption> options) {
    public static SurveyChoiceResponse of(Long surveyId, List<SurveyOption> options) {
        return new SurveyChoiceResponse(surveyId, options);
    }
}
