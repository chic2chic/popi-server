package com.lgcns.config;

import com.popi.common.grpc.member.MemberServiceGrpc;
import io.grpc.Channel;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile("!test")
public class GrpcClientConfig {

    @GrpcClient("member-service")
    private Channel channel;

    @Bean
    public MemberServiceGrpc.MemberServiceBlockingStub memberServiceBlockingStub() {
        return MemberServiceGrpc.newBlockingStub(channel);
    }
}
