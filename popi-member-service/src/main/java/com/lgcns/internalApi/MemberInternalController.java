package com.lgcns.internalApi;

import com.lgcns.dto.response.MemberInternalInfoResponse;
import com.lgcns.service.MemberService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/internal")
public class MemberInternalController {

    private final MemberService memberService;

    @GetMapping("/{memberId}")
    public MemberInternalInfoResponse findMemberId(@PathVariable Long memberId) {
        return memberService.findMemberId(memberId);
    }

    @PostMapping("/{memberId}/rejoin")
    public void rejoinMember(@PathVariable Long memberId) {
        memberService.rejoinMember(memberId);
    }
}
