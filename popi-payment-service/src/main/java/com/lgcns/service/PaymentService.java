package com.lgcns.service;

import com.lgcns.dto.request.PaymentReadyRequest;
import com.lgcns.dto.response.PaymentReadyResponse;

public interface PaymentService {
    PaymentReadyResponse preparePayment(String memberId, PaymentReadyRequest request);
}
