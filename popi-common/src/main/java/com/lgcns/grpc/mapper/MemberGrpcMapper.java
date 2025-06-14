package com.lgcns.grpc.mapper;

import com.popi.common.grpc.member.MemberAge;
import com.popi.common.grpc.member.MemberGender;
import com.popi.common.grpc.member.MemberRole;
import com.popi.common.grpc.member.MemberStatus;

public class MemberGrpcMapper {

    public static MemberAge toGrpcMemberAge(com.lgcns.enums.MemberAge age) {
        return MemberAge.valueOf(age.name());
    }

    public static com.lgcns.enums.MemberAge toDomainMemberAge(MemberAge grpcAge) {
        return com.lgcns.enums.MemberAge.valueOf(grpcAge.name());
    }

    public static MemberGender toGrpcMemberGender(com.lgcns.enums.MemberGender gender) {
        return MemberGender.valueOf(gender.name());
    }

    public static com.lgcns.enums.MemberGender toDomainMemberGender(MemberGender grpcGender) {
        return com.lgcns.enums.MemberGender.valueOf(grpcGender.name());
    }

    public static MemberRole toGrpcMemberRole(com.lgcns.enums.MemberRole role) {
        return MemberRole.valueOf(role.name());
    }

    public static com.lgcns.enums.MemberRole toDomainMemberRole(MemberRole grpcRole) {
        return com.lgcns.enums.MemberRole.valueOf(grpcRole.name());
    }

    public static MemberStatus toGrpcMemberStatus(com.lgcns.enums.MemberStatus status) {
        return MemberStatus.valueOf(status.name());
    }

    public static com.lgcns.enums.MemberStatus toDomainMemberStatus(MemberStatus grpcStatus) {
        return com.lgcns.enums.MemberStatus.valueOf(grpcStatus.name());
    }
}
