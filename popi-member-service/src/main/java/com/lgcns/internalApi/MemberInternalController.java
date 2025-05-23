package com.lgcns.internalApi;

import com.lgcns.dto.request.MemberInternalRegisterRequest;
import com.lgcns.dto.response.MemberInternalRegisterResponse;
import com.lgcns.service.MemberService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
}
