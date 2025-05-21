package com.lgcns.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
public class SwaggerConfig {

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(
                        new Info()
                                .title("PoPI Popup Service API")
                                .description("PoPI 팝업 서비스 API 명세서입니다.")
                                .version("v0.0.1"))
                .addServersItem(new Server().url("/popups"));
    }
}
