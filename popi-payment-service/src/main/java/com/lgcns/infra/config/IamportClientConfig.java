package com.lgcns.infra.config;

import com.lgcns.infra.iamport.IamportProperties;
import com.siot.IamportRestClient.IamportClient;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
public class IamportClientConfig {

    private final IamportProperties iamportProperties;

    @Bean
    public IamportClient iamportClient() {
        return new IamportClient(iamportProperties.key(), iamportProperties.secret());
    }
}
