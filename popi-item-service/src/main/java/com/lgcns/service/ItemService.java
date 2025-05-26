package com.lgcns.service;

import com.lgcns.dto.response.ItemInfoResponse;
import com.lgcns.response.SliceResponse;
import java.util.List;

public interface ItemService {
    SliceResponse<ItemInfoResponse> findItemsByName(
            Long popupId, String keyword, Long lastItemId, int size);

    List<ItemInfoResponse> findItemsDefault(Long popupId);
}
