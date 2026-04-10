package com.lottery.common;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 全局异常处理
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

    @ExceptionHandler(MissingServletRequestParameterException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, Object> handleMissingParam(MissingServletRequestParameterException e, HttpServletRequest req) {
        log.warn("[缺少参数] {} {} → {}", req.getMethod(), req.getRequestURI(), e.getMessage());
        return errorMap(400, "缺少必要参数: " + e.getParameterName());
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    @ResponseStatus(HttpStatus.METHOD_NOT_ALLOWED)
    public Map<String, Object> handleMethodNotSupported(HttpRequestMethodNotSupportedException e, HttpServletRequest req) {
        log.warn("[方法不允许] {} {} → {}", req.getMethod(), req.getRequestURI(), e.getMessage());
        return errorMap(405, "不支持的请求方法: " + req.getMethod());
    }

    @ExceptionHandler(NoResourceFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public Map<String, Object> handleNotFound(NoResourceFoundException e, HttpServletRequest req) {
        return errorMap(404, "资源不存在");
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
