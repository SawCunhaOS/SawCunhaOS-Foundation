
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

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Plain Java representation of the masking configuration parsed from YAML.
 *
 * <p>This POJO is deliberately framework-free (no Spring annotations) so it can be produced by
 * {@link PrivacyConfigLoader} using SnakeYAML alone and consumed by the engine outside the SCOS layer.
 * It mirrors the {@code scos.privacy.masking} block of {@code privacy-masking.yml}.</p>
 *
 * <p>Instances are mutable only during parsing; the engine reads them once at build time and never mutates
 * them afterwards.</p>
 */
public final class PrivacyConfig {

    /** Key-based rules for HTTP headers. */
    private final List<RuleEntry> headers = new ArrayList<>();

    /** Key-based rules for JSON body fields. */
    private final List<RuleEntry> body = new ArrayList<>();

    /** Text-based rules (literal or regex) for free-text log messages. */
    private final List<RuleEntry> logPatterns = new ArrayList<>();

    /** Field names whose audit-trail values must be encrypted at rest. */
    private final Set<String> auditEncryptFields = new LinkedHashSet<>();

    /** Builtin packs to enable / disable. */
    private final BuiltinsConfig builtins = new BuiltinsConfig();

    public List<RuleEntry> getHeaders() {
        return headers;
    }

    public List<RuleEntry> getBody() {
        return body;
    }

    public List<RuleEntry> getLogPatterns() {
        return logPatterns;
    }

    public Set<String> getAuditEncryptFields() {
        return auditEncryptFields;
    }

    public BuiltinsConfig getBuiltins() {
        return builtins;
    }

    /** @return an empty configuration (no rules, no builtins). */
    public static PrivacyConfig empty() {
        return new PrivacyConfig();
    }

    /**
     * A single masking rule as declared in YAML. A rule is either key-based ({@link #key}), or text-based
     * via {@link #literal} or {@link #regex}. Validation of mutual exclusivity happens in the loader.
     */
    public static final class RuleEntry {
        private String key;
        private String literal;
        private String regex;
        private String value;
        private String strategy;
        private Integer keepFirst;
        private Integer keepLast;
        private String maskChar;
        private Boolean preserveLength;
        private Boolean maskLocal;
        private Boolean maskDomain;

        public String getKey() {
            return key;
        }

        public void setKey(final String key) {
            this.key = key;
        }

        public String getLiteral() {
            return literal;
        }

        public void setLiteral(final String literal) {
            this.literal = literal;
        }

        public String getRegex() {
            return regex;
        }

        public void setRegex(final String regex) {
            this.regex = regex;
        }

        public String getValue() {
            return value;
        }

        public void setValue(final String value) {
            this.value = value;
        }

        public String getStrategy() {
            return strategy;
        }

        public void setStrategy(final String strategy) {
            this.strategy = strategy;
        }

        public Integer getKeepFirst() {
            return keepFirst;
        }

        public void setKeepFirst(final Integer keepFirst) {
            this.keepFirst = keepFirst;
        }

        public Integer getKeepLast() {
            return keepLast;
        }

        public void setKeepLast(final Integer keepLast) {
            this.keepLast = keepLast;
        }

        public String getMaskChar() {
            return maskChar;
        }

        public void setMaskChar(final String maskChar) {
            this.maskChar = maskChar;
        }

        public Boolean getPreserveLength() {
            return preserveLength;
        }

        public void setPreserveLength(final Boolean preserveLength) {
            this.preserveLength = preserveLength;
        }

        public Boolean getMaskLocal() {
            return maskLocal;
        }

        public void setMaskLocal(final Boolean maskLocal) {
            this.maskLocal = maskLocal;
        }

        public Boolean getMaskDomain() {
            return maskDomain;
        }

        public void setMaskDomain(final Boolean maskDomain) {
            this.maskDomain = maskDomain;
        }
    }

    /** Enabled packs and individually disabled items (e.g. {@code br.titulo-eleitor}). */
    public static final class BuiltinsConfig {
        private final List<String> enabled = new ArrayList<>();
        private final Set<String> disabled = new LinkedHashSet<>();

        public List<String> getEnabled() {
            return enabled;
        }

        public Set<String> getDisabled() {
            return disabled;
        }
    }
}
