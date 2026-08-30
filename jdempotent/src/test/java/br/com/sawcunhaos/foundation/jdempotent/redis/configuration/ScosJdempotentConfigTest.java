
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

package br.com.sawcunhaos.foundation.jdempotent.redis.configuration;

import br.com.sawcunhaos.foundation.jdempotent.core.aspect.IdempotentAspect;
import br.com.sawcunhaos.foundation.jdempotent.core.callback.ErrorConditionalCallback;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.data.redis.core.RedisTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Cobre a Story 3.4: {@code ScosJdempotentConfig} declarava dois métodos {@code @Bean} que
 * retornam {@code IdempotentAspect} sem nenhum deles ter {@code @ConditionalOnMissingBean} —
 * quando a aplicação consumidora registra um {@code ErrorConditionalCallback}, ambos os métodos
 * ficavam elegíveis e o Spring criava dois beans {@code IdempotentAspect} no mesmo contexto.
 */
class ScosJdempotentConfigTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withBean(ScosJdempotentRedisProperties.class, ScosJdempotentRedisProperties::new)
            .withBean("JdempotentRedisTemplate", RedisTemplate.class, () -> mock(RedisTemplate.class))
            .withPropertyValues("scos.jdempotent.namespace=test-app")
            .withUserConfiguration(ScosJdempotentConfig.class);

    @Test
    void naoDuplicaIdempotentAspectQuandoErrorConditionalCallbackEstaRegistrado() {
        runner.withBean(ErrorConditionalCallback.class, () -> mock(ErrorConditionalCallback.class))
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context.getBeansOfType(IdempotentAspect.class)).hasSize(1);
                });
    }
}
