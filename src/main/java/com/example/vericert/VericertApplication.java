package com.example.vericert;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@ConfigurationPropertiesScan(basePackages = "com.example.vericert")
public class VericertApplication {

    public static void main(String[] args) {
        SpringApplication.run(VericertApplication.class, args);
    }

}

