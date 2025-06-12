package com.lgcns.filter;

import java.util.UUID;
import java.util.regex.Pattern;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class TraceIdGenerationFilter implements GlobalFilter {

    private static final Pattern EXCLUDED_PATH_PATTERN =
            Pattern.compile("^/[^/]+/(v3/api-docs|actuator|prometheus)(/.*)?$");

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getURI().getPath();

        if (EXCLUDED_PATH_PATTERN.matcher(path).matches()) {
            return chain.filter(exchange);
        }

        String traceId = UUID.randomUUID().toString();

        ServerHttpRequest mutatedRequest =
                exchange.getRequest().mutate().header("trace-id", traceId).build();

        return chain.filter(exchange.mutate().request(mutatedRequest).build());
    }
}
