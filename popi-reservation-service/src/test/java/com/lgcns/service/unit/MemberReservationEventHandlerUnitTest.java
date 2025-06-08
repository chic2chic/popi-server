package com.lgcns.service.unit;

import static org.mockito.Mockito.*;

import com.lgcns.error.exception.CustomException;
import com.lgcns.event.dto.MemberReservationNotificationEvent;
import com.lgcns.event.dto.MemberReservationUpdateEvent;
import com.lgcns.event.handler.MemberReservationEventHandler;
import com.lgcns.exception.MemberReservationErrorCode;
import com.lgcns.kafka.message.MemberEnteredMessage;
import com.lgcns.kafka.producer.MemberEnteredProducer;
import com.lgcns.service.MemberReservationService;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class MemberReservationEventHandlerUnitTest {
    @InjectMocks private MemberReservationEventHandler eventHandler;

    @Mock private MemberReservationService memberReservationService;
    @Mock private MemberEnteredProducer memberEnteredProducer;

    @Nested
    class 회원예약_알림_이벤트_처리할_때 {

        @Test
        void 정상적으로_회원예약_서비스에서_회원예약_알림을_생성한다() {
            // given
            MemberReservationNotificationEvent event =
                    mock(MemberReservationNotificationEvent.class);

            // when
            eventHandler.handleMemberReservationNotificationEvent(event);

            // then
            verify(memberReservationService, times(1)).createReservationNotification(event);
        }
    }

    @Nested
    class 회원입장_이벤트를_처리할_때 {

        @Test
        void 정상적으로_회원입장_프로듀서에서_회원입장_메시지를_전송한다() {
            // given
            MemberEnteredMessage message = mock(MemberEnteredMessage.class);

            // when
            eventHandler.handleMemberEnteredEvent(message);

            // then
            verify(memberEnteredProducer).sendMessage(message);
        }
    }

    @Nested
    class 회원예약_업데이트_이벤트를_처리할_때 {

        @Test
        void 정상적으로_회원예약_서비스에서_회원예약_업데이트를_처리한다() {
            // given
            MemberReservationUpdateEvent event = mock(MemberReservationUpdateEvent.class);

            // when
            eventHandler.handleMemberReservationUpdateEvent(event);

            // then
            verify(memberReservationService, times(1)).updateMemberReservation(anyLong());
        }

        @Test
        void 회원예약이_존재하지_않을_경우_예외를_던진다() {
            // given
            MemberReservationUpdateEvent event = mock(MemberReservationUpdateEvent.class);

            doThrow(new CustomException(MemberReservationErrorCode.MEMBER_RESERVATION_NOT_FOUND))
                    .when(memberReservationService)
                    .updateMemberReservation(anyLong());

            // when
            eventHandler.handleMemberReservationUpdateEvent(event);

            // then
            verify(memberReservationService, times(1)).updateMemberReservation(anyLong());
            verify(memberReservationService, never()).cancelMemberReservation(any());
        }
    }
}
