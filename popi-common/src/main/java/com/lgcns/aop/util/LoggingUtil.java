package com.lgcns.aop.util;

import jakarta.servlet.http.HttpServletRequest;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.HashMap;
import java.util.Map;
import org.slf4j.MDC;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

public class LoggingUtil {

    public static final String TRACE_ID = "traceId";
    public static final String MEMBER_ID = "memberId";

    public static void setTraceId(String traceId) {
        if (isNotEmpty(traceId)) MDC.put(TRACE_ID, traceId);
    }

    public static String getTraceId() {
        return MDC.get(TRACE_ID);
    }

    public static void setMemberId(String memberId) {
        if (isNotEmpty(memberId)) MDC.put(MEMBER_ID, memberId);
    }

    public static String getMemberId() {
        return MDC.get(MEMBER_ID);
    }

    public static void clearMDC() {
        MDC.clear();
    }

    private static boolean isNotEmpty(String value) {
        return value != null && !value.isEmpty();
    }

    public static HttpServletRequest getCurrentHttpRequest() {
        RequestAttributes attributes = RequestContextHolder.getRequestAttributes();
        if (attributes instanceof ServletRequestAttributes servletRequestAttributes) {
            return servletRequestAttributes.getRequest();
        }
        return null;
    }

    public static String getRequestUriWithQuery(HttpServletRequest request) {
        if (request == null) return null;
        String query = request.getQueryString();
        return request.getRequestURI() + (query == null ? "" : "?" + query);
    }

    public static String getMethodSignature(Method method) {
        return method.getDeclaringClass().getSimpleName() + "." + method.getName();
    }

    public static Map<String, Object> extractParams(Method method, Object[] args) {
        Map<String, Object> paramMap = new HashMap<>();
        if (args == null || args.length == 0) return paramMap;

        Parameter[] parameters = method.getParameters();
        for (int i = 0; i < parameters.length; i++) {
            paramMap.put(parameters[i].getName(), args[i]);
        }
        return paramMap;
    }

    public static long calculateDuration(long startTime) {
        return System.currentTimeMillis() - startTime;
    }

    public static String getShortErrorMessage(String errorMessage) {
        return errorMessage != null && errorMessage.contains(":")
                ? errorMessage.substring(errorMessage.lastIndexOf(":") + 1).trim()
                : errorMessage;
    }
}
