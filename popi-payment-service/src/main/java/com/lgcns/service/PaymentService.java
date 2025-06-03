package com.lgcns.service;

import com.lgcns.dto.request.PaymentReadyRequest;
import com.lgcns.dto.response.ItemBuyerCountResponse;
import com.lgcns.dto.response.PaymentReadyResponse;
import com.siot.IamportRestClient.exception.IamportResponseException;
import java.io.IOException;
import java.util.List;

public interface PaymentService {
    PaymentReadyResponse preparePayment(String memberId, PaymentReadyRequest request);

    void findPaymentByImpUid(String impUid) throws IamportResponseException, IOException;

    List<ItemBuyerCountResponse> countItemBuyerByPopupId(Long popupId);
}
