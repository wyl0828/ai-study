package com.interview.coach;

import com.interview.coach.config.PistonProperties;
import com.interview.coach.config.AiProperties;
import com.interview.coach.config.EmbeddingProperties;
import com.interview.coach.config.ProblemCacheProperties;
import com.interview.coach.config.RagVectorProperties;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@MapperScan("com.interview.coach.mapper")
@SpringBootApplication
@EnableConfigurationProperties({
        PistonProperties.class,
        AiProperties.class,
        EmbeddingProperties.class,
        ProblemCacheProperties.class,
        RagVectorProperties.class
})
public class CoachApplication {

    public static void main(String[] args) {
        SpringApplication.run(CoachApplication.class, args);
    }
}
