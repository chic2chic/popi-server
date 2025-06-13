package com.lgcns.service.grpc;

import com.lgcns.service.MemberService;
import com.popi.common.grpc.member.MemberInternalRegisterRequest;
import com.popi.common.grpc.member.MemberInternalRegisterResponse;
import com.popi.common.grpc.member.MemberServiceGrpc;
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
}
