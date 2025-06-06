package com.lgcns.repository;

import java.util.List;

public interface FcmDeviceRepositoryCustom {
    List<String> findFcmTokensByMemberIds(List<Long> memberIds);
}
