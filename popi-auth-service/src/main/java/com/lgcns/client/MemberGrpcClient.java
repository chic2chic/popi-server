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

    private final MemberServiceGrpc.MemberServiceBlockingStub memberServiceBlockingStub;

    public MemberInternalRegisterResponse registerMember(MemberInternalRegisterRequest request) {
        return memberServiceBlockingStub.registerMember(request);
    }

    public MemberInternalInfoResponse findByOauthInfo(MemberInternalOauthInfoRequest request) {
        return memberServiceBlockingStub.findByOauthInfo(request);
    }

    public MemberInternalInfoResponse findByMemberId(MemberInternalIdRequest request) {
        return memberServiceBlockingStub.findByMemberId(request);
    }

    public void rejoinMember(MemberInternalIdRequest request) {
        memberServiceBlockingStub.rejoinMember(request);
    }
}
