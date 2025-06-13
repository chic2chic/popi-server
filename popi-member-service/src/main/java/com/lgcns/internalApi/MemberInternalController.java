package com.lgcns.internalApi;

import com.lgcns.dto.request.MemberOauthInfoRequest;
import com.lgcns.dto.response.MemberInternalInfoResponse;
import com.lgcns.service.MemberService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/internal")
public class MemberInternalController {

    private final MemberService memberService;

    @PostMapping("/oauth-info")
    public MemberInternalInfoResponse findOauthInfo(@RequestBody MemberOauthInfoRequest request) {
        return memberService.findOauthInfo(request);
    }

    @GetMapping("/{memberId}")
    public MemberInternalInfoResponse findMemberId(@PathVariable Long memberId) {
        return memberService.findMemberId(memberId);
    }

    @PostMapping("/{memberId}/rejoin")
    public void rejoinMember(@PathVariable Long memberId) {
        memberService.rejoinMember(memberId);
    }
}
