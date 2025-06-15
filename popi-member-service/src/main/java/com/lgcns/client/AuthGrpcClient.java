package com.lgcns.client;

import com.popi.common.grpc.auth.AuthServiceGrpc;
import com.popi.common.grpc.auth.RefreshTokenDeleteRequest;
import com.popi.common.grpc.member.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AuthGrpcClient {

    private final AuthServiceGrpc.AuthServiceBlockingStub authServiceBlockingStub;

    public void deleteRefreshToken(RefreshTokenDeleteRequest request) {
        authServiceBlockingStub.deleteRefreshToken(request);
    }
}
