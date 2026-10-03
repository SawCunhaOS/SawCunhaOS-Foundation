
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

package br.com.sawcunhaos.foundation.jdempotent.core.constant;

import java.util.Arrays;

/**
 * Tipos de repositório de dados reconhecidos por nome de configuração.
 *
 * <p>Apenas {@link #REDIS} e {@link #INMEMORY} têm implementação neste módulo; {@link #HAZELCAST}
 * é um valor reservado sem repositório correspondente.</p>
 */
public enum RepositoryType {

    /**
     * Valor de configuração do Redis.
     */
    REDIS("redis"),

    /**
     * Valor de configuração do Hazelcast (reservado, sem implementação neste módulo).
     */
    HAZELCAST("hazelcast"),

    /**
     * Configuração padrão (repositório em memória).
     */
    INMEMORY("default");

    private String value;

    RepositoryType(String value) {
        this.value = value;
    }

    /**
     * Valor de configuração associado ao tipo.
     *
     * @return o valor de configuração (por exemplo {@code redis})
     */
    public String value() {
        return value;
    }

    /**
     * Resolve o tipo pelo valor de configuração, ignorando maiúsculas e minúsculas.
     *
     * @param repositoryName valor de configuração (não nulo)
     * @return o tipo correspondente, ou {@link #INMEMORY} se nenhum casar
     */
    public static RepositoryType getRepositoryTypeByValue(String repositoryName){
        return Arrays.stream(values()).filter(repositoryType -> repositoryName.equalsIgnoreCase(repositoryType.value)).findAny().orElse(RepositoryType.INMEMORY);
    }
}
