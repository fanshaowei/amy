package com.amy.sunpalaceartspace.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

/**
 * @ClassName RestClientConfig
 * @Description TODO
 * @Author fantasyfan
 * @Date 2026-10-14 00:56
 */
@Configuration
public class RestClientConfig {
    @Bean
    public RestClient restTemplate() {
        return RestClient.builder().build();
    }
}
