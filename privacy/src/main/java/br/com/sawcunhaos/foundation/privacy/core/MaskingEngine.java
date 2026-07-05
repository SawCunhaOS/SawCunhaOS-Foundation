
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

package br.com.sawcunhaos.foundation.privacy.core;

import br.com.sawcunhaos.foundation.privacy.config.BuiltinPackLoader;
import br.com.sawcunhaos.foundation.privacy.config.PrivacyConfig;
import br.com.sawcunhaos.foundation.privacy.config.PrivacyConfigLoader;
import br.com.sawcunhaos.foundation.privacy.crypto.JasyptCryptoKeyProvider;
import br.com.sawcunhaos.foundation.privacy.crypto.ScosCryptoKeyProvider;
import br.com.sawcunhaos.foundation.privacy.crypto.ScosFieldCipher;
import br.com.sawcunhaos.foundation.privacy.model.DataMask;
import br.com.sawcunhaos.foundation.privacy.model.MaskStrategy;
import br.com.sawcunhaos.foundation.privacy.specification.DataMaskingValues;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;

/**
 * High-performance, immutable, thread-safe masking engine.
 *
 * <p>One instance is built once at startup and shared as a singleton across all threads. It holds no
 * per-request state and uses no synchronization, so it scales linearly with cores. Rules are flattened in
 * the precedence order <em>builtins &rarr; YAML &rarr; SPI</em> into immutable lookup structures:</p>
 *
 * <ul>
 *     <li><strong>key-based</strong> ({@link #maskStructured}) — an immutable {@code Map} keyed by the
 *         lower-cased field name, giving O(1) per field;</li>
 *     <li><strong>text-based</strong> ({@link #maskText}) — literals matched by a single Aho-Corasick pass
 *         and regexes applied only after a cheap pre-screen, with a zero-allocation fast-path that returns
 *         the same {@code String} when no PII trigger is present.</li>
 * </ul>
 *
 * <p>The engine is usable <strong>outside the SCOS layer</strong>: {@link #fromYaml(Path)} builds it from a
 * YAML file with SnakeYAML alone, without Spring.</p>
 */
public final class MaskingEngine {

    private final Map<String, DataMask> headerRules;
    private final Map<String, DataMask> bodyRules;
    private final List<TextRule> textRules;
    private final AhoCorasick literalAutomaton;
    private final List<TextRule> literalRules;
    private final Set<String> auditEncryptFields;
    private final MaskStrategies strategies;
    private final ScosFieldCipher fieldCipher;
    private final int maxPayloadBytes;

    /** Reused per-thread matcher buffer to avoid allocating on the hot path. */
    private static final ThreadLocal<StringBuilder> SCRATCH = ThreadLocal.withInitial(() -> new StringBuilder(256));

    private MaskingEngine(final Builder b) {
        this.headerRules = Collections.unmodifiableMap(b.headerRules);
        this.bodyRules = Collections.unmodifiableMap(b.bodyRules);
        this.textRules = List.copyOf(b.regexRules);
        this.literalRules = List.copyOf(b.literalRules);
        this.literalAutomaton = AhoCorasick.build(b.literalRules.stream().map(r -> r.needle).toList());
        this.auditEncryptFields = Set.copyOf(b.auditEncryptFields);
        this.fieldCipher = b.fieldCipher;
        this.strategies = new MaskStrategies(b.keyProvider, b.fieldCipher, b.strict);
        this.maxPayloadBytes = b.maxPayloadKb * 1024;
    }

    // ---------------------------------------------------------------------
    // Factories
    // ---------------------------------------------------------------------

    /**
     * Builds an engine from a YAML file, with no Spring dependency. This is the entry point for using the
     * masking engine outside the SCOS layer.
     *
     * @param yamlPath path to a {@code privacy-masking.yml}-shaped file
     * @return a ready, immutable engine
     */
    public static MaskingEngine fromYaml(final Path yamlPath) {
        try (InputStream in = Files.newInputStream(yamlPath)) {
            return fromYaml(in);
        } catch (IOException ex) {
            throw new IllegalArgumentException("Failed to read masking YAML: " + yamlPath, ex);
        }
    }

    /**
     * Builds an engine from a YAML stream, with no Spring dependency.
     *
     * @param yaml a {@code privacy-masking.yml}-shaped stream (closed by the caller)
     * @return a ready, immutable engine
     */
    public static MaskingEngine fromYaml(final InputStream yaml) {
        final PrivacyConfig config = PrivacyConfigLoader.parseAndValidate(yaml);
        return builder().config(config).build();
    }

    /** @return a new {@link Builder}. */
    public static Builder builder() {
        return new Builder();
    }

    // ---------------------------------------------------------------------
    // Public API
    // ---------------------------------------------------------------------

    /**
     * Masks a single value by field key (headers, body fields, MDC, audit). O(1) lookup; values whose key
     * has no rule are returned unchanged.
     *
     * @param key the field name (case-insensitive)
     * @param value the value to mask
     * @return the masked value, or the original when no rule matches
     */
    public String maskStructured(final String key, final String value) {
        if (key == null || value == null) {
            return value;
        }
        final DataMask rule = bodyRules.get(key.toLowerCase(Locale.ROOT));
        return rule != null ? strategies.apply(rule, value) : value;
    }

