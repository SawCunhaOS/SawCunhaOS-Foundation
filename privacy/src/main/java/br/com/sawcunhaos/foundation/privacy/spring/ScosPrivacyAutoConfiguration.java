
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

import br.com.sawcunhaos.foundation.privacy.DataMaskingService;
import br.com.sawcunhaos.foundation.privacy.SanitizationBodyComponent;
import br.com.sawcunhaos.foundation.privacy.SanitizationHeadersComponent;
import br.com.sawcunhaos.foundation.privacy.config.PrivacyConfig;
import br.com.sawcunhaos.foundation.privacy.config.PrivacyConfigLoader;
import br.com.sawcunhaos.foundation.privacy.core.MaskingEngine;
import br.com.sawcunhaos.foundation.privacy.crypto.JasyptCryptoKeyProvider;
import br.com.sawcunhaos.foundation.privacy.crypto.ScosCryptoKeyProvider;
import br.com.sawcunhaos.foundation.privacy.crypto.ScosFieldCipher;
import br.com.sawcunhaos.foundation.privacy.logback.ScosMaskingConverterSupport;
import br.com.sawcunhaos.foundation.privacy.specification.DataMaskingValues;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;

/**
 * Auto-configuration that wires the privacy module so it loads simply by being on the classpath — the same
 * pattern used by {@code audit} and {@code jdempotent} — without requiring {@code @ComponentScan} in the app.
 *
 * <p>Every public bean is {@link ConditionalOnMissingBean}, so an application can override any piece (its own
 * {@link DataMaskingValues}, a Vault/KMS-backed {@link ScosCryptoKeyProvider}, or the whole engine). The
 * masking rules come from the YAML resolved by {@link PrivacyConfigLoader}; an optional {@link DataMaskingValues}
 * SPI is injected via {@link ObjectProvider} and added on top (precedence builtins &rarr; YAML &rarr; SPI).</p>
 */
@AutoConfiguration
@EnableConfigurationProperties(ScosPrivacyProperties.class)
@ConditionalOnProperty(prefix = "scos.privacy", name = "enabled", havingValue = "true", matchIfMissing = true)
public class ScosPrivacyAutoConfiguration {

    /**
     * Loads the masking configuration from the resolved YAML source (external &rarr; classpath &rarr; builtins).
     *
     * @param props the module properties carrying the config path
     * @return the parsed configuration
     */
    @Bean
    @ConditionalOnMissingBean
    public PrivacyConfig privacyConfig(final ScosPrivacyProperties props) {
        return PrivacyConfigLoader.load(props.getMasking().getConfigPath());
    }

    /**
     * Default key provider backed by an externalized secret ({@code scos.privacy.crypto.secret} or env
     * {@code SCOS_PRIVACY_CRYPTO_SECRET}). Only created when a secret is present, so the app can plug its own.
     *
     * @param env the Spring environment used to read the secret
     * @return a key provider, or {@code null} when no secret is configured
     */
    @Bean
    @ConditionalOnMissingBean
    public ScosCryptoKeyProvider scosCryptoKeyProvider(final Environment env) {
        final String secret = env.getProperty("scos.privacy.crypto.secret",
            System.getenv("SCOS_PRIVACY_CRYPTO_SECRET"));
        return (secret != null && !secret.isBlank()) ? new JasyptCryptoKeyProvider(secret) : null;
    }

    /**
     * The reversible field cipher used by the audit trail. Created only when a key provider exists.
     *
     * @param keyProvider the key provider (may be absent)
     * @return a field cipher, or {@code null} when no key provider is available
     */
    @Bean
    @ConditionalOnMissingBean
    public ScosFieldCipher scosFieldCipher(final ObjectProvider<ScosCryptoKeyProvider> keyProvider) {
        final ScosCryptoKeyProvider provider = keyProvider.getIfAvailable();
        return provider != null ? new ScosFieldCipher(provider) : null;
    }

    /**
     * Builds the immutable singleton engine, flattening builtins, YAML and the optional SPI overrides once.
     *
     * @param config the parsed YAML configuration
     * @param props the module properties (strict mode, payload cap)
     * @param keyProvider the optional crypto key provider
     * @param spiOverrides the optional programmatic rule sources
     * @return the shared engine
     */
    @Bean
    @ConditionalOnMissingBean
    public MaskingEngine maskingEngine(final PrivacyConfig config,
                                       final ScosPrivacyProperties props,
                                       final ObjectProvider<ScosCryptoKeyProvider> keyProvider,
                                       final ObjectProvider<DataMaskingValues> spiOverrides) {
        final MaskingEngine.Builder builder = MaskingEngine.builder()
            .config(config)
            .strict(props.isStrict())
            .maxPayloadKb(props.getMaxPayloadKb())
            .keyProvider(keyProvider.getIfAvailable());
        spiOverrides.orderedStream().forEach(builder::addSpi);

        final MaskingEngine engine = builder.build();
        // Register %mask programmatically so log masking also "loads on import".
        if (props.isLogConverterEnabled()) {
            ScosMaskingConverterSupport.register(engine);
        }
        return engine;
    }

    /**
     * Facade kept for source compatibility with the previous masking API.
     *
     * @param engine the shared engine
     * @return the facade bean
     */
    @Bean
    @ConditionalOnMissingBean
    public DataMaskingService dataMaskingService(final MaskingEngine engine) {
        return new DataMaskingService(engine);
    }

    /**
     * @param dataMaskingService the facade
     * @return the header sanitizer used by the HTTP logging filters
     */
    @Bean
    @ConditionalOnMissingBean
    public SanitizationHeadersComponent sanitizationHeadersComponent(final DataMaskingService dataMaskingService) {
        return new SanitizationHeadersComponent(dataMaskingService);
    }

    /**
     * @param dataMaskingService the facade
     * @return the body sanitizer used by the HTTP logging filters
     */
    @Bean
    @ConditionalOnMissingBean
    public SanitizationBodyComponent sanitizationBodyComponent(final DataMaskingService dataMaskingService) {
        return new SanitizationBodyComponent(dataMaskingService);
    }
}
