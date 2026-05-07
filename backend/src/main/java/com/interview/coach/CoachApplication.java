package com.interview.coach;

import com.interview.coach.config.PistonProperties;
import com.interview.coach.config.AiProperties;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@MapperScan("com.interview.coach.mapper")
@SpringBootApplication
@EnableConfigurationProperties({PistonProperties.class, AiProperties.class})
public class CoachApplication {

    public static void main(String[] args) {
        SpringApplication.run(CoachApplication.class, args);
    }
}
