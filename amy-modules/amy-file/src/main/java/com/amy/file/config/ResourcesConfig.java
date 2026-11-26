package com.amy.file.config;

import java.io.File;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 静态资源映射配置。
 *
 * <p>映射路径完全由 Nacos {@code file.prefix} 决定：改配置即可换前缀，
 * 没有第二处硬编码路径。上传接口返回的 key 也用同一个前缀，二者天然对齐。</p>
 *
 * @author amy
 */
@Configuration
public class ResourcesConfig implements WebMvcConfigurer
{
    /**
     * 上传文件存储在本地的根路径
     */
    @Value("${file.path}")
    private String localFilePath;

    /**
     * 资源映射路径前缀（配置 file.prefix）
     */
    @Value("${file.prefix}")
    public String localFilePrefix;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry)
    {
        /** 本地文件上传路径 */
        String location = "file:" + localFilePath + File.separator;
        registry.addResourceHandler(localFilePrefix + "/**").addResourceLocations(location);
    }
    
    /**
     * 开启跨域
     */
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping(localFilePrefix + "/**")
                // 设置允许跨域请求的域名
                .allowedOrigins("*")
                // 设置允许的方法
                .allowedMethods("GET");
    }
}
