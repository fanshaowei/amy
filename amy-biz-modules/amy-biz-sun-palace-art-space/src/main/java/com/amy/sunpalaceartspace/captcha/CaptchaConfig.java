package com.amy.sunpalaceartspace.captcha;

import com.anji.captcha.model.common.Const;
import com.anji.captcha.service.CaptchaService;
import com.anji.captcha.service.impl.CaptchaServiceFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.Properties;

/**
 * 滑动拼图验证码装配（aj-captcha 核心 jar）。
 *
 * <p>底图使用核心包内置 defaultImages，无需额外资源；
 * 缓存实现由 META-INF/services SPI 注册（redis/local 均内置可选）。</p>
 */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(CaptchaProperties.class)
public class CaptchaConfig {

    private static final Logger log = LoggerFactory.getLogger(CaptchaConfig.class);

    @Bean
    @ConditionalOnMissingBean
    public CaptchaService captchaService(CaptchaProperties prop, StringRedisTemplate stringRedisTemplate) {
        // redis 缓存由 SPI（ServiceLoader）实例化后注入连接工厂
        if ("redis".equals(prop.getCacheType())) {
            CaptchaCacheServiceRedisImpl redisCache =
                    (CaptchaCacheServiceRedisImpl) CaptchaServiceFactory.getCache("redis");
            redisCache.setStringRedisTemplate(stringRedisTemplate);
        }

        Properties config = new Properties();
        config.put(Const.CAPTCHA_CACHETYPE, prop.getCacheType());
        config.put(Const.CAPTCHA_WATER_MARK, prop.getWaterMark());
        config.put(Const.CAPTCHA_FONT_TYPE, prop.getWaterFont());
        config.put(Const.CAPTCHA_TYPE, prop.getType());
        config.put(Const.CAPTCHA_INTERFERENCE_OPTIONS, prop.getInterferenceOptions());
        config.put(Const.CAPTCHA_SLIP_OFFSET, prop.getSlipOffset());
        config.put(Const.CAPTCHA_AES_STATUS, String.valueOf(prop.getAesStatus()));
        config.put(Const.CAPTCHA_WATER_FONT, prop.getWaterFont());
        config.put(Const.CAPTCHA_CACAHE_MAX_NUMBER, prop.getCacheNumber());
        config.put(Const.CAPTCHA_TIMING_CLEAR_SECOND, prop.getTimingClear());
        config.put(Const.REQ_FREQUENCY_LIMIT_ENABLE, prop.isReqFrequencyLimitEnable() ? "1" : "0");
        config.put(Const.REQ_GET_LOCK_LIMIT, prop.getReqGetLockLimit() + "");
        config.put(Const.REQ_GET_LOCK_SECONDS, prop.getReqGetLockSeconds() + "");
        config.put(Const.REQ_GET_MINUTE_LIMIT, prop.getReqGetMinuteLimit() + "");
        config.put(Const.REQ_CHECK_MINUTE_LIMIT, prop.getReqCheckMinuteLimit() + "");
        config.put(Const.REQ_VALIDATE_MINUTE_LIMIT, prop.getReqVerifyMinuteLimit() + "");

        CaptchaService service = CaptchaServiceFactory.getInstance(config);
        log.info("[captcha] 滑动拼图验证码初始化完成, cacheType={}", prop.getCacheType());
        return service;
    }
}
