
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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Loads {@link PrivacyConfig} from a dedicated YAML file using SnakeYAML only (no Spring, no database).
 *
 * <p><strong>Source resolution precedence:</strong></p>
 * <ol>
 *     <li>external path — {@code config-path} argument, or env {@code SCOS_PRIVACY_MASKING_CONFIG};</li>
 *     <li>classpath fallback — {@code privacy-masking.yml};</li>
 *     <li>neither present — an empty config with a WARN, so the engine still boots on builtins.</li>
 * </ol>
 *
 * <p><strong>Validation</strong> happens here, at load time: unknown keys, invalid {@code strategy},
 * negative {@code keep-*}, multi-char {@code mask-char} and ReDoS-prone regexes all raise
 * {@link PrivacyConfigException} so a misconfiguration fails fast instead of leaking data in production.</p>
 */
public final class PrivacyConfigLoader {

    private static final Logger LOG = LoggerFactory.getLogger(PrivacyConfigLoader.class);

    /** Environment variable that points to an external masking YAML file. */
    public static final String ENV_CONFIG = "SCOS_PRIVACY_MASKING_CONFIG";

    /** Classpath resource used as fallback when no external file is provided. */
    public static final String CLASSPATH_RESOURCE = "privacy-masking.yml";

    private static final Set<String> ALLOWED_RULE_KEYS = Set.of(
        "key", "literal", "regex", "value", "strategy",
        "keep-first", "keep-last", "mask-char", "preserve-length", "mask-local", "mask-domain",
        // builtin-only flags tolerated on pattern entries
        "name", "luhn", "cpf", "cnpj");

    private PrivacyConfigLoader() {
    }

    /**
     * Resolves the source per the documented precedence and parses it.
     *
     * @param configPath optional external path (may be {@code null}/blank to skip to env/classpath)
     * @return a validated {@link PrivacyConfig}; empty (with WARN) when no source is found
     */
    public static PrivacyConfig load(final String configPath) {
        final String external = (configPath != null && !configPath.isBlank())
            ? configPath
            : System.getenv(ENV_CONFIG);

        if (external != null && !external.isBlank()) {
            final Path path = Path.of(external.trim());
            if (Files.isReadable(path)) {
                LOG.info("Loading privacy masking rules from external file: {}", path);
                try (InputStream in = Files.newInputStream(path)) {
                    return parseAndValidate(in);
                } catch (IOException ex) {
                    throw new PrivacyConfigException("Failed to read privacy masking file: " + path, ex);
                }
            }
            LOG.warn("Configured privacy masking file '{}' is not readable; falling back to classpath", external);
        }

        try (InputStream in = currentThreadResource(CLASSPATH_RESOURCE)) {
            if (in != null) {
                LOG.info("Loading privacy masking rules from classpath resource: {}", CLASSPATH_RESOURCE);
                return parseAndValidate(in);
            }
        } catch (IOException ex) {
            throw new PrivacyConfigException("Failed to read classpath privacy masking resource", ex);
        }

        LOG.warn("No privacy masking YAML found (external nor classpath '{}'). "
            + "Masking will rely on builtins only.", CLASSPATH_RESOURCE);
        return PrivacyConfig.empty();
    }

    /**
     * Parses a YAML stream into a validated {@link PrivacyConfig}. Public so callers (and the
     * {@code fromYaml} factory) can feed an arbitrary stream.
     *
     * @param yaml the YAML input stream (closed by the caller)
     * @return a validated configuration
     */
    @SuppressWarnings("unchecked")
    public static PrivacyConfig parseAndValidate(final InputStream yaml) {
        final Object root = new Yaml().load(yaml);
        final PrivacyConfig config = new PrivacyConfig();
        if (!(root instanceof Map<?, ?> rootMap)) {
            return config;
        }

        final Map<String, Object> masking = navigate((Map<String, Object>) rootMap, "scos", "privacy", "masking");
        if (masking == null) {
            return config;
        }

        readBuiltins(masking, config);
        readRules((List<Object>) masking.get("headers"), config.getHeaders(), false, "headers");
        readRules((List<Object>) masking.get("body"), config.getBody(), false, "body");
        readRules((List<Object>) masking.get("log-patterns"), config.getLogPatterns(), true, "log-patterns");
        readAuditFields(masking.get("audit-encrypt-fields"), config);
        return config;
    }

    @SuppressWarnings("unchecked")
    private static void readBuiltins(final Map<String, Object> masking, final PrivacyConfig config) {
        final Object builtins = masking.get("builtins");
        if (!(builtins instanceof Map<?, ?> b)) {
            return;
        }
        final Object enabled = ((Map<String, Object>) b).get("enabled");
        if (enabled instanceof List<?> list) {
            list.forEach(item -> config.getBuiltins().getEnabled().add(String.valueOf(item).toLowerCase(Locale.ROOT)));
        }
        final Object disabled = ((Map<String, Object>) b).get("disabled");
        if (disabled instanceof List<?> list) {
            list.forEach(item -> config.getBuiltins().getDisabled().add(String.valueOf(item).toLowerCase(Locale.ROOT)));
        }
    }

