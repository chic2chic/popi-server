package com.lgcns.logging.filter;

import com.lgcns.logging.dto.HttpRequestLogInfo;
import com.lgcns.logging.dto.HttpResponseLogInfo;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.reactivestreams.Publisher;
import org.slf4j.MDC;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.*;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.http.server.reactive.ServerHttpResponseDecorator;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Slf4j
@Component
public class HttpLoggingFilter implements GlobalFilter, Ordered {

    private static final DataBufferFactory bufferFactory = new DefaultDataBufferFactory();

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String traceId = UUID.randomUUID().toString();
        MDC.put("traceId", traceId);

        logRequest(exchange);

        ServerHttpResponse originalResponse = exchange.getResponse();

        // 응답 바디를 가로채기 위해 응답 객체를 데코레이터로 감쌈
        ServerHttpResponseDecorator decoratedResponse =
                new ServerHttpResponseDecorator(originalResponse) {

                    @Override
                    public Mono<Void> writeWith(Publisher<? extends DataBuffer> body) {
                        if (body instanceof Flux<? extends DataBuffer> fluxBody) {
                            return super.writeWith(
                                    fluxBody.buffer()
                                            .map(
                                                    dataBuffers -> {
                                                        DataBuffer joined =
                                                                bufferFactory.join(dataBuffers);
                                                        byte[] content =
                                                                new byte
                                                                        [joined
                                                                                .readableByteCount()];
                                                        joined.read(content);

                                                        logResponse(content, originalResponse);

                                                        // 읽은 응답 바디를 클라이언트에게 다시 전달 (body는 한 번만 소비
                                                        // 가능하므로 wrap 필요)
                                                        return bufferFactory.wrap(content);
                                                    }));
                        }

                        // Flux가 아닌 body 타입이면 그대로 처리 (예외적인 경우)
                        return super.writeWith(body);
                    }
                };

        // 응답 객체만 데코레이터로 변환 + 체인 필터 실행
        return chain.filter(exchange.mutate().response(decoratedResponse).build())
                .doFinally(signal -> MDC.clear());
    }

    // 필터 우선순위 설정 - 가장 먼저 실행
    @Override
    public int getOrder() {
        return -1;
    }

    private static void logRequest(ServerWebExchange exchange) {
        ServerHttpRequest request = exchange.getRequest();
        log.info(HttpRequestLogInfo.from(request).toJson());
    }

    private static void logResponse(byte[] content, ServerHttpResponse originalResponse) {
        String contentType = originalResponse.getHeaders().getFirst("Content-Type");

        HttpResponseLogInfo responseLog =
                HttpResponseLogInfo.from(content, originalResponse.getStatusCode(), contentType);

        log.info(responseLog.toJson());
    }
}
