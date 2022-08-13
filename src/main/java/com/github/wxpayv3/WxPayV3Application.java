package com.github.wxpayv3;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class WxPayV3Application {

    public static void main(String[] args) {
        SpringApplication.run(WxPayV3Application.class, args);
    }

}
