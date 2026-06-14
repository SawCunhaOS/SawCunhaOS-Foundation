
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

import br.com.sawcunhaos.foundation.privacy.model.DataMask;
import br.com.sawcunhaos.foundation.privacy.model.MaskStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.Yaml;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Loads builtin masking patterns shipped as <strong>data</strong> ({@code privacy-builtins/<pack>.yml}).
 *
 * <p>Adding a country is a new YAML file, not new code. Packs are enabled by name and individual items
 * can be disabled via {@code <pack>.<name>} (e.g. {@code br.titulo-eleitor}). Items carrying a check-digit
 * flag ({@code cpf}/{@code cnpj}/{@code luhn}) are wired to the engine's validators so a numeric sequence
 * that matches the regex but fails the digit check is not masked.</p>
 */
public final class BuiltinPackLoader {

    private static final Logger LOG = LoggerFactory.getLogger(BuiltinPackLoader.class);

    /** Known packs; each maps to {@code privacy-builtins/<pack>.yml} on the classpath. */
    public static final Set<String> KNOWN_PACKS = Set.of("generic", "br", "us", "eu", "uk", "in");

    private BuiltinPackLoader() {
    }

    /**
     * A builtin pattern with an optional check-digit validator hint.
     *
     * @param mask the compiled masking rule
     * @param validator one of {@code "luhn"}, {@code "cpf"}, {@code "cnpj"} or {@code null}
     */
    public record BuiltinPattern(DataMask mask, String validator) {
    }

    /**
     * Resolves the effective builtin patterns for the enabled packs minus the disabled items.
     *
     * @param config the parsed configuration carrying the builtin selection
     * @return the ordered list of builtin patterns to apply (before YAML/SPI in precedence)
     */
    @SuppressWarnings("unchecked")
    public static List<BuiltinPattern> resolve(final PrivacyConfig config) {
        final List<BuiltinPattern> result = new ArrayList<>();
        final Set<String> disabled = config.getDisabledBuiltins();

        for (final String packRaw : config.getEnabledBuiltins()) {
            final String pack = packRaw.toLowerCase(Locale.ROOT);
            if (!KNOWN_PACKS.contains(pack)) {
                LOG.warn("Unknown builtin pack '{}' ignored. Known packs: {}", pack, KNOWN_PACKS);
                continue;
            }
            final Map<String, Object> doc = read("privacy-builtins/" + pack + ".yml");
            if (doc == null) {
                continue;
            }
            final Object patterns = doc.get("patterns");
            if (!(patterns instanceof List<?> list)) {
                continue;
            }
            for (final Object p : list) {
                if (!(p instanceof Map<?, ?> map)) {
                    continue;
                }
                final Map<String, Object> entry = (Map<String, Object>) map;
                final String name = String.valueOf(entry.get("name"));
                if (disabled.contains(pack + "." + name)) {
                    continue;
                }
                result.add(toPattern(pack, name, entry));
            }
        }
        return result;
    }

    private static BuiltinPattern toPattern(final String pack, final String name, final Map<String, Object> entry) {
        final String regex = str(entry.get("regex"));
        final String literal = str(entry.get("literal"));
        final MaskStrategy strategy = MaskStrategy.from(str(entry.get("strategy")));
        final String value = entry.get("value") != null ? str(entry.get("value")) : DataMask.DEFAULT_VALUE;
        final int keepFirst = intOr(entry.get("keep-first"), 0);
        final int keepLast = intOr(entry.get("keep-last"), 0);

        final String canonical = pack + "." + name;
        final DataMask.DataMaskBuilder builder = DataMask.builder()
            .key(canonical)
            .strategy(strategy)
            .newValue(value)
            .keepFirst(keepFirst)
            .keepLast(keepLast)
            .maskChar(DataMask.DEFAULT_MASK_CHAR)
            .preserveLength(true);

        if (regex != null) {
            ReDoSGuard.validate(regex, canonical);
            builder.regex(regex).compiled(java.util.regex.Pattern.compile(regex));
        } else if (literal != null) {
            builder.literal(literal);
        }

        final String validator = bool(entry.get("luhn")) ? "luhn"
            : bool(entry.get("cpf")) ? "cpf"
            : bool(entry.get("cnpj")) ? "cnpj"
            : null;
        return new BuiltinPattern(builder.build(), validator);
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> read(final String resource) {
        final ClassLoader cl = Thread.currentThread().getContextClassLoader();
        final ClassLoader loader = cl != null ? cl : BuiltinPackLoader.class.getClassLoader();
        try (InputStream in = loader.getResourceAsStream(resource)) {
            if (in == null) {
                LOG.warn("Builtin pack resource not found: {}", resource);
                return null;
            }
            final Object root = new Yaml().load(in);
            return root instanceof Map<?, ?> map ? (Map<String, Object>) map : null;
        } catch (Exception ex) {
            throw new PrivacyConfigException("Failed to read builtin pack: " + resource, ex);
        }
    }

    private static String str(final Object v) {
        return v == null ? null : String.valueOf(v);
    }

    private static int intOr(final Object v, final int dflt) {
        if (v instanceof Number n) {
            return n.intValue();
        }
        if (v != null) {
            try {
                return Integer.parseInt(String.valueOf(v).trim());
            } catch (NumberFormatException ignored) {
                return dflt;
            }
        }
        return dflt;
    }

    private static boolean bool(final Object v) {
        return v instanceof Boolean b ? b : Boolean.parseBoolean(String.valueOf(v));
    }
}
