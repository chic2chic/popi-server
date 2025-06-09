package com.lgcns.repository;

import static com.lgcns.domain.QFcmDevice.fcmDevice;

import com.querydsl.jpa.impl.JPAQueryFactory;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class FcmDeviceRepositoryImpl implements FcmDeviceRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    @Override
    public List<String> findFcmTokensByMemberIds(List<Long> memberIds) {
        return queryFactory
                .select(fcmDevice.token)
                .from(fcmDevice)
                .where(fcmDevice.memberId.in(memberIds))
                .fetch();
    }
}
