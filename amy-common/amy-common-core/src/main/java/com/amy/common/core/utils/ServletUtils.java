package com.amy.common.core.utils;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.util.LinkedCaseInsensitiveMap;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import com.alibaba.fastjson2.JSON;
import com.amy.common.core.constant.Constants;
import com.amy.common.core.domain.R;
import com.amy.common.core.text.Convert;
import reactor.core.publisher.Mono;

/**
 * 客户端工具类
 * 
 * @author amy
 */
public class ServletUtils
{
    /**
     * 获取String参数
     */
    public static String getParameter(String name)
    {
        return getRequest().getParameter(name);
    }

    /**
     * 获取String参数
     */
    public static String getParameter(String name, String defaultValue)
    {
        return Convert.toStr(getRequest().getParameter(name), defaultValue);
    }

    /**
     * 获取Integer参数
     */
    public static Integer getParameterToInt(String name)
    {
        return Convert.toInt(getRequest().getParameter(name));
    }

    /**
     * 获取Integer参数
     */
    public static Integer getParameterToInt(String name, Integer defaultValue)
    {
        return Convert.toInt(getRequest().getParameter(name), defaultValue);
    }

    /**
     * 获取Boolean参数
     */
    public static Boolean getParameterToBool(String name)
    {
        return Convert.toBool(getRequest().getParameter(name));
    }

    /**
     * 获取Boolean参数
     */
    public static Boolean getParameterToBool(String name, Boolean defaultValue)
    {
        return Convert.toBool(getRequest().getParameter(name), defaultValue);
    }

    /**
     * 获得所有请求参数
     *
     * @param request 请求对象{@link ServletRequest}
     * @return Map
     */
    public static Map<String, String[]> getParams(ServletRequest request)
    {
        final Map<String, String[]> map = request.getParameterMap();
        return Collections.unmodifiableMap(map);
    }

    /**
     * 获得所有请求参数
     *
     * @param request 请求对象{@link ServletRequest}
     * @return Map
     */
    public static Map<String, String> getParamMap(ServletRequest request)
    {
        Map<String, String> params = new HashMap<>();
        for (Map.Entry<String, String[]> entry : getParams(request).entrySet())
        {
            params.put(entry.getKey(), StringUtils.join(entry.getValue(), ","));
        }
        return params;
    }

    /**
     * 获取request
     */
    public static HttpServletRequest getRequest()
    {
        try
        {
            return getRequestAttributes().getRequest();
        }
        catch (Exception e)
        {
            return null;
        }
    }

    /**
     * 获取response
     */
    public static HttpServletResponse getResponse()
    {
        try
        {
            return getRequestAttributes().getResponse();
        }
        catch (Exception e)
        {
            return null;
        }
    }

    /**
     * 获取session
     */
    public static HttpSession getSession()
    {
        return getRequest().getSession();
    }

    public static ServletRequestAttributes getRequestAttributes()
    {
        try
        {
            RequestAttributes attributes = RequestContextHolder.getRequestAttributes();
            return (ServletRequestAttributes) attributes;
        }
        catch (Exception e)
        {
            return null;
        }
    }

    public static String getHeader(HttpServletRequest request, String name)
    {
        String value = request.getHeader(name);
        if (StringUtils.isEmpty(value))
        {
            return StringUtils.EMPTY;
        }
        return urlDecode(value);
    }

    public static Map<String, String> getHeaders(HttpServletRequest request)
    {
        Map<String, String> map = new LinkedCaseInsensitiveMap<>();
        Enumeration<String> enumeration = request.getHeaderNames();
        if (enumeration != null)
        {
            while (enumeration.hasMoreElements())
            {
                String key = enumeration.nextElement();
                String value = request.getHeader(key);
                map.put(key, value);
            }
        }
        return map;
    }

