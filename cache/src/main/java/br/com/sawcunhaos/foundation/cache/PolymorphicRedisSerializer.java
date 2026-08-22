
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

package br.com.sawcunhaos.foundation.cache;

import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.SerializationException;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.SerializationFeature;
import tools.jackson.dataformat.smile.SmileMapper;

public class PolymorphicRedisSerializer implements RedisSerializer<Object> {

    private final ObjectMapper mapper;

    public PolymorphicRedisSerializer() {
        this.mapper = SmileMapper.builder()
                // NÃO pretty print
                .disable(SerializationFeature.INDENT_OUTPUT)
                .disable(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES)
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)

                // Suporte Java Time
                .findAndAddModules()
                .build();
    }

    record Payload(String type, JsonNode value) {}

    @Override
    public byte[] serialize(Object value) {
        try {
            if (value == null) return new byte[0];

            Payload payload = new Payload(
                    value.getClass().getName(),
                    mapper.valueToTree(value)
            );

            return mapper.writeValueAsBytes(payload);
        } catch (Exception e) {
            throw new SerializationException("Erro ao serializar", e);
        }
    }

    @Override
    public Object deserialize(byte[] bytes) {
        try {
            if (bytes == null || bytes.length == 0) return null;

            Payload payload = mapper.readValue(bytes, Payload.class);
            Class<?> clazz = Class.forName(payload.type());

            return mapper.treeToValue(payload.value(), clazz);
        } catch (Exception e) {
            throw new SerializationException("Erro ao desserializar", e);
        }
    }
}
