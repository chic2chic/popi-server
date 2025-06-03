package com.lgcns.repository;

import com.lgcns.dto.FlatPaymentItem;
import com.lgcns.dto.response.AverageAmountResponse;
import com.lgcns.dto.response.ItemBuyerCountResponse;
import java.util.List;
import org.springframework.data.domain.Slice;

public interface PaymentRepositoryCustom {
    List<ItemBuyerCountResponse> countItemBuyerByPopupId(Long popupId);

    AverageAmountResponse findAverageAmountByPopupId(Long popupId);

    Slice<FlatPaymentItem> findAllPaymentHistoryByMemberId(
            Long memberId, Long lastPaymentId, int size);
}
