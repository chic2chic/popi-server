package com.lgcns.service.integration;

import com.google.protobuf.Empty;
import com.popi.common.grpc.auth.AuthServiceGrpc;
import com.popi.common.grpc.auth.RefreshTokenDeleteRequest;
import io.grpc.stub.StreamObserver;

public class TestAuthGrpcService extends AuthServiceGrpc.AuthServiceImplBase {

    @Override
    public void deleteRefreshToken(
            RefreshTokenDeleteRequest request, StreamObserver<Empty> responseObserver) {
        responseObserver.onNext(Empty.getDefaultInstance());
        responseObserver.onCompleted();
    }
}
