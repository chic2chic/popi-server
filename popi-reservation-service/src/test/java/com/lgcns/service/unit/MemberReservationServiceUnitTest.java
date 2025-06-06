package com.lgcns.service.unit;

import com.lgcns.client.managerClient.ManagerServiceClient;
import com.lgcns.client.memberClient.MemberServiceClient;
import com.lgcns.kafka.producer.MemberAnswerProducer;
import com.lgcns.repository.MemberReservationRepository;
import com.lgcns.service.MemberReservationServiceImpl;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;

@ExtendWith(MockitoExtension.class)
public class MemberReservationServiceUnitTest {

    @InjectMocks MemberReservationServiceImpl memberReservationService;

    @Mock MemberReservationRepository memberReservationRepository;

    @Mock ManagerServiceClient managerServiceClient;

    @Mock MemberServiceClient memberServiceClient;

    @Mock private RedisTemplate<String, Long> reservationRedisTemplate;

    @Mock private RedisTemplate<String, String> notificationRedisTemplate;

    @Mock MemberAnswerProducer memberAnswerProducer;

    // TODO 에약 가능한 날짜 조회할 때

    // TODO 설문지 조회할 때

    // TODO 설문지 등록할 때

    // TODO 예약 생성할 때

    // TODO 예약 업데이트할 때

    // TODO 예약 취소할 때

    // TODO 예약 목록 조회할 때

    // TODO 인기 팝업 조회할 때

    // TODO 가장 가까운 예약 조회할 때

    // TODO 오늘 예약자 수 조회할 때

    // TODO 회원이 팝업 입장할 때

}
