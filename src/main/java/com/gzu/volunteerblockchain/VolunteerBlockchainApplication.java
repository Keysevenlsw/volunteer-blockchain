package com.gzu.volunteerblockchain;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.mybatis.spring.annotation.MapperScan;

@SpringBootApplication
@MapperScan("com.gzu.volunteerblockchain.mapper")
public class VolunteerBlockchainApplication {

    public static void main(String[] args) {
        SpringApplication.run(VolunteerBlockchainApplication.class, args);
    }

}
