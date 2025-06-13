package com.lgcns.client;

import com.popi.common.grpc.member.*;
import io.grpc.Channel;
import lombok.RequiredArgsConstructor;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class MemberGrpcClient {

    @GrpcClient("member-service")
    private Channel channel;

    private MemberServiceGrpc.MemberServiceBlockingStub stub() {
        return MemberServiceGrpc.newBlockingStub(channel);
    }

    public MemberInternalRegisterResponse registerMember(MemberInternalRegisterRequest request) {
        return stub().registerMember(request);
    }

    public MemberInternalInfoResponse findByOauthInfo(MemberInternalOauthInfoRequest request) {
        return stub().findByOauthInfo(request);
    }

    public MemberInternalInfoResponse findByMemberId(MemberInternalIdRequest request) {
        return stub().findByMemberId(request);
    }
}
