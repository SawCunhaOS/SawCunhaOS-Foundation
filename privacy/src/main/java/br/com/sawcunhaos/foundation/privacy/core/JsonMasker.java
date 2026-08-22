
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

import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

import java.util.Map;

/**
 * Masks a JSON document by key, walking the tree <strong>once, in place</strong>.
 *
 * <p>The previous implementation re-serialized and re-parsed the body at every nesting level, which is
 * roughly quadratic. This walker parses once, mutates matching primitives directly and serializes once,
 * giving O(n) over the payload. It uses its own Jackson mapper so the module has no dependency on utils.</p>
 */
public final class JsonMasker {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final MaskingEngine engine;

    public JsonMasker(final MaskingEngine engine) {
        this.engine = engine;
    }

    /**
     * Masks a JSON string by field name. Non-JSON input is returned unchanged.
     *
     * @param json the JSON document
     * @return the masked JSON, or the original string when it is not valid JSON
     */
    public String mask(final String json) {
        if (json == null || json.isEmpty()) {
            return json;
        }
        final JsonNode root;
        try {
            root = MAPPER.readTree(json);
        } catch (JacksonException ex) {
            return json; // not JSON: leave untouched
        }
        walk(root);
        return MAPPER.writeValueAsString(root);
    }

    private void walk(final JsonNode element) {
        if (element.isObject()) {
            walkObject((ObjectNode) element);
        } else if (element.isArray()) {
            walkArray((ArrayNode) element);
        }
    }

    private void walkObject(final ObjectNode obj) {
        for (final Map.Entry<String, JsonNode> entry : obj.properties()) {
            final String key = entry.getKey();
            final JsonNode value = entry.getValue();
            if (value.isObject() || value.isArray()) {
                walk(value);
            } else if (value.isTextual() || value.isNumber() || value.isBoolean()) {
                final String original = value.asString();
                final String masked = engine.maskStructured(key, original);
                if (masked == null) {
                    // redact: blank the value in place (keys are not removed to keep structure stable)
                    obj.putNull(key);
                } else if (!masked.equals(original)) {
                    obj.put(key, masked);
                }
            }
        }
    }

    private void walkArray(final ArrayNode array) {
        for (final JsonNode item : array) {
            walk(item);
        }
    }
}
