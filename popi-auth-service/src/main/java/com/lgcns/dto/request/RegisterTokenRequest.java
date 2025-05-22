package com.lgcns.dto.request;

import com.lgcns.enums.MemberAge;
import com.lgcns.enums.MemberGender;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record RegisterTokenRequest(
        @Schema(description = "사용자 닉네임", example = "nickname")
                @NotBlank(message = "닉네임은 비워둘 수 없습니다.")
                String nickname,
        @Schema(description = "연령대", example = "TWENTIES") @NotNull(message = "연령대는 비워둘 수 없습니다.")
                MemberAge age,
        @Schema(description = "성별", example = "MALE") @NotNull(message = "성별은 비워둘 수 없습니다.")
                MemberGender gender) {}
