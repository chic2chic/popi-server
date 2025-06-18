package com.lgcns.service.integration;

import com.popi.common.grpc.member.*;
import io.grpc.stub.StreamObserver;

public class TestMemberGrpcService extends MemberServiceGrpc.MemberServiceImplBase {

    @Override
    public void findByMemberId(
            MemberInternalIdRequest request,
            StreamObserver<MemberInternalInfoResponse> responseObserver) {
        MemberInternalInfoResponse response =
                MemberInternalInfoResponse.newBuilder()
                        .setMemberId(1L)
                        .setNickname("testNickname")
                        .setAge(MemberAge.TEENAGER)
                        .setGender(MemberGender.MALE)
                        .setRole(MemberRole.USER)
                        .setStatus(MemberStatus.NORMAL)
                        .build();
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }
}
