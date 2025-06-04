package com.lgcns.repository;

import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class FcmDeviceRepositoryImpl implements FcmDeviceRepositoryCustom {

    private final JPAQueryFactory queryFactory;
}
