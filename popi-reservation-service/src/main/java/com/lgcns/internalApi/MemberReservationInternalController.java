package com.lgcns.internalApi;

import com.lgcns.service.MemberReservationService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/internal")
public class MemberReservationInternalController {

    private final MemberReservationService memberReservationService;

    @GetMapping("/popups/popularity")
    public List<Long> findHotPopups() {
        return memberReservationService.findHotPopupIds();
    }
}
