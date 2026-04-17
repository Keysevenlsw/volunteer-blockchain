package com.gzu.volunteerblockchain;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@MapperScan("com.gzu.volunteerblockchain.mapper")
@EnableAsync
public class VolunteerBlockchainApplication {

    public static void main(String[] args) {
        SpringApplication.run(VolunteerBlockchainApplication.class, args);
    }

}
