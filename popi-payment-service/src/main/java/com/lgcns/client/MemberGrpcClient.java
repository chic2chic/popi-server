package com.lgcns.client;

import com.popi.common.grpc.member.MemberInternalIdRequest;
import com.popi.common.grpc.member.MemberInternalInfoResponse;
import com.popi.common.grpc.member.MemberServiceGrpc;
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
