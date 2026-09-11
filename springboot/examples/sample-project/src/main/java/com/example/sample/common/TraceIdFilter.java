package com.example.sample.common;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

/** traceId 入口过滤器：优先透传上游 {@code X-Trace-Id}，缺失则生成，绑定 MDC 并回写响应头。 */
@Component
public class TraceIdFilter extends OncePerRequestFilter {

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {
    String traceId = request.getHeader(TraceId.HEADER);
    if (!StringUtils.hasText(traceId)) {
      traceId = TraceId.generate();
    }
    TraceId.bind(traceId);
    response.setHeader(TraceId.HEADER, traceId);
    try {
      filterChain.doFilter(request, response);
    } finally {
      TraceId.clear();
    }
  }
}
