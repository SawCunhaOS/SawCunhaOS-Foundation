
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

package br.com.sawcunhaos.foundation.jdempotent.core.generator;

import br.com.sawcunhaos.foundation.jdempotent.core.model.IdempotencyKey;
import br.com.sawcunhaos.foundation.jdempotent.core.model.IdempotentRequestWrapper;
import org.apache.commons.lang3.StringUtils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;

/**
 *
 *
 */
public class DefaultKeyGenerator implements KeyGenerator {

    private final String namespace;

    /**
     * No-namespace generator (no prefix is added to the generated key). Used by callers that
     * build an {@code IdempotentAspect} programmatically, outside of Spring auto-configuration
     * (e.g. tests). Does not read any environment variable — Story 3.10 removed the previous
     * silent {@code System.getenv(APP_NAME)} fallback in favor of an explicit, Spring-configured
     * namespace (see {@link #DefaultKeyGenerator(String)}).
     */
    public DefaultKeyGenerator() {
        this(null);
    }

    /**
     * @param namespace prefix namespace resolved by the caller (e.g. from a Spring
     *                  {@code @ConfigurationProperties} bean); may be {@code null}/blank, in which
     *                  case no prefix is added.
     */
    public DefaultKeyGenerator(String namespace) {
        this.namespace = namespace;
    }

    /**
     *
     * Generates a idempotent key for incoming event
     *
     * @param requestObject
     * @param listenerName
     * @param builder
     * @param messageDigest
     * @return
     */
    public IdempotencyKey generateIdempotentKey(IdempotentRequestWrapper requestObject, String listenerName, StringBuilder builder, MessageDigest messageDigest) {
        messageDigest.update(requestObject.toString().getBytes(StandardCharsets.UTF_8));
        byte[] digest = messageDigest.digest();

        if (!StringUtils.isBlank(namespace)) {
            builder.append(namespace);
            builder.append("-");
        }

        if (!StringUtils.isBlank(listenerName)) {
            builder.append(listenerName);
            builder.append("-");
        }

        builder.append(HexFormat.of().formatHex(digest));

        return new IdempotencyKey(builder.toString());
    }
}
