package com.amy.sunpalaceartspace.service.impl;

import com.amy.common.core.exception.ServiceException;
import com.amy.sunpalaceartspace.config.AliConfig;
import com.amy.sunpalaceartspace.domain.dto.AliIdCheckDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.nio.charset.StandardCharsets;

/**
 * 阿里云市场「身份证二要素核验」调用封装。
 *
 * <p>注意：该接口只接受 {@code application/x-www-form-urlencoded} 表单提交，
 * 传 JSON 请求体会被网关直接拒绝（HTTP 400 且响应体为空，异常信息只剩 "[no body]"，
 * 极难定位）。所以这里必须用 MultiValueMap 而不是 Map 当请求体。</p>
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

    /**
     * 身份证二要素核验（姓名 + 身份证号）
     *
     * @param name  姓名
     * @param idNum 身份证号
     * @return 核验结果，data.result = 0 一致 / 1 不一致
     */
    public AliIdCheckDto checkUserIdentify(String name, String idNum) {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>(2);
        form.add("name", name);
        form.add("idcard", idNum);

        AliIdCheckDto dto;
        try {
            dto = restClient.post()
                    .uri(aliConfig.getUrl())
                    .contentType(FORM_CONTENT_TYPE)
                    .accept(MediaType.APPLICATION_JSON)
                    .header(HttpHeaders.AUTHORIZATION, "APPCODE " + aliConfig.getAppCode())
                    .body(form)
                    .retrieve()
                    .body(AliIdCheckDto.class);
        } catch (RestClientResponseException e) {
            // 阿里云网关对非法请求返回的是空 body，必须把状态码打出来，否则只能看到 "400 Bad Request: [no body]"
            log.error("阿里身份核验接口返回异常状态, status={}, responseBody={}",
                    e.getStatusCode().value(), e.getResponseBodyAsString(), e);
            throw new ServiceException("身份核验服务异常，请稍后重试");
        } catch (ResourceAccessException e) {
            log.error("阿里身份核验接口网络异常, url={}", aliConfig.getUrl(), e);
            throw new ServiceException("身份核验服务连接失败，请检查网络或稍后重试");
        }

        if (dto == null || dto.getData() == null) {
            log.error("阿里身份核验接口返回内容为空, dto={}", dto);
            throw new ServiceException("身份核验服务异常，请稍后重试");
        }
        return dto;
    }
}
