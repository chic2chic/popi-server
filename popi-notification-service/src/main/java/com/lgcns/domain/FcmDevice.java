package com.lgcns.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class FcmDevice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "fcm_token_id")
    private Long id;

    private Long memberId;
    private String token;

    @Builder(access = AccessLevel.PRIVATE)
    private FcmDevice(Long memberId, String token) {
        this.memberId = memberId;
        this.token = token;
    }

    public static FcmDevice of(Long memberId, String token) {
        return FcmDevice.builder().memberId(memberId).token(token).build();
    }
}
