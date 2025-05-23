package com.lgcns.client;

import com.lgcns.config.FeignConfig;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "auth", configuration = FeignConfig.class)
public interface AuthServiceClient {

    @DeleteMapping("/internal//{memberId}/refresh-token")
    void deleteRefreshToken(@PathVariable String memberId);
}
