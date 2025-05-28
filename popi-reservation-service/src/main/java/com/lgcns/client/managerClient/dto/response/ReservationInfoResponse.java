package com.lgcns.client.managerClient.dto.response;

import java.time.LocalDate;
import java.time.LocalTime;

public record ReservationInfoResponse(
        Long popupId, LocalDate reservationDate, LocalTime reservationTime) {}
