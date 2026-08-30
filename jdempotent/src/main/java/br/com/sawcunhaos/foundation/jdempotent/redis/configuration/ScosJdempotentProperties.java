
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

import jakarta.annotation.PostConstruct;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Story 3.10: namespace de prefixo de chave, configurável via propriedade Spring
 * ({@code scos.jdempotent.namespace}) em vez do antigo {@code System.getenv(APP_NAME)}, que
 * silenciosamente não aplicava prefixo algum quando a variável de ambiente não estava definida.
 * Sem valor default: a ausência da propriedade falha a inicialização do contexto Spring, em vez
 * de deixar aplicações diferentes colidirem chaves de idempotência no mesmo Redis.
 *
 * <p>Classe isolada, não absorvida por uma classe de propriedades geral do módulo: a Story 3.14
 * (migração {@code @Value} -> {@code @ConfigurationProperties}) ainda não estava implementada no
 * momento desta story — ver Dev Notes da Story 3.10 e Completion Notes.</p>
 *
 * <p>Revisão adversarial (patch): {@code @NotBlank}/{@code @Validated} só falha se houver um
 * provider JSR-380 (Bean Validation) no classpath do consumidor — este módulo declara
 * {@code hibernate-validator} apenas em {@code test} scope (ver {@code jdempotent/pom.xml}), então
 * um consumidor sem validator próprio passaria pela validação em silêncio. O
 * {@code @PostConstruct} abaixo é a checagem incondicional, independente de validator externo,
 * que garante a falha explícita exigida pela AC #1 em qualquer classpath.</p>
 */
@ConfigurationProperties(prefix = "scos.jdempotent")
@Validated
@Data
public class ScosJdempotentProperties {

    @NotBlank(message = "scos.jdempotent.namespace deve ser configurado (sem valor default) para "
            + "evitar colisão de chaves de idempotência entre aplicações que compartilham o mesmo Redis")
    private String namespace;

    @PostConstruct
    public void validateNamespace() {
        if (StringUtils.isBlank(namespace)) {
            throw new IllegalStateException(
                    "scos.jdempotent.namespace deve ser configurado (sem valor default) para "
                            + "evitar colisão de chaves de idempotência entre aplicações que compartilham o mesmo Redis");
        }
    }
}
