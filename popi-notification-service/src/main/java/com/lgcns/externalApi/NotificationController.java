package com.lgcns.externalApi;

import com.lgcns.dto.request.FcmRequest;
import com.lgcns.infra.firebase.FcmSender;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@Tag(name = "알림 서버 테스트 API", description = "알림 서버 테스트 API입니다.")
public class NotificationController {

    private final FcmSender fcmSender;

    @PostMapping("/fcm")
    @Operation(summary = "fcm 테스트", description = "fcm 메세지 생성을 테스트합니다.")
    public ResponseEntity<Void> sendFcmTest(@RequestBody FcmRequest fcmRequest) {
        fcmSender.sendFcm(fcmRequest);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }
}
