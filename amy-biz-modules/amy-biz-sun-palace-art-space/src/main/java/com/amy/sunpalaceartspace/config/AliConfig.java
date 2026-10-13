package com.amy.sunpalaceartspace.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.context.annotation.Configuration;

/**
 * @ClassName AliConfig
 * @Description TODO
 * @Author fantasyfan
 * @Date 2026-10-14 00:47
 */
@Data
@RefreshScope
@Configuration
@ConfigurationProperties(prefix = "ali.id-check")
public class AliConfig {
    private String url;
    private String appKey;
    private String appSecret;
    private String appCode;
}
