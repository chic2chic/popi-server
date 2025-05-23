package com.lgcns.client;

import com.lgcns.config.FeignConfig;
import com.lgcns.dto.request.MemberInternalRegisterRequest;
import com.lgcns.dto.response.MemberInternalRegisterResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;

@FeignClient(name = "members", configuration = FeignConfig.class)
public interface MemberServiceClient {

    @PostMapping("/internal/register")
    MemberInternalRegisterResponse registerMember(MemberInternalRegisterRequest request);
}
