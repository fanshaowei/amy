package com.amy.sunpalaceartspace.service.impl;

import com.amy.sunpalaceartspace.config.AliConfig;
import com.amy.sunpalaceartspace.domain.dto.AliIdCheckDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.Map;

/**
 * @ClassName AliIdCheckService
 * @Description TODO
 * @Author fantasyfan
 * @Date 2026-10-14 00:59
 */
@Slf4j
@RequiredArgsConstructor
@Service
public class AliIdCheckService {
    private final AliConfig aliConfig;
    private final RestClient restClient;

    public AliIdCheckDto checkUserIdentify(String name, String idNum) {
        Map<String, String> body = Map.of("name", name, "idcard", idNum);
        ResponseEntity<AliIdCheckDto> entity = restClient.post()
                .uri(aliConfig.getUrl())
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .header(HttpHeaders.AUTHORIZATION, "APPCODE " + aliConfig.getAppCode())
                .body(body)
                .retrieve()
                .toEntity(AliIdCheckDto.class);
        return entity.getBody();
    }
}
