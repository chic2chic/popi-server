package com.lgcns.filter;

import static com.lgcns.constants.SecurityConstants.REFRESH_TOKEN_COOKIE_NAME;

import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpCookie;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;

@Component
public class RefreshTokenHeaderFilter
        extends AbstractGatewayFilterFactory<RefreshTokenHeaderFilter.Config> {

    public RefreshTokenHeaderFilter() {
        super(Config.class);
    }

    public static class Config {}

    @Override
    public GatewayFilter apply(RefreshTokenHeaderFilter.Config config) {
        return (exchange, chain) -> {
            HttpCookie cookie =
                    exchange.getRequest().getCookies().getFirst(REFRESH_TOKEN_COOKIE_NAME);

            if (cookie != null) {
                ServerHttpRequest mutatedRequest =
                        exchange.getRequest()
                                .mutate()
                                .header("refresh-token", cookie.getValue())
                                .build();

                exchange = exchange.mutate().request(mutatedRequest).build();
            }

            return chain.filter(exchange);
        };
    }
}
