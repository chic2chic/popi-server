package com.lgcns.service.item;

import com.lgcns.dto.item.response.ItemInfoResponse;
import com.lgcns.response.SliceResponse;

public interface ItemService {
    SliceResponse<ItemInfoResponse> findItemsByName(
            Long popupId, String searchName, Long lastItemId, int size);
}
