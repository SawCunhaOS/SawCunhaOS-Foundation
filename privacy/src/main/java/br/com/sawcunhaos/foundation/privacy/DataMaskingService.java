
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

package br.com.sawcunhaos.foundation.privacy;

import br.com.sawcunhaos.foundation.privacy.core.JsonMasker;
import br.com.sawcunhaos.foundation.privacy.core.MaskingEngine;

/**
 * Thin facade over {@link MaskingEngine} exposing the key-based and text masking operations used by the
 * HTTP logging filters and other consumers. New code may also use the engine directly.
 *
 * <p>The facade is stateless and thread-safe; it simply delegates to the immutable engine.</p>
 */
public class DataMaskingService {

    private final MaskingEngine engine;
    private final JsonMasker jsonMasker;

    public DataMaskingService(final MaskingEngine engine) {
        this.engine = engine;
        this.jsonMasker = new JsonMasker(engine);
    }

    /**
     * Masks an HTTP header value by header name.
     *
     * @param key the header name
     * @param value the header value
     * @return the masked value, or the original when no rule matches
     */
    public String applyDataMaskValueHeader(final String key, final String value) {
        return engine.maskHeader(key, value);
    }

    /**
     * Masks a single body field value by field name.
     *
     * @param key the field name
     * @param value the field value
     * @return the masked value, or the original when no rule matches
     */
    public String applyDataMaskValueBody(final String key, final String value) {
        return engine.maskStructured(key, value);
    }

    /**
     * Masks a whole JSON body by field name, walking the tree once.
     *
     * @param json the JSON document
     * @return the masked JSON
     */
    public String applyDataMaskBody(final String json) {
        return jsonMasker.mask(json);
    }

    /**
     * Masks a free-text message (used by the Logback converter).
     *
     * @param message the rendered log message
     * @return the masked message, or the same reference when nothing matches
     */
    public String applyDataMaskText(final String message) {
        return engine.maskText(message);
    }

    /** @return the underlying immutable engine */
    public MaskingEngine engine() {
        return engine;
    }
}
