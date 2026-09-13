package com.amy.template;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import com.amy.common.security.annotation.EnableCustomConfig;
import com.amy.common.security.annotation.EnableRyFeignClients;

/**
 * 模块启动类（模板）。
 * <p>
 * 生成新模块时 __APP_CLASS__ / com.amy.template 会被替换为目标类名与包名。
 * </p>
 *
 * @author amy
 */
@EnableCustomConfig
@EnableRyFeignClients
@SpringBootApplication
public class AmyTemplateApplication
{
    public static void main(String[] args)
    {
        SpringApplication.run(AmyTemplateApplication.class, args);
    }
}
