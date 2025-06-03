package com.lgcns.repository;

import com.lgcns.dto.response.AverageAmountResponse;
import com.lgcns.dto.response.ItemBuyerCountResponse;
import java.util.List;

public interface PaymentRepositoryCustom {
    List<ItemBuyerCountResponse> countItemBuyerByPopupId(Long popupId);

    AverageAmountResponse findAverageAmountByPopupId(Long popupId);
}
