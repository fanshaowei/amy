package com.amy.sunpalaceartspace.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.script.DefaultRedisScript;

/**
 * @ClassName RedisScriptConfig
 * @Description TODO
 * @Author fantasyfan
 * @Date 2026-09-27 15:48
 */
@Configuration
public class RedisScriptConfig {
    private final String reserveScriptPath = "lua/reserve.lua";

    @Bean
    public DefaultRedisScript<Long> reserveScript() {
        DefaultRedisScript<Long> script = new DefaultRedisScript<>();
        script.setLocation(new ClassPathResource(reserveScriptPath));
        script.setResultType(Long.class);

        return script;
    }
}
