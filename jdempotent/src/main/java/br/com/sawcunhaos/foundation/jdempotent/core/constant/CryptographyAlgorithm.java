
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

/**
 *
 * Supported hash algorithms to generate idempotency key
 *
 */
public enum CryptographyAlgorithm {

    /**
     * use md5 hash algorithm
     */
    MD5("MD5"),

    /**
     * use SHA-256 hash algorithm
     */
    SHA256("SHA-256"),

    /**
     * use SHA-1 hash algorithm
     */
    SHA1("SHA-1");

    private final String algorithm;

    CryptographyAlgorithm(String algorithm) {
        this.algorithm = algorithm;
    }

    public String value(){
        return algorithm;
    }
}
