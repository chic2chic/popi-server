package com.lgcns.internalApi;

import com.lgcns.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/internal")
public class AuthInternalController {

    private final AuthService authService;

    @DeleteMapping("/{memberId}/refresh-token")
    public void deleteRefreshToken(@PathVariable String memberId) {
        authService.deleteRefreshToken(memberId);
    }
}
