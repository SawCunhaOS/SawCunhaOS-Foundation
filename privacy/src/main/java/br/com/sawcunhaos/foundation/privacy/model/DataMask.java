
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

package br.com.sawcunhaos.foundation.privacy.model;

import lombok.Builder;

import java.util.regex.Pattern;

/**
 * Immutable description of a single masking rule.
 *
 * <p>This record is the unit consumed by the masking engine for both key-based rules
 * ({@code headers}/{@code body}) and text-based rules ({@code log-patterns}). It is fully
 * immutable and safe to share across threads.</p>
 *
 * <p><strong>Performance:</strong> when {@link #regex()} is set, the {@link Pattern} is
 * <strong>pre-compiled</strong> at build time and stored in {@link #compiled()} so the hot path never
 * recompiles. Use the {@link #ofRegex} / {@link #ofLiteral} factories so the {@code compiled} field is
 * populated consistently.</p>
 *
 * @param key the field key for key-based rules, or the canonical name of a builtin pattern; {@code null}
 *            for anonymous text-based rules
 * @param newValue the literal replacement used by the {@link MaskStrategy#FIXED} strategy (default {@code "***"})
 * @param strategy the masking strategy to apply (never {@code null})
 * @param literal the literal needle for text matching (Aho-Corasick), or {@code null} when {@link #regex()} is used
 * @param regex the regular expression source for text matching, or {@code null} for literal/key-based rules
 * @param compiled the pre-compiled {@link Pattern} for {@link #regex()} (never recompiled on the hot path)
 * @param keepFirst characters preserved at the start for {@link MaskStrategy#PARTIAL}
 * @param keepLast characters preserved at the end for {@link MaskStrategy#PARTIAL}
 * @param maskChar the character used to fill the masked region (default {@code '*'})
 * @param preserveLength whether {@link MaskStrategy#PARTIAL} preserves the original length
 */
@Builder
public record DataMask(
    String key,
    String newValue,
    MaskStrategy strategy,
    String literal,
    String regex,
    Pattern compiled,
    int keepFirst,
    int keepLast,
    char maskChar,
    boolean preserveLength
) {

    /** Default replacement value used by the {@code fixed} strategy. */
    public static final String DEFAULT_VALUE = "***";

    /** Default character used to fill masked regions. */
    public static final char DEFAULT_MASK_CHAR = '*';

    /**
     * Creates a key-based rule (matched by field name).
     *
     * @param key the field key (required)
     * @param strategy the strategy to apply
     * @param newValue the replacement for {@code fixed}; {@link #DEFAULT_VALUE} when {@code null}
     * @return an immutable rule
     */
    public static DataMask ofKey(final String key, final MaskStrategy strategy, final String newValue) {
        return DataMask.builder()
            .key(key)
            .strategy(strategy == null ? MaskStrategy.FIXED : strategy)
            .newValue(newValue == null ? DEFAULT_VALUE : newValue)
            .maskChar(DEFAULT_MASK_CHAR)
            .preserveLength(true)
            .build();
    }

    /**
     * Creates a literal text rule (matched by Aho-Corasick, no regex compilation).
     *
     * @param literal the literal needle (required)
     * @param strategy the strategy to apply over the matched text
     * @param newValue the replacement for {@code fixed}; {@link #DEFAULT_VALUE} when {@code null}
     * @return an immutable rule
     */
    public static DataMask ofLiteral(final String literal, final MaskStrategy strategy, final String newValue) {
        return DataMask.builder()
            .literal(literal)
            .strategy(strategy == null ? MaskStrategy.FIXED : strategy)
            .newValue(newValue == null ? DEFAULT_VALUE : newValue)
            .maskChar(DEFAULT_MASK_CHAR)
            .preserveLength(true)
            .build();
    }

    /**
     * Creates a regex text rule with the {@link Pattern} pre-compiled.
     *
     * @param key canonical name (useful for builtins / overriding by key); may be {@code null}
     * @param regex the regular expression source (required)
     * @param strategy the strategy to apply over the matched group
     * @param newValue the replacement for {@code fixed}; {@link #DEFAULT_VALUE} when {@code null}
     * @return an immutable rule with {@link #compiled()} populated
     */
    public static DataMask ofRegex(final String key, final String regex, final MaskStrategy strategy,
                                   final String newValue) {
        return DataMask.builder()
            .key(key)
            .regex(regex)
            .compiled(Pattern.compile(regex))
            .strategy(strategy == null ? MaskStrategy.FIXED : strategy)
            .newValue(newValue == null ? DEFAULT_VALUE : newValue)
            .maskChar(DEFAULT_MASK_CHAR)
            .preserveLength(true)
            .build();
    }
}
