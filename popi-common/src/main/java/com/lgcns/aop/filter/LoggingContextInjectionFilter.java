package com.lgcns.aop.filter;

import com.lgcns.aop.util.LoggingUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class LoggingContextInjectionFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(
            HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        try {
            String traceId = request.getHeader("trace-id");
            String memberId = request.getHeader("member-id");

            LoggingUtil.setTraceId(traceId);
            LoggingUtil.setMemberId(memberId);

            filterChain.doFilter(request, response);
        } finally {
            LoggingUtil.clearMDC();
        }
    }
}
