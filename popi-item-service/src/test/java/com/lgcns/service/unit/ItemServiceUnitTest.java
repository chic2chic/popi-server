package com.lgcns.service.unit;

import com.lgcns.client.ManagerServiceClient;
import com.lgcns.service.ItemServiceImpl;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class ItemServiceUnitTest {

    @InjectMocks private ItemServiceImpl itemService;

    @Mock private ManagerServiceClient managerServiceClient;
}
