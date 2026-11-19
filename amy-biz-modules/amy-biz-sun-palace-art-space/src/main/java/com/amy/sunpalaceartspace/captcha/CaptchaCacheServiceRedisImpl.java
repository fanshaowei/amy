package com.amy.sunpalaceartspace.captcha;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;

import java.util.Collections;
import java.util.concurrent.TimeUnit;

/**
 * aj-captcha Redis 缓存实现（SPI 注册见 resources/META-INF/services）。
 *
 * <p>验证码生成与校验状态存 Redis，保证多实例部署时生产/校验节点同步；
 * 参考官方 service/springboot3 示例实现。</p>
 */
public class CaptchaCacheServiceRedisImpl implements com.anji.captcha.service.CaptchaCacheService {

    private static final String LUA_SCRIPT = "local key = KEYS[1] " +
            "local incrementValue = tonumber(ARGV[1]) " +
            "if redis.call('EXISTS', key) == 1 then " +
            "    return redis.call('INCRBY', key, incrementValue) " +
            "else " +
            "    return incrementValue " +
            "end";

    private StringRedisTemplate stringRedisTemplate;

    public void setStringRedisTemplate(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    @Override
    public String type() {
        return "redis";
    }

    @Override
    public void set(String key, String value, long expiresInSeconds) {
        stringRedisTemplate.opsForValue().set(key, value, expiresInSeconds, TimeUnit.SECONDS);
    }

    @Override
    public boolean exists(String key) {
        return Boolean.TRUE.equals(stringRedisTemplate.hasKey(key));
    }

    @Override
    public void delete(String key) {
        stringRedisTemplate.delete(key);
    }

    @Override
    public String get(String key) {
        return stringRedisTemplate.opsForValue().get(key);
    }

    @Override
    public Long increment(String key, long val) {
        RedisScript<Long> script = new DefaultRedisScript<>(LUA_SCRIPT, Long.class);
        return stringRedisTemplate.execute(
                script,
                Collections.singletonList(key),
                String.valueOf(val)
        );
    }

    @Override
    public void setExpire(String key, long l) {
        stringRedisTemplate.expire(key, l, TimeUnit.SECONDS);
    }
}
