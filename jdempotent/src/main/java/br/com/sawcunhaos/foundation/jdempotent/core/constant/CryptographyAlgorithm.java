
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

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Algoritmos de hash suportados para gerar a chave de idempotência.
 *
 * <p>O aspecto usa {@link #SHA256} tanto para a chave quanto para o {@code payloadHash}.</p>
 */
public enum CryptographyAlgorithm {

    /**
     * Usa o algoritmo de hash MD5.
     */
    MD5("MD5"),

    /**
     * Usa o algoritmo de hash SHA-256.
     */
    SHA256("SHA-256"),

    /**
     * Usa o algoritmo de hash SHA-1.
     */
    SHA1("SHA-1");

    private final String algorithm;

    CryptographyAlgorithm(String algorithm) {
        this.algorithm = algorithm;
    }

    /**
     * Nome JCA do algoritmo, aceito por {@link MessageDigest#getInstance(String)}.
     *
     * @return o nome do algoritmo (por exemplo {@code SHA-256})
     */
    public String value(){
        return algorithm;
    }

    /**
     * Cria um {@link MessageDigest} novo para o algoritmo (Story 3.12), evitando repetir o
     * {@code getInstance}/catch em cada chamador ({@code IdempotentAspect#execute} e
     * {@code IdempotencyKeyResolver#resolve}). Um digest por chamada: {@link MessageDigest} não é
     * thread-safe.
     *
     * @return um digest novo
     * @throws IllegalStateException se a JVM não suporta o algoritmo
     */
    public MessageDigest newDigest() {
        try {
            return MessageDigest.getInstance(algorithm);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("Algorithm not supported: " + algorithm, e);
        }
    }
}
