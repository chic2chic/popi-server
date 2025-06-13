package com.lgcns.service.grpc;

import com.lgcns.error.exception.CustomException;
import com.lgcns.exception.MemberErrorCode;
import com.lgcns.service.MemberService;
import com.popi.common.grpc.member.*;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import net.devh.boot.grpc.server.service.GrpcService;

@GrpcService
@RequiredArgsConstructor
public class MemberGrpcService extends MemberServiceGrpc.MemberServiceImplBase {

    private final MemberService memberService;

    @Override
    public void registerMember(
            MemberInternalRegisterRequest request,
            StreamObserver<MemberInternalRegisterResponse> responseObserver) {
        MemberInternalRegisterResponse grpcResponse = memberService.registerMember(request);

        responseObserver.onNext(grpcResponse);
        responseObserver.onCompleted();
    }

    @Override
    public void findByOauthInfo(
            MemberInternalOauthInfoRequest request,
            StreamObserver<MemberInternalInfoResponse> responseObserver) {
        try {
            MemberInternalInfoResponse grpcResponse = memberService.findByOauthInfo(request);
            responseObserver.onNext(grpcResponse);
            responseObserver.onCompleted();
        } catch (CustomException e) {
            if (e.getErrorCode() == MemberErrorCode.MEMBER_NOT_FOUND) {
                responseObserver.onError(Status.NOT_FOUND.asRuntimeException());
            } else {
                responseObserver.onError(Status.INTERNAL.asRuntimeException());
            }
        }
    }

    @Override
    public void findByMemberId(
            MemberInternalIdRequest request,
            StreamObserver<MemberInternalInfoResponse> responseObserver) {
        MemberInternalInfoResponse grpcResponse = memberService.findByMemberId(request);

        responseObserver.onNext(grpcResponse);
        responseObserver.onCompleted();
    }
}
