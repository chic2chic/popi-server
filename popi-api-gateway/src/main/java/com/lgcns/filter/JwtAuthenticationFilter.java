package com.lgcns.filter;

import static com.lgcns.common.constants.SecurityConstants.TOKEN_PREFIX;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lgcns.dto.AccessTokenDto;
import com.lgcns.error.ErrorResponse;
import com.lgcns.error.FailResponse;
import com.lgcns.error.exception.GatewayAuthErrorCode;
import com.lgcns.service.JwtTokenService;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
public class JwtAuthenticationFilter
        extends AbstractGatewayFilterFactory<JwtAuthenticationFilter.Config> {

    private final JwtTokenService jwtTokenService;
    private final ObjectMapper objectMapper;

    public JwtAuthenticationFilter(JwtTokenService jwtTokenService, ObjectMapper objectMapper) {
        super(Config.class);
        this.jwtTokenService = jwtTokenService;
        this.objectMapper = objectMapper;
    }

    public static class Config {}

    @Override
    public GatewayFilter apply(JwtAuthenticationFilter.Config config) {
        return (exchange, chain) -> {
            ServerHttpRequest request = exchange.getRequest();

            String accessTokenHeaderValue = extractAccessTokenFromHeader(request);

            // 헤더에 AT가 있으면 우선적으로 검증
            if (accessTokenHeaderValue != null) {
                AccessTokenDto accessTokenDto =
                        jwtTokenService.retrieveAccessToken(accessTokenHeaderValue);

                // AT가 유효하면 통과
                if (accessTokenDto != null) {
                    ServerHttpRequest mutatedRequest =
                            request.mutate()
                                    .header("member-id", String.valueOf(accessTokenDto.memberId()))
                                    .header("member-role", accessTokenDto.role().name())
                                    .build();

                    return chain.filter(exchange.mutate().request(mutatedRequest).build());
                }
            }

            return onError(exchange, GatewayAuthErrorCode.AUTH_INVALID_TOKEN);
        };
    }

    private Mono<Void> onError(ServerWebExchange exchange, GatewayAuthErrorCode errorCode) {
        ServerHttpResponse response = exchange.getResponse();

        response.setStatusCode(errorCode.getHttpStatus());
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);

        final ErrorResponse errorResponse =
                ErrorResponse.of(errorCode.name(), errorCode.getMessage());

        try {
            byte[] bytes =
                    objectMapper.writeValueAsBytes(
                            FailResponse.of(errorCode.getHttpStatus().value(), errorResponse));
            DataBuffer buffer = response.bufferFactory().wrap(bytes);

            return response.writeWith(Mono.just(buffer));
        } catch (JsonProcessingException e) {
            response.setStatusCode(HttpStatus.INTERNAL_SERVER_ERROR);

            return response.setComplete();
        }
    }

    private String extractAccessTokenFromHeader(ServerHttpRequest request) {
        String header = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);

        if (header != null && header.startsWith(TOKEN_PREFIX)) {
            return header.replace(TOKEN_PREFIX, "");
        }

        return null;
    }
}
