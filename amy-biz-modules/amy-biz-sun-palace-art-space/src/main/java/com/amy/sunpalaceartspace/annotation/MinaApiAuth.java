package com.amy.sunpalaceartspace.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @ClassName MinaApiAuth
 * @Description TODO
 * @Author fantasyfan
 * @Date 2026-09-20 17:31
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface MinaApiAuth {
}