    /**
     * Masks a header value by header name.
     *
     * @param key the header name (case-insensitive)
     * @param value the header value
     * @return the masked value, or the original when no rule matches
     */
    public String maskHeader(final String key, final String value) {
        if (key == null || value == null) {
            return value;
        }
        final DataMask rule = headerRules.get(key.toLowerCase(Locale.ROOT));
        return rule != null ? strategies.apply(rule, value) : value;
    }

    /**
     * Masks free-text (e.g. a rendered log message). Uses a zero-allocation fast-path: if the message has
     * no trigger character (digit or {@code @}), the same {@code String} reference is returned without any
     * matching work. Otherwise literals are matched by a single Aho-Corasick pass and regex rules are
     * applied (each with its pre-compiled {@link java.util.regex.Pattern} and a check-digit validator when
     * configured).
     *
     * @param message the text to mask
     * @return the masked text, or the same reference when nothing matches
     */
    public String maskText(final String message) {
        if (message == null || message.isEmpty()) {
            return message;
        }
        final String capped = cap(message);
        if (!hasTrigger(capped) && literalAutomaton == null) {
            return message; // fast-path: dominant case (lines without PII)
        }

        String result = capped;
        // Literal needles (Aho-Corasick) first.
        if (literalAutomaton != null) {
            result = applyLiterals(result);
        }
        // Regex rules: apply only those whose pre-screen could match.
        for (final TextRule rule : textRules) {
            result = applyRegex(result, rule);
        }
        return result;
    }

    /**
     * @return the immutable set of field names to encrypt at rest in the audit trail
     */
    public Set<String> auditEncryptFields() {
        return auditEncryptFields;
    }

    /**
     * @return the configured field cipher (may be {@code null} when crypto is not configured)
     */
    public ScosFieldCipher fieldCipher() {
        return fieldCipher;
    }

    // ---------------------------------------------------------------------
    // Text helpers
    // ---------------------------------------------------------------------

    private String applyLiterals(final String text) {
        final List<AhoCorasick.Match> matches = literalAutomaton.search(text);
        if (matches.isEmpty()) {
            return text;
        }
        // Rebuild from the end so positions stay valid.
        final StringBuilder sb = new StringBuilder(text);
        matches.sort((a, c) -> Integer.compare(c.end(), a.end()));
        for (final AhoCorasick.Match m : matches) {
            final int start = m.end() - m.length();
            final TextRule rule = literalRules.get(m.needleIndex());
            final String matched = text.substring(start, m.end());
            sb.replace(start, m.end(), strategies.apply(rule.mask, matched));
        }
        return sb.toString();
    }

    private String applyRegex(final String text, final TextRule rule) {
        final Matcher matcher = rule.mask.compiled().matcher(text);
        if (!matcher.find()) {
            return text;
        }
        final StringBuilder out = SCRATCH.get();
        out.setLength(0);
        matcher.reset();
        while (matcher.find()) {
            final String matched = matcher.group();
            final String replacement = (rule.validator != null
                && !DocumentValidators.passes(rule.validator, matched))
                ? matched // failed the check-digit: not real PII, leave as-is
                : strategies.apply(rule.mask, matched);
            matcher.appendReplacement(out, Matcher.quoteReplacement(replacement == null ? "" : replacement));
        }
        matcher.appendTail(out);
        return out.toString();
    }

    /** Cheap single scan for a trigger character. Absence means the fast-path can skip all matching. */
    private static boolean hasTrigger(final String s) {
        for (int i = 0; i < s.length(); i++) {
            final char c = s.charAt(i);
            if (c == '@' || (c >= '0' && c <= '9')) {
                return true;
            }
        }
        return false;
    }

    private String cap(final String message) {
        if (maxPayloadBytes <= 0 || message.length() <= maxPayloadBytes) {
            return message;
        }
        return message.substring(0, maxPayloadBytes) + "...[truncated]";
    }

    // ---------------------------------------------------------------------
    // Builder (flattens precedence builtins -> YAML -> SPI)
    // ---------------------------------------------------------------------

    /** A text rule pairs a compiled mask with an optional check-digit validator and a literal needle. */
    private record TextRule(DataMask mask, String validator, String needle) {
    }

    /**
     * Builder that flattens every source into immutable lookup structures exactly once.
     */
    public static final class Builder {
        private final Map<String, DataMask> headerRules = new LinkedHashMap<>();
        private final Map<String, DataMask> bodyRules = new LinkedHashMap<>();
        private final List<TextRule> regexRules = new ArrayList<>();
        private final List<TextRule> literalRules = new ArrayList<>();
        private final List<String> auditEncryptFields = new ArrayList<>();
        private ScosCryptoKeyProvider keyProvider;
        private ScosFieldCipher fieldCipher;
        private boolean strict;
        private int maxPayloadKb = 64;
        private PrivacyConfig config = PrivacyConfig.empty();
        private final List<DataMaskingValues> spi = new ArrayList<>();

