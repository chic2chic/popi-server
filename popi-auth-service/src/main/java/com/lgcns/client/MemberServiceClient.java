package com.lgcns.client;

import com.lgcns.config.FeignConfig;
import com.lgcns.dto.request.MemberOauthInfoRequest;
import com.lgcns.dto.response.MemberInternalInfoResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

@FeignClient(
        name = "${member.service.name}",
        url = "${member.service.url:}",
        configuration = FeignConfig.class)
public interface MemberServiceClient {

    @PostMapping("/internal/oauth-info")
    MemberInternalInfoResponse findByOauthInfo(@RequestBody MemberOauthInfoRequest request);

    @GetMapping("/internal/{memberId}")
    MemberInternalInfoResponse findByMemberId(@PathVariable Long memberId);

    @PostMapping("/internal/{memberId}/rejoin")
    void rejoinMember(@PathVariable Long memberId);
}
