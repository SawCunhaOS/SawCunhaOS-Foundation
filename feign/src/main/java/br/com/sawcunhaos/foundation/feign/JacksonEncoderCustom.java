
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

package br.com.sawcunhaos.foundation.feign;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import feign.RequestTemplate;
import feign.Util;
import feign.codec.EncodeException;
import feign.codec.Encoder;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JavaType;
import tools.jackson.databind.ObjectMapper;

import java.lang.reflect.Type;


/**
 * {@link Encoder} do Feign que serializa o corpo da requisição com o {@link ObjectMapper} do
 * Jackson 3 ({@code tools.jackson}) injetado, no lugar do encoder baseado em Gson usado antes da
 * migração Gson para Jackson (Story 1.3). Assim a requisição usa as mesmas configurações do mapper
 * da aplicação (módulos, formato de datas, inclusão de nulos).
 *
 * <p>O corpo é gravado como bytes JSON em UTF-8 via {@link RequestTemplate#body(byte[],
 * java.nio.charset.Charset)}. O tipo usado é o {@code bodyType} genérico declarado no método do
 * cliente, e não o {@code getClass()} do objeto, para preservar parâmetros de tipo.
 *
 * <p>Thread-safe: não guarda estado além do {@code ObjectMapper}, que é imutável e compartilhado.
 *
 * @see JacksonDecoderCustom
 */
public class JacksonEncoderCustom implements Encoder {
    private final ObjectMapper mapper;

    /**
     * Cria o encoder.
     *
     * @param mapper mapper Jackson 3 usado para serializar; compartilhado, não é copiado
     */
    @SuppressFBWarnings(value = "EI_EXPOSE_REP2",
            justification = "tools.jackson ObjectMapper (Jackson 3) is immutable and thread-safe; the injected instance is shared by design")
    public JacksonEncoderCustom(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    /**
     * Serializa {@code object} como JSON UTF-8 e define o resultado como corpo da requisição.
     *
     * @param object   objeto a enviar
     * @param bodyType tipo genérico declarado do parâmetro de corpo
     * @param template requisição Feign que recebe o corpo
     * @throws EncodeException se o Jackson falhar ao serializar (causa preservada)
     */
    public void encode(Object object, Type bodyType, RequestTemplate template) {
        try {
            JavaType javaType = this.mapper.getTypeFactory().constructType(bodyType);
            template.body(this.mapper.writerFor(javaType).writeValueAsBytes(object), Util.UTF_8);
        } catch (JacksonException e) {
            // JacksonException é não checada; o contrato do Encoder do Feign exige EncodeException.
            throw new EncodeException(e.getMessage(), e);
        }
    }
}
