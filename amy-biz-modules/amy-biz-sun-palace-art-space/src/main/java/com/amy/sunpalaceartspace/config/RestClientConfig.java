package com.amy.sunpalaceartspace.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;
import java.time.Duration;

/**
 * @ClassName RestClientConfig
 * @Description TODO
 * @Author fantasyfan
 * @Date 2026-10-14 00:56
 */
@Configuration
public class RestClientConfig {

    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(5);
    private static final Duration READ_TIMEOUT = Duration.ofSeconds(10);

    /**
     * 外部接口调用统一走这里，必须带超时：
     * 不设置的话第三方接口挂起会一直占着 tomcat 线程，几次就把服务拖死。
     */
    @Bean
    public RestClient restTemplate() {
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(
                HttpClient.newBuilder().connectTimeout(CONNECT_TIMEOUT).build());
        requestFactory.setReadTimeout(READ_TIMEOUT);
        return RestClient.builder().requestFactory(requestFactory).build();
    }
}
