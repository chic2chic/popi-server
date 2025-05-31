package com.lgcns.kafka.message;

import com.lgcns.dto.request.SurveyChoiceRequest;
import java.util.List;

public record MemberAnswerMessage(Long memberId, List<SurveyChoiceRequest> surveyChoices) {
    public static MemberAnswerMessage of(Long memberId, List<SurveyChoiceRequest> surveyChoices) {
        return new MemberAnswerMessage(memberId, surveyChoices);
    }
}
