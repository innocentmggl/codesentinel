package com.sentinel;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class CodeSentinelApplication {

    static void main(String[] args) {
        SpringApplication.run(CodeSentinelApplication.class, args);
    }
}
