
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

package br.com.sawcunhaos.foundation.utils.configuration.cache.properties;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import lombok.Data;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

import java.util.ArrayList;
import java.util.List;

/**
 * Propriedades de configuração para caches customizados
 * Suporta refresh dinâmico via Spring Cloud Config
 */
@ConditionalOnProperty(
        prefix = "spring.cache",
        name = "enable",
        havingValue = "true",
        matchIfMissing = true)
@Component
@RefreshScope
@ConfigurationProperties(prefix = "scos.cache")
@Validated
@Data
public class ScosCacheProperties {

    /**
     * Lista de configurações de caches customizados
     */
    @Valid
    private List<ScosCacheModel> caches = new ArrayList<>();

    /**
     * Tempo de vida padrão para caches em segundos
     * Default: 3600 segundos (1 hora)
     */
    @Min(value = 1, message = "TTL deve ser no mínimo 1 segundo")
    private long redisTimeToLive = 3600;

    /**
     * Prefixo global para todas as chaves de cache
     * Útil para separar ambientes ou aplicações no mesmo Redis
     * Exemplo: "myapp" gera chaves como "myapp:users:123"
     */
    private String keyPrefix;

    /**
     * Habilita compressão de valores grandes (> 1KB)
     * Reduz uso de memória no Redis mas aumenta CPU
     */
    private boolean enableCompression = false;

    /**
     * Tamanho mínimo em bytes para compressão (se habilitada)
     */
    @Min(value = 512, message = "Tamanho mínimo de compressão deve ser >= 512 bytes")
    private int compressionThreshold = 1024;

    /**
     * Adiciona um cache à lista de configurações
     */
    public void addCache(ScosCacheModel cache) {
        if (this.caches == null) {
            this.caches = new ArrayList<>();
        }
        this.caches.add(cache);
    }

    /**
     * Busca configuração de um cache específico pelo nome
     */
    public ScosCacheModel getCacheByName(String cacheName) {
        if (caches == null) {
            return null;
        }
        return caches.stream()
                .filter(cache -> cache.getName().equals(cacheName))
                .findFirst()
                .orElse(null);
    }

    /**
     * Verifica se existe configuração para um cache específico
     */
    public boolean hasCacheConfig(String cacheName) {
        return getCacheByName(cacheName) != null;
    }
}
