
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
import feign.Response;
import feign.Util;
import feign.codec.Decoder;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.lang.reflect.Type;

/**
 * {@link Decoder} do Feign que desserializa o corpo da resposta com o {@link ObjectMapper} do
 * Jackson 3 ({@code tools.jackson}) injetado, no lugar do decoder baseado em Gson usado antes da
 * migração Gson para Jackson (Story 1.3).
 *
 * <p>Regras de decodificação, na ordem em que são avaliadas:
 * <ol>
 *   <li>status {@code 404} ou {@code 204}: devolve {@link Util#emptyValueOf(Type)} (lista vazia,
 *       {@code Optional.empty()} etc.), sem ler o corpo;</li>
 *   <li>corpo ausente ({@code response.body() == null}): devolve {@code null};</li>
 *   <li>corpo vazio (primeiro {@code read()} devolve {@code -1}): devolve {@code null}, em vez de
 *       deixar o Jackson falhar com "No content to map";</li>
 *   <li>caso contrário, {@code mapper.readValue(reader, constructType(type))}.</li>
 * </ol>
 *
 * <p>Thread-safe: não guarda estado além do {@code ObjectMapper}, que é imutável e compartilhado.
 *
 * @see JacksonEncoderCustom
 */
public class JacksonDecoderCustom implements Decoder {
    private final ObjectMapper mapper;

    /**
     * Cria o decoder.
     *
     * @param mapper mapper Jackson 3 usado para ler o corpo; compartilhado, não é copiado
     */
    @SuppressFBWarnings(value = "EI_EXPOSE_REP2",
            justification = "tools.jackson ObjectMapper (Jackson 3) is immutable and thread-safe; the injected instance is shared by design")
    public JacksonDecoderCustom(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    /**
     * Converte o corpo da resposta no tipo pedido.
     *
     * @param response resposta HTTP do Feign
     * @param type     tipo genérico de retorno do método do cliente (preserva parâmetros como
     *                 {@code List<Foo>})
     * @return o objeto desserializado; {@code null} se não há corpo ou ele é vazio; o valor vazio
     *     do tipo se o status é 404 ou 204
     * @throws IOException se a leitura do corpo falha (inclusive quando o Jackson embrulha uma
     *                     {@code IOException} em {@link JacksonException})
     * @throws JacksonException se o conteúdo não é JSON válido ou não casa com o tipo
     */
    public Object decode(Response response, Type type) throws IOException {
        if (response.status() != 404 && response.status() != 204) {
            if (response.body() == null) {
                return null;
            } else {
                Reader reader = response.body().asReader(response.charset());
                // mark/reset de 1 char permite espiar o primeiro caractere para detectar corpo vazio
                // sem consumi-lo; BufferedReader(…, 1) é o mínimo para readers sem suporte a mark.
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
                    // O Jackson 3 embrulha falhas de I/O do reader em JacksonException (não checada).
                    // Desembrulha a IOException original para o Feign tratá-la como erro de I/O/rede
                    // (candidata a retry), e não como erro de conteúdo; as demais (JSON inválido,
                    // tipo incompatível) seguem como JacksonException.
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
