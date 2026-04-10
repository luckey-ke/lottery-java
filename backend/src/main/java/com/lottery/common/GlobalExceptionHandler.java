package com.lottery.common;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 全局异常处理 + 响应包装
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ApiResult<Void> handleBusiness(BusinessException e, HttpServletRequest req) {
        log.warn("[业务异常] {} {} → {}", req.getMethod(), req.getRequestURI(), e.getMessage());
        return ApiResult.error(e.getCode(), e.getMessage());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResult<Void> handleIllegalArg(IllegalArgumentException e, HttpServletRequest req) {
        log.warn("[参数错误] {} {} → {}", req.getMethod(), req.getRequestURI(), e.getMessage());
        return ApiResult.error(400, e.getMessage());
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ApiResult<Void> handleException(Exception e, HttpServletRequest req) {
        log.error("[系统异常] {} {}", req.getMethod(), req.getRequestURI(), e);
        return ApiResult.error("服务器内部错误");
    }
}
