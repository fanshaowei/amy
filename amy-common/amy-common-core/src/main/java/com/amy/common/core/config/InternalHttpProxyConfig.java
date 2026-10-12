package com.amy.common.core.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;

/**
 * 内部服务调用（Feign / HttpURLConnection）绕过 JVM 系统代理。
 * <p>
 * 背景：开发机上如果配置了系统代理（Clash / Mihomo 等，IDEA 会把 -Dhttp.proxyHost、-DsocksProxyHost
 * 传给启动的 JVM），Feign 默认使用 {@code Client.Default}，底层是 {@code java.net.HttpURLConnection}，
 * 它会走 JVM 的系统代理。于是「auth -> amy-system」这类内网调用会被发往代理端口（如 127.0.0.1:7890），
 * 一旦代理软件没启动，就会直接报：
 * <pre>
 * Connection refused executing GET http://amy-system/user/info/admin
 * </pre>
 * 网关（WebFlux / Reactor Netty）不走 JVM 代理，所以现象是「网关转发正常、Feign 调用全部失败」。
 * <p>
 * 处理：启动时把本机回环地址与 RFC1918 内网地址追加到 {@code http.nonProxyHosts} 和
 * {@code socksNonProxyHosts}，保证服务间调用永远直连；外部地址仍按原样走代理，不影响正常上网。
 *
 * @author amy
 */
@Configuration
public class InternalHttpProxyConfig
{
    private static final Logger log = LoggerFactory.getLogger(InternalHttpProxyConfig.class);

    /** 需要绕过代理的主机：本机回环 + 私有网段（写法遵循 JDK nonProxyHosts 规则，* 为通配符） */
    private static final String BYPASS_HOSTS =
            "localhost|127.*|::1|192.168.*|10.*|172.1[6-9].*|172.2[0-9].*|172.3[01].*|*.local";

    static
    {
        appendBypass("http.nonProxyHosts");
        appendBypass("socksNonProxyHosts");
    }

    private static void appendBypass(String key)
    {
        String current = System.getProperty(key);
        if (current == null || current.isBlank())
        {
            System.setProperty(key, BYPASS_HOSTS);
        }
        else if (!current.contains("192.168.*"))
        {
            System.setProperty(key, current + "|" + BYPASS_HOSTS);
        }
        log.info("内部调用代理绕过已生效：{} = {}", key, System.getProperty(key));
    }
}
