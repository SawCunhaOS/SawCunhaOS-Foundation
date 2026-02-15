
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

package br.com.sawcunhaos.foundation.utils.configuration.cache;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.interceptor.KeyGenerator;
import org.springframework.util.StringUtils;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Gerador de chaves customizado para cache
 * Formato da chave: ClassName::methodName::param1::param2::paramN
 * Características:
 * - Filtra tokens de autenticação (Bearer, JWT, Authorization)
 * - Trata valores nulos com segurança
 * - Usa hash para parâmetros muito grandes
 * - Performance otimizada com StringBuilder
 * - Seguro contra NPE
 * Exemplo de chave gerada:
 * "UserService::findById::123"
 * "ProductService::findByCategory::electronics::true"
 */
@Slf4j
public class ScosCacheKeyGenerator implements KeyGenerator {

    private static final String KEY_SEPARATOR = "::";
    private static final int MAX_PARAM_LENGTH = 200;

    // Palavras-chave que identificam tokens/senhas a serem filtradas
    private static final String[] SENSITIVE_KEYWORDS = {
            "bearer", "token", "authorization", "password", "secret", "jwt", "apikey", "api-key"
    };

    @Override
    public Object generate(Object target, Method method, Object... params) {
        try {
            StringBuilder keyBuilder = new StringBuilder();

            // 1. Nome da classe (simplificado)
            keyBuilder.append(getSimpleClassName(target));
            keyBuilder.append(KEY_SEPARATOR);

            // 2. Nome do método
            keyBuilder.append(method.getName());

            // 3. Parâmetros (filtrados e processados)
            if (params != null && params.length > 0) {
                String paramsKey = processParameters(params);
                if (StringUtils.hasText(paramsKey)) {
                    keyBuilder.append(KEY_SEPARATOR);
                    keyBuilder.append(paramsKey);
                }
            }

            String generatedKey = keyBuilder.toString();
            log.trace("Cache key gerada: {}", generatedKey);

            return generatedKey;

        } catch (Exception e) {
            log.error("Erro ao gerar chave de cache para {}#{}: {}",
                    target.getClass().getSimpleName(), method.getName(), e.getMessage());

            // Fallback: chave simples para garantir funcionamento
            return generateFallbackKey(target, method);
        }
    }

    /**
     * Processa parâmetros filtrando valores sensíveis
     */
    private String processParameters(Object[] params) {
        return Arrays.stream(params)
                .filter(Objects::nonNull)
                .filter(this::isNotSensitiveParam)
                .map(this::paramToString)
                .collect(Collectors.joining(KEY_SEPARATOR));
    }

    /**
     * Verifica se parâmetro não contém dados sensíveis
     */
    private boolean isNotSensitiveParam(Object param) {
        if (param == null) {
            return false;
        }

        String paramStr = param.toString().toLowerCase();

        // Filtra se contiver qualquer palavra sensível
        for (String keyword : SENSITIVE_KEYWORDS) {
            if (paramStr.contains(keyword)) {
                log.trace("Parâmetro sensível filtrado da chave de cache");
                return false;
            }
        }

        return true;
    }

    /**
     * Converte parâmetro para string de forma segura
     */
    private String paramToString(Object param) {
        if (param == null) {
            return "null";
        }

        String paramStr = param.toString();

        // Se parâmetro for muito grande, usa hash
        if (paramStr.length() > MAX_PARAM_LENGTH) {
            int hash = paramStr.hashCode();
            log.trace("Parâmetro muito longo, usando hash: {}", hash);
            return "hash_" + hash;
        }

        // Remove caracteres problemáticos para chaves
        return sanitizeKeyPart(paramStr);
    }

    /**
     * Remove/substitui caracteres problemáticos
     */
    private String sanitizeKeyPart(String part) {
        return part
                .replace(" ", "_")
                .replace(":", "-")
                .replace("\n", "")
                .replace("\r", "")
                .replace("\t", "")
                .trim();
    }

    /**
     * Obtém nome simplificado da classe
     */
    private String getSimpleClassName(Object target) {
        Class<?> clazz = target.getClass();

        // Remove sufixos de proxies do Spring
        String className = clazz.getSimpleName();
        if (className.contains("$$")) {
            className = className.substring(0, className.indexOf("$$"));
        }

        return className;
    }

    /**
     * Gera chave de fallback em caso de erro
     */
    private String generateFallbackKey(Object target, Method method) {
        return String.format("%s::%s::fallback_%d",
                getSimpleClassName(target),
                method.getName(),
                System.currentTimeMillis());
    }
}
