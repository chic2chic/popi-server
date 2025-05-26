package com.lgcns.service;

import com.lgcns.client.ManagerServiceClient;
import com.lgcns.dto.response.ItemInfoResponse;
import com.lgcns.response.SliceResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ItemServiceImpl implements ItemService {

    private final ManagerServiceClient managerServiceClient;

    @Override
    public SliceResponse<ItemInfoResponse> findItemsByName(
            Long popupId, String searchName, Long lastItemId, int size) {
        return managerServiceClient.findItemsByName(popupId, searchName, lastItemId, size);
    }
}
