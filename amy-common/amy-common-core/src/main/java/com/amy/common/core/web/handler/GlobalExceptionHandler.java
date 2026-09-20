package com.amy.common.core.web.handler;

import com.amy.common.core.constant.HttpStatus;
import com.amy.common.core.domain.R;
import com.amy.common.core.exception.InnerAuthException;
import com.amy.common.core.exception.PreAuthorizeException;
import com.amy.common.core.exception.ServiceException;
import com.amy.common.core.exception.auth.NotPermissionException;
import com.amy.common.core.exception.auth.NotRoleException;
import com.amy.common.core.utils.ServletUtils;
import com.amy.common.core.utils.StringUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.converter.HttpMessageConversionException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 全局异常处理器
 *
 * @author amy
 */
@RestControllerAdvice
public class GlobalExceptionHandler
{
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * 权限码异常
     */
    @ExceptionHandler(NotPermissionException.class)
    public R<Void> handleNotPermissionException(NotPermissionException e, HttpServletRequest request) {
        String requestURI = request.getRequestURI();
        log.error("请求地址:{},权限码校验失败:{}", requestURI, e.getMessage());
        ServletUtils.getResponse().setStatus(HttpStatus.FORBIDDEN);
        return R.fail(HttpStatus.FORBIDDEN, "没有访问权限，请联系管理员授权");
    }

    /**
     * 角色权限异常
     */
    @ExceptionHandler(NotRoleException.class)
    public R<Void> handleNotRoleException(NotRoleException e, HttpServletRequest request) {
        String requestURI = request.getRequestURI();
        log.error("请求地址:{},角色权限校验失败:{}", requestURI, e.getMessage());
        ServletUtils.getResponse().setStatus(HttpStatus.FORBIDDEN);
        return R.fail(HttpStatus.FORBIDDEN, "没有访问权限，请联系管理员授权");
    }

    /**
     * 请求方式不支持
     */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public R<Void> handleHttpRequestMethodNotSupported(HttpRequestMethodNotSupportedException e,
                                                                HttpServletRequest request) {
        String requestURI = request.getRequestURI();
        log.error("请求地址:{},不支持{}请求", requestURI, e);
        ServletUtils.getResponse().setStatus(HttpStatus.BAD_METHOD);
        return R.fail(e.getMessage());
    }

    /**
     * 请求参数转换异常
     */
    @ExceptionHandler(HttpMessageConversionException.class)
    public R<Void> httpMessageConversionException(HttpMessageConversionException e, HttpServletRequest request) {
        String requestURI = request.getRequestURI();
        log.error("请求地址:{},请求参数转换异常:{}", requestURI, e.getMessage());
        ServletUtils.getResponse().setStatus(HttpStatus.BAD_REQUEST);
        return R.fail(HttpStatus.BAD_REQUEST, "请求参数异常!");
    }

    /**
     * 空参异常处理
     */
    @ExceptionHandler(BindException.class)
    public R<Void> handleBindException(BindException e, HttpServletRequest request) {
        String requestUri = request.getRequestURI();
        List<Map<String, Object>> bindExeList = new ArrayList<>();
        List<FieldError> fieldErrors = e.getFieldErrors();
        fieldErrors.forEach((oe) -> {
            Map<String, Object> fieldMap = new HashMap<>();
            fieldMap.put("field", oe.getField());
            fieldMap.put("fieldValue", oe.getRejectedValue());
            fieldMap.put("fieldMsg", oe.getDefaultMessage());
            bindExeList.add(fieldMap);
        });
        log.error("请求地址:{},请求参数异常:{}, BindException:{}", requestUri, bindExeList, e.getMessage());

        ServletUtils.getResponse().setStatus(HttpStatus.BAD_REQUEST);
        return R.fail(HttpStatus.BAD_REQUEST, "请求参数异常!");
    }

    /**
     * jsr 规范中的验证异常，参数嵌套对象
     *
     * @param e
     * @return
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public R<Void> constraintViolationException(ConstraintViolationException e, HttpServletRequest request) {
        Set<ConstraintViolation<?>> violations = e.getConstraintViolations();
        String message = violations.stream()
                .map(ConstraintViolation::getMessage)
                .collect(Collectors.joining(";"));
        log.error("请求地址:{}, 请求参数异常:{}, ConstraintViolationException:{}", request.getRequestURI(), message, e.getMessage());
        ServletUtils.getResponse().setStatus(HttpStatus.BAD_REQUEST);
        return R.fail(HttpStatus.BAD_REQUEST, "请求参数异常!");
    }

    /**
     * spring 封装的参数验证异常， 在controller中没有写BindingResult参数时，会进入
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Object handleMethodArgumentNotValidException(MethodArgumentNotValidException e, HttpServletRequest request) {
        String message = e.getBindingResult().getFieldError().getDefaultMessage();
        log.error("请求地址:{}, 请求参数异常:{}, MethodArgumentNotValidException:{}", request.getRequestURI(), message, e.getMessage());
        ServletUtils.getResponse().setStatus(HttpStatus.BAD_REQUEST);
        return R.fail(HttpStatus.BAD_REQUEST, "请求参数异常!");
    }

    /**
     * 内部认证异常
     */
    @ExceptionHandler(InnerAuthException.class)
    public R handleInnerAuthException(InnerAuthException e) {
        ServletUtils.getResponse().setStatus(HttpStatus.UNAUTHORIZED);
        return R.fail(HttpStatus.UNAUTHORIZED, e.getMessage());
    }

    @ExceptionHandler(PreAuthorizeException.class)
    public R handleIPreAuthorizeException(PreAuthorizeException e)
    {
        return R.fail(e.getMessage());
    }

    /**
     * 业务异常
     */
    @ExceptionHandler(ServiceException.class)
    public R handleServiceException(ServiceException e, HttpServletRequest request) {
        log.error("请求地址'{}',业务异常: {}", request.getRequestURI(), e.getMessage());
        ServletUtils.getResponse().setStatus(HttpStatus.BUSINESS_ERROR);
        Integer code = e.getCode();
        return StringUtils.isNotNull(code) ? R.fail(code, e.getMessage()) : R.fail(e.getMessage());
    }

    /**
     * 拦截未知的运行时异常
     */
    @ExceptionHandler(RuntimeException.class)
    public R handleRuntimeException(RuntimeException e, HttpServletRequest request)
    {
        String requestURI = request.getRequestURI();
        log.error("请求地址'{}',运行时异常:{}", requestURI, e);
        ServletUtils.getResponse().setStatus(HttpStatus.BUSINESS_ERROR);
        return R.fail(e.getMessage());
    }

    /**
     * 系统异常
     */
    @ExceptionHandler(Exception.class)
    public R handleException(Exception e, HttpServletRequest request)
    {
        String requestURI = request.getRequestURI();
        log.error("请求地址'{}',发生系统异常:{}", requestURI, e);
        ServletUtils.getResponse().setStatus(HttpStatus.ERROR);
        return R.fail(e.getMessage());
    }
}
