package com.lgcns.externalApi;

import com.lgcns.domain.FcmDevice;
import com.lgcns.repository.FcmDeviceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class NotificationController {

    private final FcmDeviceRepository fcmDeviceRepository;

    @PostMapping("/fcm-token")
    @Transactional
    public ResponseEntity<Void> fcmDeviceCreate(
            @RequestHeader("member-id") Long memberId,
            @RequestParam(name = "fcmToken") String fcmToken) {
        FcmDevice fcmDevice = FcmDevice.createFcmDevice(memberId, fcmToken);
        fcmDeviceRepository.save(fcmDevice);

        return ResponseEntity.status(HttpStatus.CREATED).build();
    }
}
