package com.amy.sunpalaceartspace.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.context.annotation.Configuration;

/**
 * @ClassName SpasConfig
 * @Description TODO
 * @Author fantasyfan
 * @Date 2026-11-22 17:53
 */
@Data
@RefreshScope
@Configuration
@ConfigurationProperties(prefix = "spas")
public class SpasConfig {
    /**
     * 单位分钟
     */
    private Long verifyCodeEffectiveDuration = 1L;
}
