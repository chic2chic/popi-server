package com.lgcns.repository;

import com.lgcns.domain.FcmDevice;
import java.util.List;

public interface FcmDeviceRepositoryCustom {

    List<FcmDevice> findFcmSendList(List<Long> memberIdList);
}
