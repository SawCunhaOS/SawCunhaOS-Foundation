
/*
 *
 *  * Copyright 2026 SawCunha Open System - SawCunhaOS-Foundation
 *  *
 *  * Licensed under the Apache License, Version 2.0 (the "License");
 *  * you may not use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  *     http://www.apache.org/licenses/LICENSE-2.0
 *
 */

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
