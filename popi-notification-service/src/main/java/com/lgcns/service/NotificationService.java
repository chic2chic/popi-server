package com.lgcns.service;

import com.lgcns.domain.FcmDevice;
import java.util.List;

public interface NotificationService {

    List<FcmDevice> findFcmSendList();
}
