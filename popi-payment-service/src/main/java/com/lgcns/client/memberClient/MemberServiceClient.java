package com.lgcns.client.memberClient;

import com.lgcns.config.FeignConfig;
import com.lgcns.dto.response.MemberInternalInfoResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(
        name = "${member.service.name}",
        url = "${member.service.url:}",
        configuration = FeignConfig.class)
public interface MemberServiceClient {
    @GetMapping("/internal/{memberId}")
    MemberInternalInfoResponse findMemberInfo(@PathVariable Long memberId);
}
