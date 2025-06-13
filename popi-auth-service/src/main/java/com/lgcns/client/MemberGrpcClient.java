package com.lgcns.client;

import com.popi.common.grpc.member.MemberInternalRegisterRequest;
import com.popi.common.grpc.member.MemberInternalRegisterResponse;
import com.popi.common.grpc.member.MemberServiceGrpc;
import io.grpc.Channel;
import lombok.RequiredArgsConstructor;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class MemberGrpcClient {

    @GrpcClient("member-service")
    private Channel channel;

    public MemberInternalRegisterResponse registerMember(MemberInternalRegisterRequest request) {
        MemberServiceGrpc.MemberServiceBlockingStub stub =
                MemberServiceGrpc.newBlockingStub(channel);

        MemberInternalRegisterRequest grpcRequest =
                MemberInternalRegisterRequest.newBuilder()
                        .setOauthId(request.getOauthId())
                        .setOauthProvider(request.getOauthProvider())
                        .setNickname(request.getNickname())
                        .setAge(request.getAge())
                        .setGender(request.getGender())
                        .build();

        return stub.registerMember(grpcRequest);
    }
}