        /** @param strictMode whether missing crypto is a startup error; default {@code false} */
        public Builder strict(final boolean strictMode) {
            this.strict = strictMode;
            return this;
        }

        /** @param kb max payload size in KB before masking truncates the input */
        public Builder maxPayloadKb(final int kb) {
            this.maxPayloadKb = kb;
            return this;
        }

        /** @param provider the crypto key provider (enables {@code hash}/{@code encrypt}) */
        public Builder keyProvider(final ScosCryptoKeyProvider provider) {
            this.keyProvider = provider;
            return this;
        }

        /** @param config the parsed YAML configuration */
        public Builder config(final PrivacyConfig config) {
            this.config = config != null ? config : PrivacyConfig.empty();
            return this;
        }

        /** @param values an optional SPI source added on top of the YAML */
        public Builder addSpi(final DataMaskingValues values) {
            if (values != null) {
                this.spi.add(values);
            }
            return this;
        }

        /**
         * Flattens builtins, YAML and SPI (in that precedence) and constructs the immutable engine.
         *
         * @return a ready engine
         */
        public MaskingEngine build() {
            if (keyProvider == null) {
                final String secret = System.getenv("SCOS_PRIVACY_CRYPTO_SECRET");
                if (secret != null && !secret.isBlank()) {
                    keyProvider = new JasyptCryptoKeyProvider(secret);
                }
            }
            if (keyProvider != null) {
                fieldCipher = new ScosFieldCipher(keyProvider);
            }

            // 1) builtins
            for (final BuiltinPackLoader.BuiltinPattern bp : BuiltinPackLoader.resolve(config)) {
                addTextRule(bp.mask(), bp.validator());
            }
            // 2) YAML
            applyConfig(config);
            // 3) SPI override (wins by key)
            for (final DataMaskingValues v : spi) {
                v.headersValue().forEach(d -> headerRules.put(lower(d.key()), normalize(d)));
                v.bodyValue().forEach(d -> bodyRules.put(lower(d.key()), normalize(d)));
                v.logPatterns().forEach(d -> addTextRule(normalize(d), null));
                auditEncryptFields.addAll(v.auditEncryptFields());
            }
            return new MaskingEngine(this);
        }

        private void applyConfig(final PrivacyConfig cfg) {
            cfg.getHeaders().forEach(e -> {
                final DataMask m = toMask(e);
                headerRules.put(lower(m.key()), m);
            });
            cfg.getBody().forEach(e -> {
                final DataMask m = toMask(e);
                bodyRules.put(lower(m.key()), m);
            });
            cfg.getLogPatterns().forEach(e -> addTextRule(toMask(e), null));
            auditEncryptFields.addAll(cfg.getAuditEncryptFields());
        }

        private void addTextRule(final DataMask mask, final String validator) {
            if (mask.literal() != null) {
                literalRules.add(new TextRule(mask, validator, mask.literal()));
            } else if (mask.compiled() != null) {
                regexRules.add(new TextRule(mask, validator, null));
            }
        }

        private DataMask toMask(final PrivacyConfig.RuleEntry e) {
            final MaskStrategy strategy = MaskStrategy.from(e.getStrategy());
            final char maskChar = (e.getMaskChar() != null && !e.getMaskChar().isEmpty())
                ? e.getMaskChar().charAt(0) : DataMask.DEFAULT_MASK_CHAR;
            final DataMask.DataMaskBuilder b = DataMask.builder()
                .key(e.getKey())
                .literal(e.getLiteral())
                .regex(e.getRegex())
                .strategy(strategy)
                .newValue(e.getValue() != null ? e.getValue() : DataMask.DEFAULT_VALUE)
                .keepFirst(e.getKeepFirst() != null ? e.getKeepFirst() : 0)
                .keepLast(e.getKeepLast() != null ? e.getKeepLast() : 0)
                .maskChar(maskChar)
                .preserveLength(e.getPreserveLength() == null || e.getPreserveLength());
            if (e.getRegex() != null) {
                b.compiled(java.util.regex.Pattern.compile(e.getRegex()));
            }
            return b.build();
        }

        private DataMask normalize(final DataMask d) {
            // SPI rules may omit the new fields; ensure sane defaults and pre-compile regex.
            final DataMask.DataMaskBuilder b = DataMask.builder()
                .key(d.key())
                .literal(d.literal())
                .regex(d.regex())
                .strategy(d.strategy() != null ? d.strategy() : MaskStrategy.FIXED)
                .newValue(d.newValue() != null ? d.newValue() : DataMask.DEFAULT_VALUE)
                .keepFirst(d.keepFirst())
                .keepLast(d.keepLast())
                .maskChar(d.maskChar() == 0 ? DataMask.DEFAULT_MASK_CHAR : d.maskChar())
                .preserveLength(d.preserveLength());
            if (d.regex() != null && d.compiled() == null) {
                b.compiled(java.util.regex.Pattern.compile(d.regex()));
            } else {
                b.compiled(d.compiled());
            }
            return b.build();
        }

        private static String lower(final String s) {
            return s == null ? "" : s.toLowerCase(Locale.ROOT);
        }
    }
}
