package com.lgcns.client.memberClient;

import com.popi.common.grpc.member.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class MemberGrpcClient {

    private final MemberServiceGrpc.MemberServiceBlockingStub memberServiceBlockingStub;

    public MemberInternalInfoResponse findByMemberId(MemberInternalIdRequest request) {
        return memberServiceBlockingStub.findByMemberId(request);
    }
}
