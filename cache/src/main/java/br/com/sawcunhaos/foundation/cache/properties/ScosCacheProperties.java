
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

package br.com.sawcunhaos.foundation.cache.properties;

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
 * Propriedades {@code scos.cache.*}: TTL padrão, prefixo de chave e lista de caches com TTL
 * próprio. Validada com Bean Validation e recarregável via {@code @RefreshScope}.
 *
 * <p>Atenção: só {@code redisTimeToLive}, {@code keyPrefix} e {@code caches} são consumidos por
 * {@link br.com.sawcunhaos.foundation.cache.ScosCacheConfiguration}; {@code enableCompression} e
 * {@code compressionThreshold} hoje não têm efeito.</p>
 */
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
     * Adiciona um cache à lista de configurações (cria a lista se estiver {@code null}).
     *
     * @param cache configuração do cache a adicionar
     */
    public void addCache(ScosCacheModel cache) {
        if (this.caches == null) {
            this.caches = new ArrayList<>();
        }
        this.caches.add(cache);
    }

    /**
     * Busca a configuração de um cache pelo nome.
     *
     * @param cacheName nome do cache
     * @return a configuração, ou {@code null} se não houver
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
     * Indica se há configuração para o cache.
     *
     * @param cacheName nome do cache
     * @return {@code true} se {@link #getCacheByName(String)} achar uma entrada
     */
    public boolean hasCacheConfig(String cacheName) {
        return getCacheByName(cacheName) != null;
    }
}
