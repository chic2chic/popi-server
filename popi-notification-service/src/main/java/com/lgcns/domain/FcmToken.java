package com.lgcns.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class FcmToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "fcm_token_id")
    private Long id;

    private String username;
    private String token;

    @Builder(access = AccessLevel.PRIVATE)
    private FcmToken(String username, String token) {
        this.username = username;
        this.token = token;
    }

    public static FcmToken of(String username, String token) {
        return FcmToken.builder().username(username).token(token).build();
    }
}
