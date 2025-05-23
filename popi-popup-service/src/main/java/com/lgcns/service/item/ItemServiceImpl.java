package com.lgcns.service.item;

import com.lgcns.client.ManagerServiceClient;
import com.lgcns.dto.item.response.ItemInfoResponse;
import com.lgcns.response.SliceResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ItemServiceImpl implements ItemService {

    private final ManagerServiceClient managerServiceClient;

    @Override
    public SliceResponse<ItemInfoResponse> findAllItems(Long popupId, Long lastItemId, int size) {
        return managerServiceClient.findAllItems(popupId, lastItemId, size);
    }
}