    /**
     * 将字符串渲染到客户端
     * 
     * @param response 渲染对象
     * @param string 待渲染的字符串
     */
    public static void renderString(HttpServletResponse response, String string)
    {
        try
        {
            response.setStatus(200);
            response.setContentType("application/json");
            response.setCharacterEncoding("utf-8");
            response.getWriter().print(string);
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
    }

    /**
     * 是否是Ajax异步请求
     * 
     * @param request
     */
    public static boolean isAjaxRequest(HttpServletRequest request)
    {
        String accept = request.getHeader("accept");
        if (accept != null && accept.contains("application/json"))
        {
            return true;
        }

        String xRequestedWith = request.getHeader("X-Requested-With");
        if (xRequestedWith != null && xRequestedWith.contains("XMLHttpRequest"))
        {
            return true;
        }

        String uri = request.getRequestURI();
        if (StringUtils.inStringIgnoreCase(uri, ".json", ".xml"))
        {
            return true;
        }

        String ajax = request.getParameter("__ajax");
        return StringUtils.inStringIgnoreCase(ajax, "json", "xml");
    }

    /**
     * 内容编码
     * 
     * @param str 内容
     * @return 编码后的内容
     */
    public static String urlEncode(String str)
    {
        try
        {
            return URLEncoder.encode(str, Constants.UTF8);
        }
        catch (UnsupportedEncodingException e)
        {
            return StringUtils.EMPTY;
        }
    }

    /**
     * 内容解码
     * 
     * @param str 内容
     * @return 解码后的内容
     */
    public static String urlDecode(String str)
    {
        try
        {
            return URLDecoder.decode(str, Constants.UTF8);
        }
        catch (UnsupportedEncodingException e)
        {
            return StringUtils.EMPTY;
        }
    }

    /**
     * 网关错误响应（推荐入口）：HTTP 状态码与响应体业务码统一使用 status.value()。
     *
     * <p>响应体仍是标准 R 结构 R.fail(code, msg)，按 body.code / body.msg 判断的客户端
     * （后台、小程序）无需同步改动；同时 HTTP 层正确表达 401/403/404/429/500 语义，
     * 便于监控、缓存策略以及 axios/fetch 的 error 分支识别。</p>
     *
     * <p>下面两个以 int code 结尾的重载会把 HTTP 状态固定为 200，只把 code 写进响应体。
     * 历史上 AuthFilter 传入 HttpStatus.UNAUTHORIZED（int 常量 401）命中的正是那个重载，
     * 导致鉴权失败被伪装成 HTTP 200；网关侧「请求被拒绝 / 处理失败」请勿再使用它们。</p>
     *
     * @param response ServerHttpResponse
     * @param status HTTP 状态码（Spring 枚举，如 HttpStatus.UNAUTHORIZED）
     * @param msg 返回给调用方的中文提示
     * @return Mono&lt;Void&gt;
     */
    public static Mono<Void> webFluxResponseWriter(ServerHttpResponse response, HttpStatus status, String msg)
    {
        String text = (msg == null || msg.isBlank()) ? status.getReasonPhrase() : msg;
        return webFluxResponseWriter(response, MediaType.APPLICATION_JSON_VALUE, status,
                text, status.value());
    }

    /**
     * 设置webflux模型响应。
     *
     * @deprecated HTTP 状态固定为 200（失败也被当成成功响应），仅用于兼容旧调用；
     *             网关错误请改用 {@link #webFluxResponseWriter(ServerHttpResponse, HttpStatus, String)}。
     *
     * @param response ServerHttpResponse
     * @param value 响应内容
     * @return Mono&lt;Void&gt;
     */
    @Deprecated
    public static Mono<Void> webFluxResponseWriter(ServerHttpResponse response, Object value)
    {
        return webFluxResponseWriter(response, HttpStatus.OK, value, R.FAIL);
    }

    /**
     * 设置webflux模型响应。
     *
     * @deprecated HTTP 状态固定为 200，第三个参数只是写进响应体的业务码，不是 HTTP 状态码；
     *             网关错误请改用 {@link #webFluxResponseWriter(ServerHttpResponse, HttpStatus, String)}。
     *
     * @param response ServerHttpResponse
     * @param code 响应业务码
     * @param value 响应内容
     * @return Mono&lt;Void&gt;
     */
    @Deprecated
    public static Mono<Void> webFluxResponseWriter(ServerHttpResponse response, Object value, int code)
    {
        return webFluxResponseWriter(response, HttpStatus.OK, value, code);
    }

    /**
     * 设置webflux模型响应
     *
     * @param response ServerHttpResponse
     * @param status http状态码
     * @param code 响应状态码
     * @param value 响应内容
     * @return Mono<Void>
     */
    public static Mono<Void> webFluxResponseWriter(ServerHttpResponse response, HttpStatus status, Object value, int code)
    {
        return webFluxResponseWriter(response, MediaType.APPLICATION_JSON_VALUE, status, value, code);
    }

    /**
     * 设置webflux模型响应
     *
     * @param response ServerHttpResponse
     * @param contentType content-type
     * @param status http状态码
     * @param code 响应状态码
     * @param value 响应内容
     * @return Mono<Void>
     */
    public static Mono<Void> webFluxResponseWriter(ServerHttpResponse response, String contentType, HttpStatus status, Object value, int code)
    {
        response.setStatusCode(status);
        response.getHeaders().add(HttpHeaders.CONTENT_TYPE, contentType);
        // value 可能为 null（如 ValidateCodeFilter 里 e.getMessage() 为空），兜底避免 NPE
        R<?> result = R.fail(code, String.valueOf(value));
        DataBuffer dataBuffer = response.bufferFactory().wrap(JSON.toJSONString(result).getBytes());
        return response.writeWith(Mono.just(dataBuffer));
    }
}
