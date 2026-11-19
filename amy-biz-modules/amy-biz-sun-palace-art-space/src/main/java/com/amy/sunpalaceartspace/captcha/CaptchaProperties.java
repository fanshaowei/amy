package com.amy.sunpalaceartspace.captcha;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 滑动拼图验证码配置（aj-captcha）。
 *
 * <p>官方 captcha-spring-boot-starter 通过 spring.factories 注册自动配置，
 * 在 Spring Boot 3+/4 下已失效（且携带 javax servlet 时代写法），
 * 故这里仅依赖 framework 无关的 captcha 核心 jar，由本类承接配置绑定。</p>
 */
@ConfigurationProperties(prefix = "aj.captcha")
public class CaptchaProperties {

    /** 验证码类型：blockPuzzle 滑动拼图 */
    private String type = "blockPuzzle";

    /** 缓存类型：redis（支持多实例部署）/ local（仅单实例内存） */
    private String cacheType = "redis";

    /** 右下角水印文字 */
    private String waterMark = "我的水印";

    /** 右下角水印字体（核心包内置文泉驿正黑） */
    private String waterFont = "WenQuanZhengHei.ttf";

    /** 校验滑动拼图允许误差偏移量（像素） */
    private String slipOffset = "5";

    /** 坐标 AES 加密开关 */
    private Boolean aesStatus = true;

    /** 滑块干扰项（0/1/2） */
    private String interferenceOptions = "1";

    /** local 缓存阈值 */
    private String cacheNumber = "1000";

    /** local 定时清理过期缓存（秒） */
    private String timingClear = "180";

    /** 接口分钟级频控开关（依赖所选缓存类型） */
    private boolean reqFrequencyLimitEnable = false;

    /** check 连续失败锁定次数 */
    private int reqGetLockLimit = 5;

    /** check 连续失败锁定时长（秒） */
    private int reqGetLockSeconds = 300;

    /** get 接口一分钟内限制次数 */
    private int reqGetMinuteLimit = 60;

    /** check 接口一分钟内限制次数 */
    private int reqCheckMinuteLimit = 60;

    /** 二次校验（verification）一分钟内限制次数 */
    private int reqVerifyMinuteLimit = 60;

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getCacheType() {
        return cacheType;
    }

    public void setCacheType(String cacheType) {
        this.cacheType = cacheType;
    }

    public String getWaterMark() {
        return waterMark;
    }

    public void setWaterMark(String waterMark) {
        this.waterMark = waterMark;
    }

    public String getWaterFont() {
        return waterFont;
    }

    public void setWaterFont(String waterFont) {
        this.waterFont = waterFont;
    }

    public String getSlipOffset() {
        return slipOffset;
    }

    public void setSlipOffset(String slipOffset) {
        this.slipOffset = slipOffset;
    }

    public Boolean getAesStatus() {
        return aesStatus;
    }

    public void setAesStatus(Boolean aesStatus) {
        this.aesStatus = aesStatus;
    }

    public String getInterferenceOptions() {
        return interferenceOptions;
    }

    public void setInterferenceOptions(String interferenceOptions) {
        this.interferenceOptions = interferenceOptions;
    }

    public String getCacheNumber() {
        return cacheNumber;
    }

    public void setCacheNumber(String cacheNumber) {
        this.cacheNumber = cacheNumber;
    }

    public String getTimingClear() {
        return timingClear;
    }

    public void setTimingClear(String timingClear) {
        this.timingClear = timingClear;
    }

    public boolean isReqFrequencyLimitEnable() {
        return reqFrequencyLimitEnable;
    }

    public void setReqFrequencyLimitEnable(boolean reqFrequencyLimitEnable) {
        this.reqFrequencyLimitEnable = reqFrequencyLimitEnable;
    }

    public int getReqGetLockLimit() {
        return reqGetLockLimit;
    }

    public void setReqGetLockLimit(int reqGetLockLimit) {
        this.reqGetLockLimit = reqGetLockLimit;
    }

    public int getReqGetLockSeconds() {
        return reqGetLockSeconds;
    }

    public void setReqGetLockSeconds(int reqGetLockSeconds) {
        this.reqGetLockSeconds = reqGetLockSeconds;
    }

    public int getReqGetMinuteLimit() {
        return reqGetMinuteLimit;
    }

    public void setReqGetMinuteLimit(int reqGetMinuteLimit) {
        this.reqGetMinuteLimit = reqGetMinuteLimit;
    }

    public int getReqCheckMinuteLimit() {
        return reqCheckMinuteLimit;
    }

    public void setReqCheckMinuteLimit(int reqCheckMinuteLimit) {
        this.reqCheckMinuteLimit = reqCheckMinuteLimit;
    }

    public int getReqVerifyMinuteLimit() {
        return reqVerifyMinuteLimit;
    }

    public void setReqVerifyMinuteLimit(int reqVerifyMinuteLimit) {
        this.reqVerifyMinuteLimit = reqVerifyMinuteLimit;
    }
}
