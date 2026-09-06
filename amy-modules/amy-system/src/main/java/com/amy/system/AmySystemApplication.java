package com.amy.system;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import com.amy.common.security.annotation.EnableCustomConfig;
import com.amy.common.security.annotation.EnableRyFeignClients;

/**
 * 系统模块
 *
 * @author amy
 */
@EnableCustomConfig
@EnableRyFeignClients
@SpringBootApplication
public class AmySystemApplication
{
    public static void main(String[] args)
    {
        SpringApplication.run(AmySystemApplication.class, args);
        System.out.println("(♥◠‿◠)ﾉﾞ  系统模块启动成功   ლ(´ڡ`ლ)ﾞ  \n" +
                " .-------.       ____     __        \n" +
                " |  _ _   \\      \\   \\   /  /    \n" +
                " | ( ' )  |       \\  _. /  '       \n" +
                " |(_ o _) /        _( )_ .'         \n" +
                " | (_,_).' __  ___(_ o _)'          \n" +
                " |  |\\ \\  |  ||   |(_,_)'         \n" +
                " |  | \\ `'   /|   `-'  /           \n" +
                " |  |  \\    /  \\      /           \n" +
                " ''-'   `'-'    `-..-'              ");
    }
}
