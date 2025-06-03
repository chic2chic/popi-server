package com.lgcns.kafka.message.dto;

public record SurveyChoiceDto(Long surveyId, Long choiceId) {
    public static SurveyChoiceDto of(Long surveyId, Long choiceId) {
        return new SurveyChoiceDto(surveyId, choiceId);
    }
}
