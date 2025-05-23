package com.lgcns.controller;

import com.lgcns.dto.item.response.ItemInfoResponse;
import com.lgcns.response.SliceResponse;
import com.lgcns.service.item.ItemService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/popups/{popupId}/items")
@Tag(name = "2. 상품 API", description = "상품 관련 API 입니다.")
public class ItemController {

    private final ItemService itemService;

    @GetMapping
    @Operation(summary = "상품 목록 조회", description = "현재 팝업의 모든 상품을 무한 스크롤을 위하여 페이징 처리한 뒤 반환합니다.")
    public SliceResponse<ItemInfoResponse> userItemFindAll(
            @PathVariable(name = "popupId") Long popupId,
            @RequestParam(name = "lastItemId", required = false) Long lastItemId,
            @RequestParam(name = "size", defaultValue = "8") int size) {
        return itemService.findAllItems(popupId, lastItemId, size);
    }
}
