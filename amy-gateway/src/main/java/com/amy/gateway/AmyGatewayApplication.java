package com.amy.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration;

/**
 * 网关启动程序
 *
 * @author amy
 */
@SpringBootApplication(exclude = {DataSourceAutoConfiguration.class })
public class AmyGatewayApplication
{
    public static void main(String[] args)
    {
        SpringApplication.run(AmyGatewayApplication.class, args);
        System.out.println("(♥◠‿◠)ﾉﾞ 网关启动成功   ლ(´ڡ`ლ)ﾞ");
    }
}
