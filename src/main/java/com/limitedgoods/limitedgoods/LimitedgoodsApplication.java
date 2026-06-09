package com.limitedgoods.limitedgoods;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class LimitedgoodsApplication {

    public static void main(String[] args) {
        SpringApplication.run(LimitedgoodsApplication.class, args);
    }

}
