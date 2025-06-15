package com.lgcns.service.grpc;

import com.google.protobuf.Empty;
import com.lgcns.service.AuthService;
import com.popi.common.grpc.auth.AuthServiceGrpc;
import com.popi.common.grpc.auth.RefreshTokenDeleteRequest;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import net.devh.boot.grpc.server.service.GrpcService;

@GrpcService
@RequiredArgsConstructor
public class AuthGrpcService extends AuthServiceGrpc.AuthServiceImplBase {

    private final AuthService authService;

    @Override
    public void deleteRefreshToken(
            RefreshTokenDeleteRequest request, StreamObserver<Empty> responseObserver) {
        authService.deleteRefreshToken(request);
        responseObserver.onNext(Empty.getDefaultInstance());
        responseObserver.onCompleted();
    }
}