    @SuppressWarnings("unchecked")
    private static void readRules(final List<Object> raw, final List<PrivacyConfig.RuleEntry> target,
                                  final boolean logPattern, final String section) {
        if (raw == null) {
            return;
        }
        for (final Object item : raw) {
            if (!(item instanceof Map<?, ?> map)) {
                throw new PrivacyConfigException("Section '" + section + "' must contain a list of maps");
            }
            final PrivacyConfig.RuleEntry entry = toEntry((Map<String, Object>) map, section);
            validateEntry(entry, logPattern, section);
            target.add(entry);
        }
    }

    private static PrivacyConfig.RuleEntry toEntry(final Map<String, Object> map, final String section) {
        final PrivacyConfig.RuleEntry entry = new PrivacyConfig.RuleEntry();
        for (final Map.Entry<String, Object> e : map.entrySet()) {
            final String k = e.getKey();
            if (!ALLOWED_RULE_KEYS.contains(k)) {
                throw new PrivacyConfigException("Unknown key '" + k + "' in section '" + section + "'");
            }
            final Object v = e.getValue();
            switch (k) {
                case "key" -> entry.setKey(asString(v));
                case "literal" -> entry.setLiteral(asString(v));
                case "regex" -> entry.setRegex(asString(v));
                case "value" -> entry.setValue(asString(v));
                case "strategy" -> entry.setStrategy(asString(v));
                case "keep-first" -> entry.setKeepFirst(asInt(v, "keep-first"));
                case "keep-last" -> entry.setKeepLast(asInt(v, "keep-last"));
                case "mask-char" -> entry.setMaskChar(asString(v));
                case "preserve-length" -> entry.setPreserveLength(asBool(v));
                case "mask-local" -> entry.setMaskLocal(asBool(v));
                case "mask-domain" -> entry.setMaskDomain(asBool(v));
                default -> { /* name/luhn/cpf/cnpj: builtin flags handled by BuiltinPackLoader */ }
            }
        }
        return entry;
    }

    private static void validateEntry(final PrivacyConfig.RuleEntry entry, final boolean logPattern,
                                      final String section) {
        final String id = section + "[" + (entry.getKey() != null ? entry.getKey()
            : entry.getLiteral() != null ? entry.getLiteral() : entry.getRegex()) + "]";

        // strategy: closed enum, fails fast on unknown token.
        final var strategy = br.com.sawcunhaos.foundation.privacy.model.MaskStrategy.from(entry.getStrategy());
        if (logPattern && !strategy.isAllowedForLogPatterns()) {
            throw new PrivacyConfigException("Rule " + id + " uses strategy '" + strategy
                + "' which is not allowed for log-patterns (only fixed, partial, hash)");
        }

        if (entry.getKeepFirst() != null && entry.getKeepFirst() < 0) {
            throw new PrivacyConfigException("Rule " + id + " has negative keep-first");
        }
        if (entry.getKeepLast() != null && entry.getKeepLast() < 0) {
            throw new PrivacyConfigException("Rule " + id + " has negative keep-last");
        }
        if (entry.getMaskChar() != null && entry.getMaskChar().length() != 1) {
            throw new PrivacyConfigException("Rule " + id + " has mask-char that is not a single character");
        }
        if (entry.getRegex() != null) {
            try {
                java.util.regex.Pattern.compile(entry.getRegex());
            } catch (RuntimeException ex) {
                throw new PrivacyConfigException("Rule " + id + " has an invalid regex: " + entry.getRegex(), ex);
            }
            ReDoSGuard.validate(entry.getRegex(), id);
        }
        final boolean hasKey = entry.getKey() != null;
        final boolean hasText = entry.getLiteral() != null || entry.getRegex() != null;
        if (!hasKey && !hasText) {
            throw new PrivacyConfigException("Rule " + id + " must define one of: key, literal, regex");
        }
    }

    @SuppressWarnings("unchecked")
    private static void readAuditFields(final Object raw, final PrivacyConfig config) {
        if (raw instanceof List<?> list) {
            list.forEach(item -> config.getAuditEncryptFields().add(String.valueOf(item)));
        }
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> navigate(final Map<String, Object> root, final String... path) {
        Map<String, Object> current = root;
        for (final String key : path) {
            final Object next = current.get(key);
            if (!(next instanceof Map<?, ?> map)) {
                return null;
            }
            current = (Map<String, Object>) map;
        }
        return current;
    }

    private static InputStream currentThreadResource(final String name) {
        final ClassLoader cl = Thread.currentThread().getContextClassLoader();
        final ClassLoader loader = cl != null ? cl : PrivacyConfigLoader.class.getClassLoader();
        return loader.getResourceAsStream(name);
    }

    private static String asString(final Object v) {
        return v == null ? null : String.valueOf(v);
    }

    private static Integer asInt(final Object v, final String field) {
        if (v == null) {
            return null;
        }
        if (v instanceof Number n) {
            return n.intValue();
        }
        try {
            return Integer.parseInt(String.valueOf(v).trim());
        } catch (NumberFormatException ex) {
            throw new PrivacyConfigException("Field '" + field + "' must be an integer, got: " + v, ex);
        }
    }

    private static Boolean asBool(final Object v) {
        if (v == null) {
            return null;
        }
        if (v instanceof Boolean b) {
            return b;
        }
        return Boolean.parseBoolean(String.valueOf(v).trim());
    }

    static List<String> orderedRuleKeys() {
        return new ArrayList<>(ALLOWED_RULE_KEYS);
    }
}
