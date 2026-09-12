package com.amy.wechat;

import com.amy.common.security.annotation.EnableCustomConfig;
import com.amy.common.security.annotation.EnableRyFeignClients;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Configuration;

@EnableAutoConfiguration
@Configuration
@EnableCustomConfig
@EnableRyFeignClients
@SpringBootApplication
public class AmyWechatApplication {
    public static void main(String[] args) {
        SpringApplication.run(AmyWechatApplication.class, args);
        System.out.println("(♥◠‿◠)ﾉﾞ  微信模块启动成功   ლ(´ڡ`ლ)ﾞ");
    }
}
