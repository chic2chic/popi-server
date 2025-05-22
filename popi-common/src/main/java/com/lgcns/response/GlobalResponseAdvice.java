package com.lgcns.response;

import com.lgcns.annotation.RawResponse;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpResponse;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

@RestControllerAdvice(basePackages = "com.lgcns.externalApi")
public class GlobalResponseAdvice implements ResponseBodyAdvice {

    @Override
    public boolean supports(MethodParameter returnType, Class converterType) {
        return true;
    }

    @Override
    public Object beforeBodyWrite(
            Object body,
            MethodParameter returnType,
            MediaType selectedContentType,
            Class selectedConverterType,
            ServerHttpRequest request,
            ServerHttpResponse response) {

        if (hasRawResponseAnnotation(returnType)) return body;

        HttpServletResponse servletResponse =
                ((ServletServerHttpResponse) response).getServletResponse();
        int status = servletResponse.getStatus();
        HttpStatus resolve = HttpStatus.resolve(status);

        if (resolve == null || body instanceof String) {
            return body;
        }

        if (resolve.is2xxSuccessful()) {
            return GlobalResponse.success(status, body);
        }

        return body;
    }

    private boolean hasRawResponseAnnotation(MethodParameter returnType) {
        return returnType.getMethodAnnotation(RawResponse.class) != null
                || returnType.getDeclaringClass().getAnnotation(RawResponse.class) != null;
    }
}
