package com.lgcns.client;

import com.popi.common.grpc.auth.AuthServiceGrpc;
import com.popi.common.grpc.auth.RefreshTokenDeleteRequest;
import com.popi.common.grpc.member.*;
import io.grpc.Channel;
import lombok.RequiredArgsConstructor;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AuthGrpcClient {

    @GrpcClient("auth-service")
    private Channel channel;

    private AuthServiceGrpc.AuthServiceBlockingStub stub() {
        return AuthServiceGrpc.newBlockingStub(channel);
    }

    public void deleteRefreshToken(RefreshTokenDeleteRequest request) {
        stub().deleteRefreshToken(request);
    }
}
