package com.lgcns.externalApi;

import com.google.api.core.ApiFuture;
import com.lgcns.dto.request.FcmRequest;
import com.lgcns.service.FcmService;
import com.lgcns.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.concurrent.ExecutionException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@Tag(name = "알림 서버 API", description = "알림 서버 API 입니다.")
@Slf4j
public class NotificationController {

    private final NotificationService notificationService;
    private final FcmService fcmService;

    @PostMapping("/fcm/register")
    @Operation(summary = "FCM 토큰 등록", description = "사용자 디바이스의 FCM 토큰을 등록합니다.")
    public ResponseEntity<Void> fcmTokenCreate(
            @RequestHeader("member-id") String memberId,
            @Valid @RequestBody FcmRequest fcmRequest) {
        notificationService.saveFcmToken(memberId, fcmRequest.fcmToken());
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @PostMapping("/test")
    public ResponseEntity<String> test(@RequestParam(name = "fcmToken") String fcmToken)
            throws ExecutionException, InterruptedException {
        try {
            ApiFuture<String> message = fcmService.sendMessageSync(fcmToken);
            return ResponseEntity.status(HttpStatus.CREATED).body(message.get());
        } catch (ExecutionException e) {
            log.error("ExecutionException 발생: {}", e.getCause().getMessage(), e.getCause());
            throw e; // 또는 예외 래핑해서 throw
        }
    }
}
