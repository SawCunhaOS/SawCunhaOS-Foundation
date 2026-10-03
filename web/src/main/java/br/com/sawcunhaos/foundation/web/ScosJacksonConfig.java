
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

package br.com.sawcunhaos.foundation.web;

import org.springframework.boot.jackson.autoconfigure.JsonMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.core.StreamReadFeature;
import tools.jackson.databind.DeserializationFeature;

/**
 * Customiza o {@code JsonMapper} (Jackson 3) do Spring Boot: não falha em propriedade desconhecida
 * nem em {@code null} para tipo primitivo (usa o valor padrão), e inclui o trecho do fonte JSON na
 * localização dos erros de leitura.
 *
 * <p>Não está em {@code AutoConfiguration.imports}: é uma {@code @Configuration} comum, ativada
 * quando o pacote {@code br.com.sawcunhaos.foundation.web} entra no component scan da aplicação.</p>
 *
 * @since 1.2.0
 */
@Configuration
public class ScosJacksonConfig {

    // Tolerância na entrada: cliente que envia campo extra ou null em primitivo não recebe 400 por isso.
    @Bean
    JsonMapperBuilderCustomizer jacksonCustomizer() {
        return builder -> builder
                .disable(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES)
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                .enable(StreamReadFeature.INCLUDE_SOURCE_IN_LOCATION);
    }
}
