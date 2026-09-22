package com.cyys.application;

import org.apache.ibatis.annotations.Mapper;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "com.cyys")
@MapperScan(basePackages = "com.cyys", annotationClass = Mapper.class)
public class CyysApplication {
    public static void main(String[] args) {
        SpringApplication.run(CyysApplication.class, args);
    }
}
