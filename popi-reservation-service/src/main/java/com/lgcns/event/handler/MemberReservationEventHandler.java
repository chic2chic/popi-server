package com.lgcns.event.handler;

import com.lgcns.error.exception.CustomException;
import com.lgcns.event.dto.MemberReservationUpdateEvent;
import com.lgcns.exception.MemberReservationErrorCode;
import com.lgcns.kafka.message.MemberEnteredMessage;
import com.lgcns.kafka.producer.MemberEnteredProducer;
import com.lgcns.service.MemberReservationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@RequiredArgsConstructor
@Component
@Slf4j
public class MemberReservationEventHandler {

    private final MemberReservationService memberReservationService;
    private final MemberEnteredProducer memberEnteredProducer;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleMemberReservationUpdateEvent(MemberReservationUpdateEvent event) {
        tryUpdateEvent(event.memberReservationId(), event.waitTime(), 0);
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleMemberEnteredEvent(MemberEnteredMessage message) {
        memberEnteredProducer.sendMessage(message);
    }

    private void tryUpdateEvent(Long memberReservationId, long waitTime, int retryCount) {
        final long INITIAL_WAIT = waitTime == 0L ? 1000L : waitTime;

        try {
            memberReservationService.updateMemberReservation(memberReservationId);
        } catch (CustomException e) {
            if (e.getErrorCode() == MemberReservationErrorCode.MEMBER_RESERVATION_NOT_FOUND
                    || e.getErrorCode() == MemberReservationErrorCode.RESERVATION_NOT_FOUND) {
                log.info(
                        "Non-retryable error for memberReservationId: {} - {}",
                        memberReservationId,
                        e.getMessage());
                return;
            }
            handleRetry(memberReservationId, INITIAL_WAIT, retryCount);
        } catch (Exception e) {
            handleRetry(memberReservationId, INITIAL_WAIT, retryCount);
        }
    }

    private void handleRetry(Long memberReservationId, long waitTime, int retryCount) {
        final int MAX_RETRIES = 9;

        if (retryCount >= MAX_RETRIES) {
            log.error(
                    "Exceeded max retry attempts for memberReservationId: {}", memberReservationId);
            memberReservationService.cancelMemberReservation(memberReservationId);
            return;
        }

        long nextWait = waitTime * 2;

        try {
            Thread.sleep(waitTime);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            return;
        }

        log.info(
                "Retrying memberReservationId: {} (attempt #{}, next wait: {}ms)",
                memberReservationId,
                retryCount + 1,
                nextWait);
        tryUpdateEvent(memberReservationId, nextWait, retryCount + 1);
    }
}
