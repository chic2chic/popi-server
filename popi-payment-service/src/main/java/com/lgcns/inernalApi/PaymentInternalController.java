package com.lgcns.inernalApi;

import com.lgcns.dto.response.AverageAmountResponse;
import com.lgcns.dto.response.ItemBuyerCountResponse;
import com.lgcns.service.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal")
@RequiredArgsConstructor
@Tag(name = "결제 서버 Internal API", description = "결제 서버 Internal API입니다.")
public class PaymentInternalController {

    private final PaymentService paymentService;

    @GetMapping("/{popupId}/buyer-counts")
    @Operation(summary = "상품별 구매자 수 조회", description = "해당 팝업에서 상품별로 중복되지 않는 구매자 수를 조회합니다.")
    public List<ItemBuyerCountResponse> countItemBuyerByPopupId(@PathVariable Long popupId) {
        return paymentService.countItemBuyerByPopupId(popupId);
    }

    @GetMapping("/{popupId}/average-purchase")
    @Operation(summary = "1인당 평균 구매액 조회", description = "팝업의 총 평균 구매액과 오늘의 평균 구매액을 조회합니다.")
    public AverageAmountResponse averageAmountFind(@PathVariable Long popupId) {
        return paymentService.findAverageAmount(popupId);
    }
}
