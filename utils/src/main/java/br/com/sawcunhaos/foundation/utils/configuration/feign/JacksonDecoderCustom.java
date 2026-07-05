
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

package br.com.sawcunhaos.foundation.utils.configuration.feign;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import feign.Response;
import feign.Util;
import feign.codec.Decoder;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.lang.reflect.Type;

public class JacksonDecoderCustom implements Decoder {
    private final ObjectMapper mapper;

    @SuppressFBWarnings(value = "EI_EXPOSE_REP2",
            justification = "tools.jackson ObjectMapper (Jackson 3) is immutable and thread-safe; the injected instance is shared by design")
    public JacksonDecoderCustom(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    public Object decode(Response response, Type type) throws IOException {
        if (response.status() != 404 && response.status() != 204) {
            if (response.body() == null) {
                return null;
            } else {
                Reader reader = response.body().asReader(response.charset());
                if (!reader.markSupported()) {
                    reader = new BufferedReader(reader, 1);
                }

                try {
                    reader.mark(1);
                    if (reader.read() == -1) {
                        return null;
                    } else {
                        reader.reset();
                        return this.mapper.readValue(reader, this.mapper.constructType(type));
                    }
                } catch (JacksonException var5) {
                    if (var5.getCause() != null && var5.getCause() instanceof IOException) {
                        throw (IOException) var5.getCause();
                    } else {
                        throw var5;
                    }
                }
            }
        } else {
            return Util.emptyValueOf(type);
        }
    }
}
