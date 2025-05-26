package com.lgcns.externalApi;

import com.lgcns.dto.response.ItemInfoResponse;
import com.lgcns.response.SliceResponse;
import com.lgcns.service.ItemService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/{popupId}")
@Tag(name = "2. 상품 API", description = "상품 관련 API 입니다.")
public class ItemController {

    private final ItemService itemService;

    @GetMapping
    @Operation(
            summary = "상품 목록 조회",
            description =
                    "keyword를 포함하지 않으면 현재 팝업의 모든 상품을 무한 스크롤을 위하여 페이징 처리한 뒤 반환합니다.</br>"
                            + "keyword를 포함하여 호출하면 상품명에 keyword가 포함된 모든 상품을 페이징 처리한 뒤 반환합니다.")
    public SliceResponse<ItemInfoResponse> userItemFindByName(
            @Parameter(description = "팝업 ID", example = "1") @PathVariable(name = "popupId")
                    Long popupId,
            @Parameter(description = "검색할 상품 이름 (비워두면 모든 상품을 반환합니다.)", example = "포토카드")
                    @RequestParam(name = "keyword", required = false)
                    String keyword,
            @Parameter(description = "이전 페이지의 마지막 ID (첫 요청 시 비워두세요.)", example = "2")
                    @RequestParam(name = "lastItemId", required = false)
                    Long lastItemId,
            @Parameter(description = "페이지 크기 (기본 8)", example = "8")
                    @RequestParam(name = "size", defaultValue = "8")
                    int size) {
        return itemService.findItemsByName(popupId, keyword, lastItemId, size);
    }
}
