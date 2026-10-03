
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

package br.com.sawcunhaos.foundation.jpa.hibernate;


import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.format.AbstractJsonFormatMapper;
import tools.jackson.core.JsonGenerator;
import tools.jackson.core.JsonParser;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.lang.reflect.Type;

/**
 * {@code FormatMapper} do Hibernate que serializa colunas JSON ({@code @JdbcTypeCode(SqlTypes.JSON)})
 * com Jackson 3 ({@code tools.jackson}). Existe porque a Foundation migrou de Gson para Jackson
 * (Story 1.3): o nome da classe só faz sentido após essa migração.
 *
 * <p>Ativação (não é automática): configure
 * {@code spring.jpa.properties.hibernate.type.json_format_mapper} com o nome completo desta classe.
 * O Hibernate a instancia pelo construtor sem argumentos, que usa um {@code ObjectMapper} padrão
 * ({@code new ObjectMapper()}), <b>sem</b> os módulos/configuração do {@code ObjectMapper} do
 * Spring; para usar o seu, instancie com {@link #JacksonCustomJsonFormatMapper(ObjectMapper)}.
 *
 * <p>Os métodos herdados de {@code AbstractJsonFormatMapper} fazem a ponte entre o tipo Java da
 * propriedade e o {@code JsonParser}/{@code JsonGenerator} que o Hibernate fornece.
 *
 * @since 1.2.0
 */
public final class JacksonCustomJsonFormatMapper extends AbstractJsonFormatMapper {

    /** Nome curto do mapper, no padrão dos {@code FormatMapper} embutidos do Hibernate. */
    public static final String SHORT_NAME = "jackson";

    private final ObjectMapper objectMapper;

    /** Usa um {@code ObjectMapper} padrão do Jackson 3 (sem módulos extras). */
    public JacksonCustomJsonFormatMapper() {
        this( new ObjectMapper() );
    }

    /**
     * Usa o {@code ObjectMapper} informado (compartilhado, não copiado).
     *
     * @param objectMapper mapper Jackson 3; não deve ser {@code null}
     */
    @SuppressFBWarnings(value = "EI_EXPOSE_REP2",
            justification = "tools.jackson ObjectMapper (Jackson 3) is immutable and thread-safe; the injected instance is shared by design")
    public JacksonCustomJsonFormatMapper(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /** {@inheritDoc} Escreve {@code value} no {@code JsonGenerator} {@code target}. */
    @Override
    public <T> void writeToTarget(T value, JavaType<T> javaType, Object target, WrapperOptions options)
            throws IOException {
        objectMapper.writerFor( objectMapper.constructType( javaType.getJavaType() ) )
                .writeValue( (JsonGenerator) target, value );
    }

    /** {@inheritDoc} Lê o valor do {@code JsonParser} {@code source} como o tipo Java da propriedade. */
    @Override
    public <T> T readFromSource(JavaType<T> javaType, Object source, WrapperOptions options) throws IOException {
        return objectMapper.readValue( (JsonParser) source, objectMapper.constructType( javaType.getJavaType() ) );
    }

    /** {@inheritDoc} Só aceita fontes {@code JsonParser} (Jackson 3). */
    @Override
    public boolean supportsSourceType(Class<?> sourceType) {
        return JsonParser.class.isAssignableFrom( sourceType );
    }

    /** {@inheritDoc} Só aceita destinos {@code JsonGenerator} (Jackson 3). */
    @Override
    public boolean supportsTargetType(Class<?> targetType) {
        return JsonGenerator.class.isAssignableFrom( targetType );
    }

    /** {@inheritDoc} Desserializa o texto JSON para o {@code type} informado. */
    @Override
    public <T> T fromString(CharSequence charSequence, Type type) {
        return objectMapper.readValue( charSequence.toString(), objectMapper.constructType( type ) );
    }

    /** {@inheritDoc} Serializa {@code value} como texto JSON usando {@code type} como tipo declarado. */
    @Override
    public <T> String toString(T value, Type type) {
        return objectMapper.writerFor( objectMapper.constructType( type ) ).writeValueAsString( value );
    }
}
