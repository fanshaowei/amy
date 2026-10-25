package com.amy.sunpalaceartspace.service.impl;

import com.amy.common.core.exception.ServiceException;
import com.amy.sunpalaceartspace.config.AliConfig;
import com.amy.sunpalaceartspace.domain.dto.AliIdCheckDto;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.nio.charset.StandardCharsets;

/**
 * 阿里云市场「身份证二要素核验」调用封装。
 *
 * <p>请求侧：该接口只接受 {@code application/x-www-form-urlencoded} 表单提交，
 * 传 JSON 请求体会被网关直接拒绝（HTTP 400 且响应体为空，异常信息只剩 "[no body]"，
 * 极难定位）。所以这里必须用 MultiValueMap 而不是 Map 当请求体。</p>
 *
 * <p>响应侧：网关/接口返回的 Content-Type 可能是 {@code text/plain;charset=utf-8}，
 * 而内容其实是 JSON 字符串。若直接用 {@code body(AliIdCheckDto.class)}，
 * Spring 找不到能把 text/plain 转成 DTO 的 HttpMessageConverter，会抛
 * {@link org.springframework.web.client.UnknownContentTypeException}
 * （它继承 {@link RestClientException} 而非 {@link RestClientResponseException}，
 * 很容易漏 catch）。因此这里先以 String 收原文，再手动反序列化。</p>
 *
 * @ClassName AliIdCheckService
 * @Description 阿里身份核验
 * @Author fantasyfan
 * @Date 2026-10-14 00:59
 */
@Slf4j
@RequiredArgsConstructor
@Service
public class AliIdCheckService {

    /**
     * 表单编码的 Content-Type，显式带上 UTF-8。
     * 不写 charset 时，姓名里的中文有可能按 ISO-8859-1 编码发出去，导致核验结果与姓名不符。
     */
    private static final MediaType FORM_CONTENT_TYPE =
            new MediaType(MediaType.APPLICATION_FORM_URLENCODED, StandardCharsets.UTF_8);

    private final AliConfig aliConfig;
    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    /**
     * 身份证二要素核验（姓名 + 身份证号）
     *
     * @param name  姓名
     * @param idNum 身份证号
     * @return 核验结果，data.isok = true 一致 / false 不一致
     */
    public AliIdCheckDto checkUserIdentify(String name, String idNum) {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>(2);
        form.add("realName", name);
        form.add("cardNo", idNum);

        String rawBody;
        try {
            // 先以 String 接收：无论对端回 application/json 还是 text/plain 都能拿到原文，
            // 避免 HttpMessageConverter 不匹配导致 UnknownContentTypeException。
            ResponseEntity<String> response = restClient.post()
                    .uri(aliConfig.getUrl())
                    .contentType(FORM_CONTENT_TYPE)
                    .accept(MediaType.APPLICATION_JSON, MediaType.TEXT_PLAIN)
                    .header(HttpHeaders.AUTHORIZATION, "APPCODE " + aliConfig.getAppCode())
                    .body(form)
                    .retrieve()
                    .toEntity(String.class);
            rawBody = response.getBody();
        } catch (RestClientResponseException e) {
            // 阿里云网关对非法请求返回的是空 body，必须把状态码打出来，否则只能看到 "400 Bad Request: [no body]"
            log.error("阿里身份核验接口返回异常状态, status={}, responseBody={}",
                    e.getStatusCode().value(), e.getResponseBodyAsString(), e);
            throw new ServiceException("身份核验服务异常，请稍后重试");
        } catch (ResourceAccessException e) {
            log.error("阿里身份核验接口网络异常, url={}", aliConfig.getUrl(), e);
            throw new ServiceException("身份核验服务连接失败，请检查网络或稍后重试");
        } catch (RestClientException e) {
            // 响应类型与期望类型不匹配（content type 非 JSON）等，兜住防止原始异常暴露到前端
            log.error("阿里身份核验接口响应解析失败, 请检查 APPCODE 是否有效/是否已购买该接口, url={}",
                    aliConfig.getUrl(), e);
            throw new ServiceException("身份核验服务异常，请稍后重试");
        }

        if (!StringUtils.hasText(rawBody)) {
            log.error("阿里身份核验接口返回空响应体, 常见于 APPCODE 无效/欠费/未购买该接口, url={}", aliConfig.getUrl());
            throw new ServiceException("身份核验服务异常，请稍后重试");
        }

        AliIdCheckDto dto;
        try {
            dto = objectMapper.readValue(rawBody, AliIdCheckDto.class);
        } catch (Exception e) {
            // 网关异常时返回的是 text/plain 明文（如 "AppCode is invalid"），打原文才能定位
            log.error("阿里身份核验响应体解析失败, 原始响应={}", rawBody, e);
            throw new ServiceException("身份核验服务返回异常，请稍后重试");
        }

        if (dto.getErrorCode() == null || dto.getErrorCode() != 0) {
            // 注意：errorCode 是 Integer，必须先判 null 再比较，否则拆箱 NPE
            log.error("身份核验服务返回业务错误, errorCode={}, reason={}, 原始响应={}",
                    dto.getErrorCode(), dto.getReason(), rawBody);
            throw new ServiceException(StringUtils.hasText(dto.getReason())
                    ? dto.getReason()
                    : "身份核验服务异常，请稍后重试");
        }
        return dto;
    }
}
