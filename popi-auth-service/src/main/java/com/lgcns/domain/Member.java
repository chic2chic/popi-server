package com.lgcns.domain;

import com.lgcns.model.BaseTimeEntity;
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

    private String phoneNumber;

    private int age;

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
            String phoneNumber,
            int age,
            MemberGender gender,
            MemberStatus status,
            MemberRole role) {
        this.nickname = nickname;
        this.phoneNumber = phoneNumber;
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
}
