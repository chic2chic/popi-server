package com.lgcns.externalApi;

import com.lgcns.dto.request.PaymentReadyRequest;
import com.lgcns.dto.response.PaymentReadyResponse;
import com.lgcns.service.PaymentService;
import com.siot.IamportRestClient.exception.IamportResponseException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

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

    @PostMapping("/verify/{impUid}")
    @Operation(
            summary = "impUid 기반 결제 검증",
            description = "impUid에 해당하는 결제 정보를 아임포트에서 조회하고, 결제 금액과 상태를 검증합니다.")
    public void paymentByImpUidFind(@PathVariable("impUid") String impUid)
            throws IamportResponseException, IOException {
        paymentService.findPaymentByImpUid(impUid);
    }
}
