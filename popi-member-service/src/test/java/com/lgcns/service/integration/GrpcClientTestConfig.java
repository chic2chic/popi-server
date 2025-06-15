package com.lgcns.service.integration;

import static com.lgcns.service.integration.GrpcTestConstants.SERVER_NAME;

import com.popi.common.grpc.auth.AuthServiceGrpc;
import io.grpc.ManagedChannel;
import io.grpc.inprocess.InProcessChannelBuilder;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

@TestConfiguration
public class GrpcClientTestConfig {

    @Bean
    public AuthServiceGrpc.AuthServiceBlockingStub authServiceBlockingStub() {
        ManagedChannel channel =
                InProcessChannelBuilder.forName(SERVER_NAME)
                        .usePlaintext()
                        .directExecutor()
                        .build();

        return AuthServiceGrpc.newBlockingStub(channel);
    }
}
