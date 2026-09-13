package com.amy.sunpalaceartspace;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import com.amy.common.security.annotation.EnableCustomConfig;
import com.amy.common.security.annotation.EnableRyFeignClients;
import org.springframework.context.annotation.Configuration;

@EnableAutoConfiguration
@Configuration
@EnableCustomConfig
@EnableRyFeignClients
@SpringBootApplication
public class AmyBizSunPalaceArtSpaceApplication {

    public static void main(String[] args) {
        SpringApplication.run(AmyBizSunPalaceArtSpaceApplication.class, args);
        System.out.println("(♥◠‿◠)ﾉﾞ  太阳宫艺术空间模块启动成功   ლ(´ڡ`ლ)ﾞ");
    }

}
