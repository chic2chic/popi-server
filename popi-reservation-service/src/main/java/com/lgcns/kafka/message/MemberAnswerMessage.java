package com.lgcns.kafka.message;

import com.lgcns.dto.request.SurveyChoiceRequest;
import com.lgcns.kafka.message.dto.SurveyChoiceDto;
import java.util.List;

public record MemberAnswerMessage(Long memberId, List<SurveyChoiceDto> surveyChoices) {
    public static MemberAnswerMessage of(Long memberId, List<SurveyChoiceRequest> requests) {
        List<SurveyChoiceDto> surveyChoices =
                requests.stream().map(r -> SurveyChoiceDto.of(r.surveyId(), r.choiceId())).toList();
        return new MemberAnswerMessage(memberId, surveyChoices);
    }
}
