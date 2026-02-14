package br.com.sawcunhaos.foundation.jdempotent.redis.test.app;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.data.redis.autoconfigure.DataRedisAutoConfiguration;
import org.springframework.boot.data.redis.autoconfigure.DataRedisRepositoriesAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication(exclude = {
        DataRedisRepositoriesAutoConfiguration.class,
        DataRedisAutoConfiguration.class
})
@ComponentScan(
        basePackages={"*ignore*", "br.com.sawcunhaos.foundation"},
        excludeFilters={
            @ComponentScan.Filter(type = org.springframework.context.annotation.FilterType.REGEX, pattern = "br.com.sawcunhaos.foundation.jdempotent.core.*")
        }
)
public class JdempotentTestApplication {

    public static void main(String[] args) {
        SpringApplication.run(JdempotentTestApplication.class, args);
    }

}
