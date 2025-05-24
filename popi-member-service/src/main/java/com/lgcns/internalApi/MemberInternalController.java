package com.lgcns.internalApi;

import com.lgcns.dto.request.MemberInternalRegisterRequest;
import com.lgcns.dto.request.MemberOauthInfoRequest;
import com.lgcns.dto.response.MemberInternalInfoResponse;
import com.lgcns.dto.response.MemberInternalRegisterResponse;
import com.lgcns.service.MemberService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/internal")
public class MemberInternalController {

    private final MemberService memberService;

    @PostMapping("/register")
    public MemberInternalRegisterResponse registerMember(
            @RequestBody @Valid MemberInternalRegisterRequest request) {
        return memberService.registerMember(request);
    }

    @PostMapping("/oauth-info")
    public MemberInternalInfoResponse findOauthInfo(@RequestBody MemberOauthInfoRequest request) {
        return memberService.findOauthInfo(request);
    }

    @GetMapping("/{memberId}")
    public MemberInternalInfoResponse findMemberId(@PathVariable Long memberId) {
        return memberService.findMemberId(memberId);
    }
}
