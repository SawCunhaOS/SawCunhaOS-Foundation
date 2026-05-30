
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

package br.com.sawcunhaos.foundation.privacy.config;

/**
 * Thrown when the masking configuration is invalid.
 *
 * <p>By design this is raised <strong>at load/startup time</strong> (unknown key, invalid strategy,
 * negative {@code keep-*}, multi-char {@code mask-char}, ReDoS-prone regex) rather than in production,
 * so a misconfiguration fails fast instead of silently leaking data.</p>
 */
public class PrivacyConfigException extends RuntimeException {

    public PrivacyConfigException(final String message) {
        super(message);
    }

    public PrivacyConfigException(final String message, final Throwable cause) {
        super(message, cause);
    }
}
