package com.amy.sunpalaceartspace.aspect;

import com.amy.common.core.exception.InnerAuthException;
import com.amy.common.core.exception.ServiceException;
import com.amy.common.core.utils.DateUtils;
import com.amy.common.core.utils.JwtUtils;
import com.amy.common.core.utils.ServletUtils;
import com.amy.common.redis.service.RedisService;
import com.amy.sunpalaceartspace.annotation.MinaApiAuth;
import com.amy.sunpalaceartspace.domain.bo.WechatApiTokenClaimBO;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.logging.log4j.util.Strings;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.concurrent.TimeUnit;

import static com.amy.sunpalaceartspace.constant.SpasConstant.CASH_WX_API_TOKEN_OPEN_ID;

/**
 * @ClassName MinaApiAuthAspect
 * @Description TODO
 * @Author fantasyfan
 * @Date 2026-09-20 17:33
 */
@Slf4j
@AllArgsConstructor
@Aspect
@Component
public class MinaApiAuthAspect {
    private final RedisService redisService;
    private final JwtUtils jwtUtils;
    private final ObjectMapper objectMapper;

    private final String errMsg = "用户未登录或登录已过期，请重新登录";

    @Around("@annotation(minaApiAuth)")
    public Object aroundMinaApiAuth(ProceedingJoinPoint point, MinaApiAuth minaApiAuth) throws Throwable {
        String token = ServletUtils.getRequest().getHeader("Authorization");
        if(Strings.isBlank(token)) {
            throw new InnerAuthException(errMsg);
        }

        try {
            Claims claims = jwtUtils.parseToken(token);
            WechatApiTokenClaimBO wechatApiTokenClaimBO = objectMapper.convertValue(claims, WechatApiTokenClaimBO.class);
            Long exp = wechatApiTokenClaimBO.getExp();
            // exp 已是秒（NumericDate），转回毫秒再比较
            if (DateUtils.getNowDate().after(new Date(exp * 1000))) {
                throw new InnerAuthException(errMsg);
            }

            Object openId = redisService.getCacheObject(CASH_WX_API_TOKEN_OPEN_ID.formatted(token));
            if (null == openId) {
                throw new InnerAuthException(errMsg);
            } else {
                // 续期
                redisService.setCacheObject(String.format(CASH_WX_API_TOKEN_OPEN_ID, token), openId, 2L, TimeUnit.HOURS);
            }
        } catch (ExpiredJwtException e) {
            log.error("token expired, {}", e.getMessage());
            throw new InnerAuthException("当前登录已过期，请重新登录");
        } catch (Exception e) {
            log.error(e.getMessage());
            throw new ServiceException("系统异常，请稍后重试");
        }

        return point.proceed();
    }
}
