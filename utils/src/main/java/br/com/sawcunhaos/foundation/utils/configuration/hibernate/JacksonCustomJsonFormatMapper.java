
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

package br.com.sawcunhaos.foundation.utils.configuration.hibernate;


import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.format.AbstractJsonFormatMapper;
import tools.jackson.core.JsonGenerator;
import tools.jackson.core.JsonParser;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.lang.reflect.Type;

public final class JacksonCustomJsonFormatMapper extends AbstractJsonFormatMapper {

    public static final String SHORT_NAME = "jackson";

    private final ObjectMapper objectMapper;

    public JacksonCustomJsonFormatMapper() {
        this( new ObjectMapper() );
    }

    public JacksonCustomJsonFormatMapper(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public <T> void writeToTarget(T value, JavaType<T> javaType, Object target, WrapperOptions options)
            throws IOException {
        objectMapper.writerFor( objectMapper.constructType( javaType.getJavaType() ) )
                .writeValue( (JsonGenerator) target, value );
    }

    @Override
    public <T> T readFromSource(JavaType<T> javaType, Object source, WrapperOptions options) throws IOException {
        return objectMapper.readValue( (JsonParser) source, objectMapper.constructType( javaType.getJavaType() ) );
    }

    @Override
    public boolean supportsSourceType(Class<?> sourceType) {
        return JsonParser.class.isAssignableFrom( sourceType );
    }

    @Override
    public boolean supportsTargetType(Class<?> targetType) {
        return JsonGenerator.class.isAssignableFrom( targetType );
    }

    @Override
    public <T> T fromString(CharSequence charSequence, Type type) {
        return objectMapper.readValue( charSequence.toString(), objectMapper.constructType( type ) );
    }

    @Override
    public <T> String toString(T value, Type type) {
        return objectMapper.writerFor( objectMapper.constructType( type ) ).writeValueAsString( value );
    }
}
