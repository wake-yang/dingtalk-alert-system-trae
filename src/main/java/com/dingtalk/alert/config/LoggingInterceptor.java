package com.dingtalk.alert.config;

import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.UUID;

/**
 * 日志拦截器 - 添加traceID和接口耗时统计
 */
@Slf4j
@Component
public class LoggingInterceptor implements HandlerInterceptor {

    private static final String TRACE_ID = "traceId";
    private static final String START_TIME = "startTime";
    private static final String REQUEST_URI = "requestUri";
    private static final String REQUEST_METHOD = "requestMethod";

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 生成traceID
        String traceId = UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        MDC.put(TRACE_ID, traceId);
        
        // 记录请求开始时间
        long startTime = System.currentTimeMillis();
        MDC.put(START_TIME, String.valueOf(startTime));
        
        // 记录请求信息
        String requestUri = request.getRequestURI();
        String requestMethod = request.getMethod();
        MDC.put(REQUEST_URI, requestUri);
        MDC.put(REQUEST_METHOD, requestMethod);
        
        // 记录请求开始日志
        log.info("==> {} {} 开始处理", requestMethod, requestUri);
        
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        try {
            // 计算接口耗时
            String startTimeStr = MDC.get(START_TIME);
            if (startTimeStr != null) {
                long startTime = Long.parseLong(startTimeStr);
                long endTime = System.currentTimeMillis();
                long duration = endTime - startTime;
                
                String requestMethod = MDC.get(REQUEST_METHOD);
                String requestUri = MDC.get(REQUEST_URI);
                
                // 记录请求完成日志
                if (ex != null) {
                    log.error("<== {} {} 处理异常，耗时: {}ms，异常: {}", 
                            requestMethod, requestUri, duration, ex.getMessage());
                } else {
                    log.info("<== {} {} 处理完成，耗时: {}ms，状态码: {}", 
                            requestMethod, requestUri, duration, response.getStatus());
                }
            }
        } finally {
            // 清理MDC
            MDC.clear();
        }
    }
}