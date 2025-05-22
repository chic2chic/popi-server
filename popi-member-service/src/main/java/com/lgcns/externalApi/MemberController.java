package com.lgcns.externalApi;

import com.lgcns.dto.response.MemberInfoResponse;
import com.lgcns.service.MemberService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@Tag(name = "회원 서버 API", description = "회원 서버 API입니다.")
public class MemberController {

    private final MemberService memberService;

    @GetMapping("/me")
    @Operation(summary = "회원 정보 조회", description = "로그인한 회원 정보를 조회합니다.")
    public MemberInfoResponse memberInfoFind(@RequestHeader("member-id") String memberId) {
        return memberService.findMemberInfo(memberId);
    }
}
