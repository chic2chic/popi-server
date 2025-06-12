package com.lgcns.logging.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lgcns.logging.dto.HttpRequestLogInfo;
import com.lgcns.logging.dto.HttpResponseLogInfo;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.reactivestreams.Publisher;
import org.slf4j.MDC;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.*;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpRequestDecorator;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.http.server.reactive.ServerHttpResponseDecorator;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Slf4j
@Component
public class HttpLoggingFilter implements GlobalFilter, Ordered {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String traceId = UUID.randomUUID().toString();
        MDC.put("traceId", traceId);

        ServerHttpRequest request = exchange.getRequest();

        /*log.info(
        "[REQ] traceId={}, method={}, uri={}, headers={}",
        traceId,
        request.getMethod(),
        request.getURI(),
        request.getHeaders());*/
        log.info("[REQ] {}", HttpRequestLogInfo.from(request).toJson());

        // WebFlux에서는 body가 DataBuffer 형태의 stream -> join 필요
        return DataBufferUtils.join(request.getBody())
                .defaultIfEmpty(new DefaultDataBufferFactory().wrap(new byte[0])) // body 없을 경우 대비
                .flatMap(
                        dataBuffer -> {
                            byte[] reqBodyBytes = new byte[dataBuffer.readableByteCount()];
                            dataBuffer.read(reqBodyBytes);
                            DataBufferUtils.release(dataBuffer); // 메모리 누수 방지

                            String requestBody = new String(reqBodyBytes, StandardCharsets.UTF_8);

                            log.info("[REQ BODY] traceId={}, body={}", traceId, requestBody);

                            // 읽은 바디를 다시 request에 주입하려면 request를 감싸는 decorator 필요
                            ServerHttpRequest mutatedRequest = request.mutate().build();
                            mutatedRequest =
                                    new ServerHttpRequestDecorator(mutatedRequest) {
                                        @Override
                                        public Flux<DataBuffer> getBody() {
                                            return Flux.just(
                                                    new DefaultDataBufferFactory()
                                                            .wrap(reqBodyBytes));
                                        }
                                    };

                            ServerHttpResponse originalResponse = exchange.getResponse();
                            DataBufferFactory bufferFactory = originalResponse.bufferFactory();

                            // 응답을 감싸는 decorator 생성 → writeWith() 오버라이드해서 body 로깅
                            ServerHttpResponseDecorator decoratedResponse =
                                    new ServerHttpResponseDecorator(originalResponse) {
                                        @Override
                                        public Mono<Void> writeWith(
                                                Publisher<? extends DataBuffer> body) {
                                            // 응답 바디가 Flux일 경우에만 처리
                                            if (body
                                                    instanceof
                                                    Flux<? extends DataBuffer>
                                                    fluxBody) {
                                                return super.writeWith(
                                                        fluxBody.buffer()
                                                                .map(
                                                                        dataBuffers -> {
                                                                            DataBuffer joined =
                                                                                    new DefaultDataBufferFactory()
                                                                                            .join(
                                                                                                    dataBuffers);

                                                                            byte[] content =
                                                                                    new byte
                                                                                            [joined
                                                                                                    .readableByteCount()];
                                                                            joined.read(content);

                                                                            String contentType =
                                                                                    originalResponse
                                                                                            .getHeaders()
                                                                                            .getFirst(
                                                                                                    "Content-Type");
                                                                            HttpResponseLogInfo
                                                                                    responseLog =
                                                                                            HttpResponseLogInfo
                                                                                                    .from(
                                                                                                            content,
                                                                                                            originalResponse
                                                                                                                    .getStatusCode(),
                                                                                                            contentType);
                                                                            log.info(
                                                                                    "[RES] {}",
                                                                                    responseLog
                                                                                            .toJson());

                                                                            // 응답 바디 다시 감싸서 반환
                                                                            return bufferFactory
                                                                                    .wrap(content);
                                                                        }));
                                            }
                                            // Flux가 아닌 다른 타입이면 그대로 통과
                                            return super.writeWith(body);
                                        }
                                    };

                            // 필터 체인을 이어서 실행하되, 변경된 요청(request) + 응답 데코레이터(response) 사용
                            return chain.filter(
                                            exchange.mutate()
                                                    .request(mutatedRequest)
                                                    .response(decoratedResponse)
                                                    .build())
                                    .doFinally(signal -> MDC.clear());
                        });
    }

    @Override
    public int getOrder() {
        return -1;
    }
}
