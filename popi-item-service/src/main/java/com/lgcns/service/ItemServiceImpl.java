package com.lgcns.service;

import com.lgcns.client.ManagerServiceClient;
import com.lgcns.dto.response.ItemInfoResponse;
import com.lgcns.response.SliceResponse;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ItemServiceImpl implements ItemService {

    private final ManagerServiceClient managerServiceClient;

    @Override
    public SliceResponse<ItemInfoResponse> findItemsByName(
            Long popupId, String keyword, Long lastItemId, int size) {
        return managerServiceClient.findItemsByName(popupId, keyword, lastItemId, size);
    }

    @Override
    public List<ItemInfoResponse> findItemsDefault(Long popupId) {
        return managerServiceClient.findItemsDefault(popupId);
    }

    @Override
    public List<ItemInfoResponse> findItemsPopularity(Long popupId) {
        return managerServiceClient.findItemsPopularity(popupId);
    }
}
