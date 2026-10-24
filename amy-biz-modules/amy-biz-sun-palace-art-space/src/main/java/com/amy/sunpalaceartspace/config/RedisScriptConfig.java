package com.amy.sunpalaceartspace.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
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

    /**
     * 用于执行 Lua 脚本的 String 序列化模板。
     * <p>脚本的 KEYS/ARGV 全部以纯字符串传入（不做 JSON 编码），避免 {@code amyRedisTemplate}
     * 的 FastJson 序列化器把参数序列化成带双引号的 JSON（如 {@code "600"}）导致
     * {@code SET ... EX} 等需要整数的命令报 {@code value is not an integer or out of range}。</p>
     */
    @Bean
    public StringRedisTemplate reservationStringRedisTemplate(RedisConnectionFactory connectionFactory) {
        return new StringRedisTemplate(connectionFactory);
    }
}
