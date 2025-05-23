package com.lgcns.service;

import com.lgcns.dto.response.AvailableDateResponse;

public interface MemberReservationService {
    AvailableDateResponse findAvailableDate(String memberId, Long popupId, String date);
}
