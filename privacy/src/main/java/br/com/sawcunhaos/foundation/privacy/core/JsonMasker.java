
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

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;

import java.util.Map;

/**
 * Masks a JSON document by key, walking the tree <strong>once, in place</strong>.
 *
 * <p>The previous implementation re-serialized and re-parsed the body at every nesting level, which is
 * roughly quadratic. This walker parses once, mutates matching primitives directly and serializes once,
 * giving O(n) over the payload. It uses the engine's own Gson so the module has no dependency on utils.</p>
 */
public final class JsonMasker {

    private static final Gson GSON = new Gson();

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
        final JsonElement root;
        try {
            root = JsonParser.parseString(json);
        } catch (RuntimeException ex) {
            return json; // not JSON: leave untouched
        }
        walk(root);
        return GSON.toJson(root);
    }

    private void walk(final JsonElement element) {
        if (element.isJsonObject()) {
            walkObject(element.getAsJsonObject());
        } else if (element.isJsonArray()) {
            walkArray(element.getAsJsonArray());
        }
    }

    private void walkObject(final JsonObject obj) {
        for (final Map.Entry<String, JsonElement> entry : obj.entrySet()) {
            final String key = entry.getKey();
            final JsonElement value = entry.getValue();
            if (value.isJsonObject() || value.isJsonArray()) {
                walk(value);
            } else if (value.isJsonPrimitive()) {
                final String original = value.getAsString();
                final String masked = engine.maskStructured(key, original);
                if (masked == null) {
                    // redact: blank the value in place (keys are not removed to keep structure stable)
                    obj.add(key, JsonParser.parseString("null"));
                } else if (!masked.equals(original)) {
                    obj.add(key, new JsonPrimitive(masked));
                }
            }
        }
    }

    private void walkArray(final JsonArray array) {
        for (final JsonElement item : array) {
            walk(item);
        }
    }
}
