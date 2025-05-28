package com.lgcns.externalApi;

import com.lgcns.dto.request.PaymentReadyRequest;
import com.lgcns.dto.response.PaymentReadyResponse;
import com.lgcns.service.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@Tag(name = "결제 서버 API", description = "결제 서버 API입니다.")
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping("/ready")
    @Operation(
            summary = "결제 준비 정보 생성",
            description = "사용자가 장바구니에서 선택한 상품 정보를 기반으로 결제에 필요한 정보를 생성합니다.")
    public PaymentReadyResponse paymentPrepare(
            @RequestHeader("member-id") String memberId, @RequestBody PaymentReadyRequest request) {
        return paymentService.preparePayment(memberId, request);
    }
}
