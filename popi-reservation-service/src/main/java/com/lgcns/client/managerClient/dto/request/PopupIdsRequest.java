package com.lgcns.client.managerClient.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

public record PopupIdsRequest(
        @Schema(description = "상세 조회 원하는 팝업 ID 리스트", example = "[1, 2, 4]") List<Long> popupIds) {
    public static PopupIdsRequest of(List<Long> popupIds) {
        return new PopupIdsRequest(popupIds);
    }
}
