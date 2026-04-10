package com.lottery.common;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 全局异常处理
 * <p>
 * 正常响应直接返回 Map（前端无需改动）。
 * 异常响应也返回 Map + 正确的 HTTP status code。
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public Map<String, Object> handleBusiness(BusinessException e, HttpServletRequest req, HttpServletResponse resp) {
        log.warn("[业务异常] {} {} → {}", req.getMethod(), req.getRequestURI(), e.getMessage());
        resp.setStatus(e.getCode());
        return errorMap(e.getCode(), e.getMessage());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, Object> handleIllegalArg(IllegalArgumentException e, HttpServletRequest req) {
        log.warn("[参数错误] {} {} → {}", req.getMethod(), req.getRequestURI(), e.getMessage());
        return errorMap(400, e.getMessage());
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Map<String, Object> handleException(Exception e, HttpServletRequest req) {
        log.error("[系统异常] {} {}", req.getMethod(), req.getRequestURI(), e);
        return errorMap(500, "服务器内部错误");
    }

    private Map<String, Object> errorMap(int code, String message) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("error", message);
        map.put("code", code);
        return map;
    }
}
