package com.lgcns.domain;

import com.lgcns.entity.BaseTimeEntity;
import com.lgcns.error.exception.CustomException;
import com.lgcns.exception.MemberErrorCode;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Member extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "member_id")
    private Long id;

    private String nickname;

    @Embedded private OauthInfo oauthInfo;

    @Enumerated(EnumType.STRING)
    private MemberAge age;

    @Enumerated(EnumType.STRING)
    private MemberGender gender;

    @Enumerated(EnumType.STRING)
    private MemberStatus status;

    @Enumerated(EnumType.STRING)
    private MemberRole role;

    @Builder(access = AccessLevel.PRIVATE)
    private Member(
            String nickname,
            OauthInfo oauthInfo,
            MemberAge age,
            MemberGender gender,
            MemberStatus status,
            MemberRole role) {
        this.nickname = nickname;
        this.gender = gender;
        this.age = age;
        this.oauthInfo = oauthInfo;
        this.status = status;
        this.role = role;
    }

    public static Member createMember(String nickname, OauthInfo oauthInfo) {
        return Member.builder()
                .nickname(nickname)
                .oauthInfo(oauthInfo)
                .status(MemberStatus.NORMAL)
                .role(MemberRole.USER)
                .build();
    }

    public void withdrawal() {
        if (this.status == MemberStatus.DELETED) {
            throw new CustomException(MemberErrorCode.MEMBER_ALREADY_DELETED);
        }

        this.status = MemberStatus.DELETED;
    }
}
