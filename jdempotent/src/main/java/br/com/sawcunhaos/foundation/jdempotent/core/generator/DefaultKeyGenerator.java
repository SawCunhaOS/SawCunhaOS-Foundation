
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

import br.com.sawcunhaos.foundation.jdempotent.core.constant.EnvironmentVariableUtils;
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

    private final String appName;

    public DefaultKeyGenerator() {
        appName = System.getenv(EnvironmentVariableUtils.APP_NAME);
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

        if (!StringUtils.isBlank(appName)) {
            builder.append(appName);
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
