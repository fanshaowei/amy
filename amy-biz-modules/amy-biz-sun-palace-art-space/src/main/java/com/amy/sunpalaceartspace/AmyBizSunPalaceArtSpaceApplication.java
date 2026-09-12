package com.amy.sunpalaceartspace;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@EnableCustomConfig
@EnableRyFeignClients
@SpringBootApplication
public class AmyBizSunPalaceArtSpaceApplication {

    public static void main(String[] args) {
        SpringApplication.run(AmyBizSunPalaceArtSpaceApplication.class, args);
    }

}
