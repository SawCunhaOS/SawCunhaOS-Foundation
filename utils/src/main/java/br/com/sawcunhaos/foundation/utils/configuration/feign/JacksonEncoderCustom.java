
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

import feign.RequestTemplate;
import feign.Util;
import feign.codec.EncodeException;
import feign.codec.Encoder;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JavaType;
import tools.jackson.databind.ObjectMapper;

import java.lang.reflect.Type;


public class JacksonEncoderCustom implements Encoder {
    private final ObjectMapper mapper;

    public JacksonEncoderCustom(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    public void encode(Object object, Type bodyType, RequestTemplate template) {
        try {
            JavaType javaType = this.mapper.getTypeFactory().constructType(bodyType);
            template.body(this.mapper.writerFor(javaType).writeValueAsBytes(object), Util.UTF_8);
        } catch (JacksonException e) {
            throw new EncodeException(e.getMessage(), e);
        }
    }
}
