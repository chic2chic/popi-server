package com.lgcns.externalApi;

import com.lgcns.dto.response.PopupDetailsResponse;
import com.lgcns.dto.response.PopupInfoResponse;
import com.lgcns.response.SliceResponse;
import com.lgcns.service.PopupService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@Tag(name = "팝업 API", description = "팝업 스토어 관련 API 입니다.")
public class PopupController {

    private final PopupService popupService;

    @GetMapping
    @Operation(summary = "팝업 목록 조회", description = "현재 운영중인 모든 팝업을 무한 스크롤을 위하여 페이징 처리한 뒤 반환합니다.")
    public SliceResponse<PopupInfoResponse> popupFindAll(
            @Parameter(description = "검색할 팝업 이름 (비워두면 모든 팝업을 반환합니다.)", example = "black")
                    @RequestParam(name = "keyword", required = false)
                    String keyword,
            @Parameter(description = "이전 페이지의 마지막 ID (첫 요청 시 비워두세요.)", example = "1")
                    @RequestParam(required = false)
                    Long lastPopupId,
            @Parameter(description = "페이지 크기 (기본 8)", example = "8")
                    @RequestParam(defaultValue = "8")
                    int size) {
        return popupService.findPopupsByName(keyword, lastPopupId, size);
    }

    @GetMapping("/{popupId}")
    @Operation(summary = "팝업 상세 조회", description = "팝업에 대한 상세 정보를 반환합니다.")
    public PopupDetailsResponse popupDetailsFindById(@PathVariable(name = "popupId") Long popupId) {
        return popupService.findPopupDetailsById(popupId);
    }

    @GetMapping("/popularity")
    @Operation(summary = "인기 팝업 목록 조회", description = "예약자 수가 많은 상위 4개의 팝업 리스트를 반환합니다.")
    public List<PopupInfoResponse> hotPopupsFind() {
        return popupService.findHotPopups();
    }
}
