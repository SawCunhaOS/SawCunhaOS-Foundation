
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

import java.util.regex.Pattern;

/**
 * Build-time guard against regular expressions prone to catastrophic backtracking (ReDoS).
 *
 * <p>The masking engine runs on the hot path (every request, every log line). A single ReDoS-prone
 * pattern could stall threads under crafted input. This guard rejects the most common dangerous shapes
 * at startup so the failure surfaces during build/boot, never in production.</p>
 *
 * <p>It is intentionally conservative: it flags nested quantifiers (e.g. {@code (a+)+}, {@code (a*)*},
 * {@code (a+)*}) and quantified alternations of overlapping branches. It does not attempt a full
 * formal analysis; possessive quantifiers ({@code *+}, {@code ++}) and atomic groups are considered safe.</p>
 */
public final class ReDoSGuard {

    // Nested quantifier inside a group that is itself quantified: (X+)+, (X*)*, (X+)*, (X*)+, with
    // optional possessive/lazy markers. Possessive outer quantifier (+) right after ) is excluded.
    private static final Pattern NESTED_QUANTIFIER =
            Pattern.compile("\\([^()]*?(?<!\\\\)[+*][^()]*?\\)[*+?](?![+])");

    // Quantifier applied directly to a quantifier, e.g. a+* or a*+ outside possessive intent.
    private static final Pattern DOUBLE_QUANTIFIER =
        Pattern.compile("[^\\\\][+*]\\s*[+*?](?![+])");

    private ReDoSGuard() {
    }

    /**
     * Validates a regex source, rejecting catastrophic-backtracking shapes.
     *
     * @param regex the regular expression source (must already be syntactically valid)
     * @param ruleName a human-friendly identifier used in the error message
     * @throws PrivacyConfigException if the pattern is considered ReDoS-prone
     */
    public static void validate(final String regex, final String ruleName) {
        if (regex == null || regex.isEmpty()) {
            return;
        }
        if (NESTED_QUANTIFIER.matcher(regex).find()) {
            throw new PrivacyConfigException(
                "Rule '" + ruleName + "' uses a regex with nested quantifiers prone to catastrophic "
                    + "backtracking (ReDoS): " + regex + ". Use possessive quantifiers (*+, ++) or anchors.");
        }
        if (DOUBLE_QUANTIFIER.matcher(regex).find()) {
            throw new PrivacyConfigException(
                "Rule '" + ruleName + "' uses a regex with stacked quantifiers prone to catastrophic "
                    + "backtracking (ReDoS): " + regex + ". Use possessive quantifiers (*+, ++) or anchors.");
        }
    }
}
