package com.lgcns.service;

import com.lgcns.client.ReservationServiceClient;
import com.lgcns.client.dto.UpcomingReservationResponse;
import com.lgcns.domain.FcmDevice;
import com.lgcns.repository.FcmDeviceRepository;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class NotificationServiceImpl implements NotificationService {

    private final FcmDeviceRepository fcmDeviceRepository;
    private final ReservationServiceClient reservationServiceClient;

    @Override
    public List<FcmDevice> findFcmSendList() {
        List<UpcomingReservationResponse> response =
                reservationServiceClient.findUpcomingReservations();

        List<Long> memberIdList = getMemberIdList(response);

        return fcmDeviceRepository.findFcmSendList(memberIdList);
    }

    private List<Long> getMemberIdList(List<UpcomingReservationResponse> response) {
        return response.stream()
                .map(UpcomingReservationResponse::memberId)
                .collect(Collectors.toList());
    }
}
