package com.lgcns.externalApi;

import com.lgcns.dto.popup.response.PopupInfoResponse;
import com.lgcns.response.SliceResponse;
import com.lgcns.service.popup.PopupService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@Tag(name = "팝업 API", description = "팝업 스토어 관련 API 입니다.")
public class PopupController {

    private final PopupService popupService;

    @GetMapping("/popups")
    @Operation(summary = "팝업 목록 조회", description = "현재 운영중인 모든 팝업을 무한 스크롤을 위하여 페이징 처리한 뒤 반환합니다.")
    public SliceResponse<PopupInfoResponse> popupFindAll(
            @Parameter(description = "검색할 팝업 이름 (비워두면 모든 팝업을 반환합니다.)", example = "black")
                    @RequestParam(name = "keyWord", required = false)
                    String keyWord,
            @Parameter(description = "이전 페이지의 마지막 ID (첫 요청 시 비워두세요.)", example = "1")
                    @RequestParam(required = false)
                    Long lastPopupId,
            @Parameter(description = "페이지 크기 (기본 8)", example = "8")
                    @RequestParam(defaultValue = "8")
                    int size) {
        return popupService.findPopupsByName(keyWord, lastPopupId, size);
    }
}
