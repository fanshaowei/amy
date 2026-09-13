package com.amy.file;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration;

/**
 * 文件服务
 *
 * @author amy
 */
@SpringBootApplication(exclude = {DataSourceAutoConfiguration.class })
public class AmyFileApplication
{
    public static void main(String[] args)
    {
        SpringApplication.run(AmyFileApplication.class, args);
        System.out.println("(♥◠‿◠)ﾉﾞ  文件服务模块启动成功   ლ(´ڡ`ლ)ﾞ");
    }
}
