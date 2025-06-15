package com.lgcns.service.integration;

import com.google.protobuf.Empty;
import com.popi.common.grpc.member.*;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;

public class TestMemberGrpcService extends MemberServiceGrpc.MemberServiceImplBase {

    @Override
    public void registerMember(
            MemberInternalRegisterRequest request,
            StreamObserver<MemberInternalRegisterResponse> responseObserver) {
        if (request.getOauthId().equals("alreadyRegisteredOauthId")) {
            responseObserver.onError(Status.ALREADY_EXISTS.asRuntimeException());
            return;
        }

        MemberInternalRegisterResponse response =
                MemberInternalRegisterResponse.newBuilder()
                        .setMemberId(1L)
                        .setRole(MemberRole.USER)
                        .build();
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    @Override
    public void findByOauthInfo(
            MemberInternalOauthInfoRequest request,
            StreamObserver<MemberInternalInfoResponse> responseObserver) {
        String oauthId = request.getOauthId();

        if (oauthId.equals("not-registered-oauthId")) {
            responseObserver.onError(Status.NOT_FOUND.asRuntimeException());
            return;
        }

        MemberStatus status = MemberStatus.NORMAL;

        if (oauthId.equals("deleted-oauthId")) {
            status = MemberStatus.DELETED;
        }

        MemberInternalInfoResponse response =
                MemberInternalInfoResponse.newBuilder()
                        .setMemberId(1L)
                        .setNickname("testNickname")
                        .setAge(MemberAge.TEENAGER)
                        .setGender(MemberGender.MALE)
                        .setRole(MemberRole.USER)
                        .setStatus(status)
                        .build();
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

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

    @Override
    public void rejoinMember(
            MemberInternalIdRequest request, StreamObserver<Empty> responseObserver) {
        responseObserver.onNext(Empty.getDefaultInstance());
        responseObserver.onCompleted();
    }
}
