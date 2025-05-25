package com.lgcns.domain;

import com.lgcns.entity.BaseTimeEntity;
import com.lgcns.enums.MemberAge;
import com.lgcns.enums.MemberGender;
import com.lgcns.enums.MemberRole;
import com.lgcns.enums.MemberStatus;
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
            OauthInfo oauthInfo,
            String nickname,
            MemberAge age,
            MemberGender gender,
            MemberStatus status,
            MemberRole role) {
        this.oauthInfo = oauthInfo;
        this.nickname = nickname;
        this.age = age;
        this.gender = gender;
        this.status = status;
        this.role = role;
    }

    public static Member createMember(
            OauthInfo oauthInfo, String nickname, MemberGender gender, MemberAge age) {
        return Member.builder()
                .oauthInfo(oauthInfo)
                .nickname(nickname)
                .gender(gender)
                .age(age)
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

    public void reEnroll() {
        this.status = MemberStatus.NORMAL;
    }
}
