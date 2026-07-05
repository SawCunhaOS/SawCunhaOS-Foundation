
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

package br.com.sawcunhaos.foundation.privacy.spring;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Typed configuration for the privacy module ({@code scos.privacy.*}).
 *
 * <p>These properties control the module <em>behaviour</em> (enablement, strict mode, payload cap and the
 * location of the masking rules file). The masking <em>rules</em> themselves live in the dedicated YAML
 * file referenced by {@link Masking#getConfigPath()}, not here, so they stay editable as data and the core
 * remains usable outside Spring.</p>
 */
@ConfigurationProperties(prefix = "scos.privacy")
public class ScosPrivacyProperties {

    /** Whether the module is active. Defaults to {@code true} (loads on import). */
    private boolean enabled = true;

    /** When {@code true}, missing crypto for hash/encrypt is a startup error instead of a degraded mask. */
    private boolean strict = false;

    /** Maximum payload size (KB) before masking truncates the input, bounding worst-case latency. */
    private int maxPayloadKb = 64;

    /** Masking rules source and builtins selection. */
    private final Masking masking = new Masking();

    /** Application-log integration settings (Logback {@code %mask} converter). */
    private final Log log = new Log();

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(final boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isStrict() {
        return strict;
    }

    public void setStrict(final boolean strict) {
        this.strict = strict;
    }

    public int getMaxPayloadKb() {
        return maxPayloadKb;
    }

    public void setMaxPayloadKb(final int maxPayloadKb) {
        this.maxPayloadKb = maxPayloadKb;
    }

    @SuppressFBWarnings(value = "EI_EXPOSE_REP",
        justification = "Spring binds nested @ConfigurationProperties into the returned instance; it must be the live object, not a copy")
    public Masking getMasking() {
        return masking;
    }

    @SuppressFBWarnings(value = "EI_EXPOSE_REP",
        justification = "Spring binds nested @ConfigurationProperties into the returned instance; it must be the live object, not a copy")
    public Log getLog() {
        return log;
    }

    /**
     * @return whether the Logback {@code %mask} converter should be registered programmatically at startup
     */
    public boolean isLogConverterEnabled() {
        return log.isRegisterConverter();
    }

    /** Masking source resolution and default builtins toggle. */
    public static class Masking {

        /** External path to the masking YAML. Empty falls back to the classpath {@code privacy-masking.yml}. */
        private String configPath;

        /** Convenience toggle to enable the default builtin packs when the YAML does not list any. */
        private boolean defaultPatterns = true;

        public String getConfigPath() {
            return configPath;
        }

        public void setConfigPath(final String configPath) {
            this.configPath = configPath;
        }

        public boolean isDefaultPatterns() {
            return defaultPatterns;
        }

        public void setDefaultPatterns(final boolean defaultPatterns) {
            this.defaultPatterns = defaultPatterns;
        }
    }

    /** Application-log integration. */
    public static class Log {

        /**
         * Whether to register the {@code %mask} Logback conversion rule programmatically at startup
         * (so masking "loads on import" without editing {@code logback.xml}). Set to {@code false} to opt out
         * and register it manually via {@code <include>} of {@code logback-privacy.xml}.
         */
        private boolean registerConverter = true;

        public boolean isRegisterConverter() {
            return registerConverter;
        }

        public void setRegisterConverter(final boolean registerConverter) {
            this.registerConverter = registerConverter;
        }
    }
}
