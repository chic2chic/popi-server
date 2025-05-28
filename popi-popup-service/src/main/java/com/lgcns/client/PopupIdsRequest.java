package com.lgcns.client;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record PopupIdsRequest(
        @NotNull(message = "팝업 ID 리스트는 비어있을 수 없습니다.")
                @Schema(description = "상세 조회 원하는 팝업 ID 리스트", example = "[1, 2, 4]")
                List<Long> popupIds) {
    public static PopupIdsRequest of(List<Long> popupIds) {
        return new PopupIdsRequest(popupIds);
    }
}
