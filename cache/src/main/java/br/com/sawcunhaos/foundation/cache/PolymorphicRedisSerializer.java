
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
import tools.jackson.databind.JavaType;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.SerializationFeature;
import tools.jackson.dataformat.smile.SmileMapper;

import java.util.Collection;
import java.util.Set;

/**
 * {@link RedisSerializer} que grava qualquer objeto como um envelope Smile (JSON binário) com o
 * nome da classe concreta e o reconstrói na leitura, para que um mesmo cache guarde tipos
 * diferentes sem configuração por tipo.
 *
 * <p><b>Formato:</b> {@code Payload(type, elementType, value)}. {@code elementType} só existe para
 * coleções cujos elementos não nulos compartilham uma única classe concreta (os genéricos são
 * apagados em tempo de execução, então sem ele {@code List<Foo>} voltaria como lista de mapas).</p>
 *
 * <p><b>Contrato de segurança (allowlist, Story 3.16):</b> o nome da classe vem do valor lido do
 * Redis, ou seja, de dados que um atacante com escrita no Redis controla. Por isso, antes de
 * qualquer {@link Class#forName(String)}, {@code type} e {@code elementType} são checados contra a
 * allowlist; o que não bater é rejeitado com {@link SerializationException} e a classe nunca é
 * carregada. Tipos aceitos: pacote {@code br.com.sawcunhaos.}, {@code java.math.}, {@code java.time.},
 * {@code java.util.}, {@code java.lang.String}, arrays primitivos, arrays de objeto cujo componente
 * seja permitido (uma dimensão) e os nomes completos passados ao construtor
 * {@link #PolymorphicRedisSerializer(Set)}. Classes de terceiros (origem dos gadget chains de
 * Jackson conhecidos) só entram por esse construtor.</p>
 *
 * <p>{@code null} serializa para {@code byte[0]}; {@code null} ou {@code byte[0]} desserializa para
 * {@code null}. Falhas viram {@link SerializationException}. A instância é thread-safe (o
 * {@code ObjectMapper} é imutável após o build).</p>
 */
public class PolymorphicRedisSerializer implements RedisSerializer<Object> {

    /*
     * Story 3.16 (AC #1): allowlist gate applied to {@code payload.type()} — and, for collections,
     * {@code payload.elementType()} — BEFORE either is ever passed to {@link Class#forName(String)}.
     * Without this gate, any class name an attacker could write into the Redis value backing this
     * serializer would be resolved and deserialized (arbitrary-type deserialization / gadget-chain
     * risk).
     *
     * <p>Default: the {@code br.com.sawcunhaos.} package convention shared by every module in this
     * ecosystem (this serializer is not jdempotent-only — e.g. {@code security-starter}'s
     * {@code ScosSecurityContext} already goes through the same {@code ScosCacheConfiguration}
     * default with no per-caller configuration today) plus {@code java.math.}/{@code java.time.}/
     * {@code java.util.} (needed for {@code BigDecimal}, {@code java.time.*}, and every JDK
     * collection/{@code Optional}/{@code UUID} type AC #2 requires — a precise leaf-name allowlist
     * can't cover collections cheaply, since e.g. {@code List.of(...)}/{@code Map.of(...)}'s runtime
     * class differs by element count, {@code List12} vs. {@code ListN}, etc.) and exactly one
     * {@code java.lang} type, {@link #ALLOWED_JAVA_LANG_TYPE} ({@code String} is the only
     * {@code java.lang} type any consumer needs — unlike {@code java.util}, blanket-trusting
     * {@code java.lang} bought nothing but risk: {@code java.lang.Thread} was confirmed
     * instantiable through this exact gate with no legitimate reason to ever be a cached value
     * here). Anything else — third-party library classes in particular, the actual source of known
     * Jackson gadget chains — is rejected unless the consumer explicitly adds it via the
     * constructor.
     */
    private static final String DOMAIN_PACKAGE_PREFIX = "br.com.sawcunhaos.";

    private static final Set<String> ALLOWED_JDK_PACKAGE_PREFIXES = Set.of(
            "java.math.",
            "java.time.",
            "java.util."
    );

    private static final String ALLOWED_JAVA_LANG_TYPE = "java.lang.String";

    private final ObjectMapper mapper;
    private final Set<String> extraAllowedTypeNames;

    /**
     * Cria o serializador apenas com a allowlist padrão (sem tipos extras).
     */
    public PolymorphicRedisSerializer() {
        this(Set.of());
    }

    /**
     * Cria o serializador com a allowlist padrão mais tipos extras.
     *
     * @param extraAllowedTypeNames fully-qualified class names allowed in addition to the default
     *                              allowlist ({@link #DOMAIN_PACKAGE_PREFIX},
     *                              {@link #ALLOWED_JDK_PACKAGE_PREFIXES} and
     *                              {@link #ALLOWED_JAVA_LANG_TYPE}) — for a consumer caching a type
     *                              this serializer doesn't already cover by convention (Story 3.16,
     *                              Task 2).
     */
    public PolymorphicRedisSerializer(Set<String> extraAllowedTypeNames) {
        this.extraAllowedTypeNames = Set.copyOf(extraAllowedTypeNames);
        this.mapper = SmileMapper.builder()
                // NÃO pretty print
                .disable(SerializationFeature.INDENT_OUTPUT)
                .disable(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES)
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)

