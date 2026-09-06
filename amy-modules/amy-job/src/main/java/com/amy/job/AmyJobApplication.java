package com.amy.job;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import com.amy.common.security.annotation.EnableCustomConfig;
import com.amy.common.security.annotation.EnableRyFeignClients;

/**
 * 定时任务
 *
 * @author amy
 */
@EnableCustomConfig
@EnableRyFeignClients
@SpringBootApplication
public class AmyJobApplication
{
    public static void main(String[] args)
    {
        SpringApplication.run(AmyJobApplication.class, args);
        System.out.println("(♥◠‿◠)ﾉﾞ  定时任务模块启动成功   ლ(´ڡ`ლ)ﾞ");
    }
}
