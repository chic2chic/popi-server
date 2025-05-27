package com.lgcns.repository;

import static com.lgcns.domain.QFcmDevice.fcmDevice;

import com.lgcns.domain.FcmDevice;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class FcmDeviceRepositoryImpl implements FcmDeviceRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    @Override
    public List<FcmDevice> findFcmSendList(List<Long> memberIdList) {
        return queryFactory
                .selectFrom(fcmDevice)
                .where(fcmDevice.memberId.in(memberIdList))
                .orderBy(fcmDevice.memberId.asc())
                .fetch();
    }
}
