package com.lgcns.config;

import com.popi.common.grpc.auth.AuthServiceGrpc;
import io.grpc.Channel;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile("!test")
public class GrpcClientConfig {

    @GrpcClient("auth-service")
    private Channel channel;

    @Bean
    public AuthServiceGrpc.AuthServiceBlockingStub authServiceBlockingStub() {
        return AuthServiceGrpc.newBlockingStub(channel);
    }
}