                // Suporte Java Time
                .findAndAddModules()
                .build();
    }

    /**
     * @param elementType concrete class name of the collection's elements, or {@code null} when
     *                     {@code type} isn't a {@link Collection} (or is empty/all-null, in which
     *                     case there is nothing to lose). Story 3.16 (Task 4): a bare
     *                     {@code Class.forName(type)} + {@code treeToValue} round-trip loses the
     *                     element type for a generically-parameterized collection (e.g.
     *                     {@code List<CustomType>}) because {@code value.getClass()} only ever
     *                     yields the raw container class (generics are erased at runtime) —
     *                     elements would come back as {@code LinkedHashMap} instead of the original
     *                     type. Captured from the first non-null element at serialize time so
     *                     deserialize can rebuild the precise {@code List<CustomType>} JavaType.
     */
    record Payload(String type, String elementType, JsonNode value) {}

    /**
     * Serializa o valor como {@link Payload} em Smile.
     *
     * @param value objeto a gravar; {@code null} gera {@code byte[0]}
     * @return bytes do envelope
     * @throws SerializationException se o Jackson falhar ao converter o valor
     */
    @Override
    public byte[] serialize(Object value) {
        try {
            if (value == null) return new byte[0];

            String elementType = value instanceof Collection<?> collection
                    ? firstNonNullElementTypeName(collection)
                    : null;

            Payload payload = new Payload(
                    value.getClass().getName(),
                    elementType,
                    mapper.valueToTree(value)
            );

            return mapper.writeValueAsBytes(payload);
        } catch (Exception e) {
            throw new SerializationException("Erro ao serializar", e);
        }
    }

    /**
     * {@code null} for an empty/all-null collection (nothing to lose) AND for a heterogeneous one —
     * the {@code elementType} this feeds into {@code constructCollectionType} in {@link #deserialize}
     * would otherwise be force-applied to every element, breaking a mixed collection like
     * {@code List.of(42, "text")} that round-tripped fine before this story. Only a collection whose
     * non-null elements all share one concrete class gets the precise-type treatment; a mixed one
     * falls back to the pre-story raw-container behavior.
     */
    private static String firstNonNullElementTypeName(Collection<?> collection) {
        String elementType = null;
        for (Object element : collection) {
            if (element == null) {
                continue;
            }
            String currentType = element.getClass().getName();
            if (elementType == null) {
                elementType = currentType;
            } else if (!elementType.equals(currentType)) {
                return null;
            }
        }
        return elementType;
    }

    /**
     * Reconstrói o objeto gravado por {@link #serialize(Object)}, validando o tipo contra a
     * allowlist antes de resolvê-lo.
     *
     * @param bytes envelope lido do Redis; {@code null} ou vazio gera {@code null}
     * @return o objeto reconstruído
     * @throws SerializationException se o tipo não for permitido, não existir ou o conteúdo for
     *                                inválido
     */
    @Override
    public Object deserialize(byte[] bytes) {
        try {
            if (bytes == null || bytes.length == 0) return null;

            Payload payload = mapper.readValue(bytes, Payload.class);
            // Ordem importa: a allowlist roda dentro de resolveAllowedType, antes do Class.forName.
            Class<?> clazz = resolveAllowedType(payload.type());

            if (payload.elementType() == null) {
                return mapper.treeToValue(payload.value(), clazz);
            }

            Class<?> elementClazz = resolveAllowedType(payload.elementType());
            JavaType collectionType = mapper.getTypeFactory()
                    .constructCollectionType(clazz.asSubclass(Collection.class), elementClazz);
            return mapper.treeToValue(payload.value(), collectionType);
        } catch (SerializationException e) {
            // Rejeição da allowlist já é SerializationException: relança sem embrulhar de novo.
            throw e;
        } catch (Exception e) {
            throw new SerializationException("Erro ao desserializar", e);
        }
    }

    /**
     * Story 3.16 (AC #1): rejects with {@link SerializationException} BEFORE ever calling
     * {@link Class#forName(String)} — a disallowed name is never resolved, so it can never be
     * loaded or run a static initializer.
     */
    private Class<?> resolveAllowedType(String typeName) throws ClassNotFoundException {
        if (!isAllowedTypeName(typeName)) {
            throw new SerializationException("Tipo não permitido para desserialização: " + typeName);
        }
        return Class.forName(typeName);
    }

    private boolean isAllowedTypeName(String typeName) {
        if (extraAllowedTypeNames.contains(typeName)) {
            return true;
        }
        // Só String em java.lang: liberar o pacote todo permitiria, p.ex., java.lang.Thread.
        if (typeName.equals(ALLOWED_JAVA_LANG_TYPE)) {
            return true;
        }
        if (isAllowedArrayTypeName(typeName)) {
            return true;
        }
        if (typeName.startsWith(DOMAIN_PACKAGE_PREFIX)) {
            return true;
        }
        for (String prefix : ALLOWED_JDK_PACKAGE_PREFIXES) {
            if (typeName.startsWith(prefix)) {
                return true;
            }
        }
        return false;
    }

    /**
     * A JVM array type descriptor (e.g. {@code "[I"}, {@code "[Ljava.lang.String;"}) doesn't match
     * a plain package-prefix check. A primitive-array descriptor is always safe — no class is
     * resolved for it. An object-array descriptor is allowed only if its component type is.
     * Multi-dimensional arrays are outside this story's scope and are rejected.
     */
    private boolean isAllowedArrayTypeName(String typeName) {
        if (typeName.length() < 2 || typeName.charAt(0) != '[' || typeName.charAt(1) == '[') {
            return false;
        }
        if (typeName.charAt(1) != 'L') {
            return true;
        }
        String componentName = typeName.substring(2, typeName.length() - 1);
        return isAllowedTypeName(componentName);
    }
}
