package com.lgcns.externalApi;

import com.lgcns.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@Tag(name = "알림 서버 API", description = "알림 서버 API 입니다.")
public class NotificationController {

    private final NotificationService notificationService;

    @PostMapping("/fcm/register")
    @Operation(summary = "FCM 토큰 등록", description = "사용자 디바이스의 FCM 토큰을 등록합니다.")
    public ResponseEntity<Void> fcmTokenCreate(
            @RequestHeader("member-id") Long memberId,
            @RequestParam(name = "fcmToken") String fcmToken) {
        notificationService.saveFcmToken(memberId, fcmToken);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }
}
