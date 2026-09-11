
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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.serializer.SerializationException;
import tools.jackson.databind.ObjectMapper;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.net.URI;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Story 3.16. Two concerns:
 * <ul>
 *     <li>AC #1 — {@code deserialize()} rejects a type outside the allowlist before ever calling
 *     {@code Class.forName} on it.</li>
 *     <li>AC #2 — serialize/deserialize round-trips a representative set of value types without
 *     losing type or precision (module had zero unit tests before this story).</li>
 * </ul>
 *
 * <p>Not covered here, per the story's Dev Notes ("via teste ou nota, decisão do Dev"):
 * <ul>
 *     <li>A "very large object" — would only exercise generic Jackson/Smile streaming, nothing
 *     specific to this serializer's own logic, at the cost of a slow/synthetic test.</li>
 *     <li>{@code void}/{@code ResponseEntity<T>} — those are {@code jdempotent}-specific storage
 *     shapes (already covered by its own repository IT tests), not something this serializer,
 *     which only ever sees a plain {@code Object}, has distinct behavior for.</li>
 * </ul>
 * The circular-reference case IS covered below ({@link #serialize_circularReference_failsCleanly()}).
 */
class PolymorphicRedisSerializerTest {

    private PolymorphicRedisSerializer serializer;

    @BeforeEach
    void setUp() {
        serializer = new PolymorphicRedisSerializer();
    }

    private Object roundTrip(Object value) {
        return serializer.deserialize(serializer.serialize(value));
    }

    // ---- AC #1: allowlist -------------------------------------------------

    @Test
    void deserialize_allowedDomainType_roundTripsWithoutRegression() {
        SimplePojo original = new SimplePojo("Ada", 36);

        assertEquals(original, roundTrip(original));
    }

    @Test
    void deserialize_disallowedRealJdkType_rejectsWithSerializationExceptionAndNoResolutionAttempt() {
        // java.net.URI: a real, resolvable JDK class outside the default allowlist (not under
        // java.lang/java.math/java.time/java.util, nor br.com.sawcunhaos.*).
        byte[] bytes = serializer.serialize(URI.create("https://example.com/x"));

        SerializationException ex = assertThrows(SerializationException.class, () -> serializer.deserialize(bytes));
        assertTrue(ex.getMessage().contains("java.net.URI"));
        // No wrapped cause: proves the class was never handed to Class.forName/treeToValue — the
        // allowlist check rejected it first, exactly as Task 2/3 require.
        assertNull(ex.getCause());
    }

    @Test
    void deserialize_unknownDisallowedType_rejectsWithoutAttemptingClassResolution() throws Exception {
        // A class name that isn't even on the classpath. If the allowlist check ran AFTER
        // Class.forName (or not at all), this would fail with a wrapped ClassNotFoundException
        // instead — the null cause here proves resolution was never attempted.
        byte[] bytes = payloadBytes("com.example.NotOnClasspath$Malicious", null, "does-not-matter");

        SerializationException ex = assertThrows(SerializationException.class, () -> serializer.deserialize(bytes));
        assertTrue(ex.getMessage().contains("com.example.NotOnClasspath$Malicious"));
        assertNull(ex.getCause());
    }

    @Test
    void deserialize_malformedTypeString_failsCleanlyWithoutLeakingInternalDetails() throws Exception {
        byte[] bytes = payloadBytes("", null, "does-not-matter");

        SerializationException ex = assertThrows(SerializationException.class, () -> serializer.deserialize(bytes));
        assertNull(ex.getCause());
        // Clear, generic message — not a stack trace or classloader internals.
        assertTrue(ex.getMessage().startsWith("Tipo não permitido"));
    }

    @Test
    void deserialize_extraAllowedTypeName_allowsConsumerConfiguredType() {
        PolymorphicRedisSerializer extended = new PolymorphicRedisSerializer(Set.of(URI.class.getName()));
        URI original = URI.create("https://example.com/x");

        byte[] bytes = extended.serialize(original);

        assertEquals(original, extended.deserialize(bytes));
    }

    @Test
    void deserialize_javaLangTypeOutsideTheAllowedLeaf_isRejected() throws Exception {
        // Regression coverage: the default allowlist used to blanket-trust the whole java.lang.
        // package, which let java.lang.Thread through even though String is the only java.lang type
        // any consumer needs. java.util. stays a package-prefix trust (see class javadoc for why —
        // List.of()/Map.of()'s runtime class differs by element count), so java.util.Random remains
        // an accepted residual risk, same as before this fix; only java.lang. was narrowed, since it
        // was the one confirmed to admit a class (Thread) with zero legitimate use here.
        byte[] threadBytes = payloadBytes("java.lang.Thread", null, "does-not-matter");

        assertThrows(SerializationException.class, () -> serializer.deserialize(threadBytes));
    }

    @Test
    void roundTrip_listOfAndSetOfFactoryCollections_stillWork() {
        // Regression coverage: java.util. must stay a package-prefix trust, not an exact-leaf-name
        // one — List.of()/Set.of()'s runtime class (e.g. ImmutableCollections$List12/ListN) differs
        // by element count and isn't practical to enumerate, unlike ArrayList/HashMap.
        assertEquals(List.of("a", "b"), roundTrip(List.of("a", "b")));
        assertEquals(Set.of("a", "b"), roundTrip(Set.of("a", "b")));
    }

    @Test
    void deserialize_disallowedElementType_rejectsBeforeResolvingElementClass() throws Exception {
        // Container type (ArrayList) is allowed; elementType is not — must still be rejected before
        // Class.forName, exactly like the top-level type.
        byte[] bytes = payloadBytes("java.util.ArrayList", "java.lang.Thread", List.of("x"));

        SerializationException ex = assertThrows(SerializationException.class, () -> serializer.deserialize(bytes));
        assertTrue(ex.getMessage().contains("java.lang.Thread"));
        assertNull(ex.getCause());
    }

    @Test
    void deserialize_arrayTypeName_rejectsDisallowedComponentAndMultiDimensional() throws Exception {
        // Object array whose component type isn't allowed.
        byte[] uriArrayBytes = payloadBytes("[Ljava.net.URI;", null, List.of());
        assertThrows(SerializationException.class, () -> serializer.deserialize(uriArrayBytes));

        // Multi-dimensional arrays are outside this story's scope — rejected outright.
        byte[] multiDimBytes = payloadBytes("[[I", null, List.of());
        assertThrows(SerializationException.class, () -> serializer.deserialize(multiDimBytes));
    }

    @Test
    void deserialize_allowlistedPrefixButNonExistentClass_wrapsClassNotFoundException() throws Exception {
        // Passes the allowlist (domain package prefix) but doesn't exist — must still fail cleanly
        // via the existing generic catch, not leak past deserialize() unhandled.
        byte[] bytes = payloadBytes("br.com.sawcunhaos.NoSuchClass", null, "does-not-matter");

        SerializationException ex = assertThrows(SerializationException.class, () -> serializer.deserialize(bytes));
        assertInstanceOf(ClassNotFoundException.class, ex.getCause());
    }

    // ---- AC #2: round-trip coverage ---------------------------------------

    @Test
    void roundTrip_string() {
        assertEquals("hello world", roundTrip("hello world"));
    }

    @Test
    void roundTrip_simpleCustomObject() {
        SimplePojo original = new SimplePojo("Grace", 85);

        assertEquals(original, roundTrip(original));
    }

    @Test
    void roundTrip_bigDecimal() {
        BigDecimal original = new BigDecimal("12345.6789");

        assertEquals(original, roundTrip(original));
    }

    @Test
    void roundTrip_localDate() {
        LocalDate original = LocalDate.of(2026, 8, 30);

        assertEquals(original, roundTrip(original));
    }

    @Test
    void roundTrip_localDateTime() {
        LocalDateTime original = LocalDateTime.of(2026, 8, 30, 14, 5, 1);

        assertEquals(original, roundTrip(original));
    }

    @Test
    void roundTrip_instant() {
        Instant original = Instant.parse("2026-08-30T14:05:01.123Z");

        assertEquals(original, roundTrip(original));
    }

    @Test
    void roundTrip_enum() {
        assertEquals(Color.GREEN, roundTrip(Color.GREEN));
    }

    @Test
    void roundTrip_uuid() {
        UUID original = UUID.randomUUID();

        assertEquals(original, roundTrip(original));
    }

    @Test
    void roundTrip_intArray() {
        int[] original = {1, 2, 3, 42};

        assertArrayEquals(original, (int[]) roundTrip(original));
    }

    @Test
    void roundTrip_stringArray() {
        String[] original = {"a", "b", "c"};

        assertArrayEquals(original, (String[]) roundTrip(original));
    }

    @Test
    void roundTrip_map() {
        Map<String, Object> original = Map.of("a", 1, "b", "two", "c", true);

        assertEquals(original, roundTrip(original));
    }

    @Test
    void roundTrip_optionalFieldPresent() {
        WithOptional original = new WithOptional(Optional.of("value"));

        assertEquals(original, roundTrip(original));
    }

    @Test
    void roundTrip_optionalFieldEmpty() {
        WithOptional original = new WithOptional(Optional.empty());

        assertEquals(original, roundTrip(original));
    }

    @Test
    void roundTrip_nestedObject() {
        Outer original = new Outer(new Inner("nested-value"));

        assertEquals(original, roundTrip(original));
    }

    @Test
    void roundTrip_objectWithNullField() {
        SimplePojo original = new SimplePojo(null, 10);

        assertEquals(original, roundTrip(original));
    }

    @Test
    void roundTrip_nullWholeCachedValue() {
        assertNull(serializer.deserialize(serializer.serialize(null)));
        assertNull(serializer.deserialize(null));
        assertNull(serializer.deserialize(new byte[0]));
    }

    @Test
    void roundTrip_listOfCustomObjects_preservesConcreteElementType() {
        // Regression coverage for the type-erasure gap identified in Story 3.19's review: a bare
        // Class.forName(payload.type()) only ever yields the raw container class (e.g. ArrayList),
        // so without the elementType tracking added in this story, elements would come back as
        // LinkedHashMap instead of Inner.
        List<Inner> original = new ArrayList<>(List.of(new Inner("a"), new Inner("b")));

        Object result = roundTrip(original);

        assertEquals(original, result);
        List<?> resultList = (List<?>) result;
        assertInstanceOf(Inner.class, resultList.get(0));
        assertInstanceOf(Inner.class, resultList.get(1));
    }

    @Test
    void roundTrip_heterogeneousCollection_fallsBackToRawContainerBehavior() {
        // Mixed-type elements: elementType tracking must not force the first element's class onto
        // every element — this round-tripped fine before this story via the raw container class
        // alone, and must keep doing so.
        List<Object> original = new ArrayList<>(List.of(42, "text"));

        assertEquals(original, roundTrip(original));
    }

    @Test
    void serialize_circularReference_failsCleanly() {
        // Documents the Task 4 requirement ("falha clara vs. hang vs. StackOverflowError") rather
        // than fixing it — not asked for by this story. Jackson's default nesting-depth guard (or,
        // failing that, a StackOverflowError) turns an unbounded cycle into a clear failure instead
        // of a hang either way; assert only on Throwable so this test doesn't depend on which of
        // the two Jackson happens to hit first.
        Node a = new Node("a");
        Node b = new Node("b");
        a.next = b;
        b.next = a;

        assertThrows(Throwable.class, () -> serializer.serialize(a));
    }

    // ---- test fixtures ------------------------------------------------

    private byte[] payloadBytes(String type, String elementType, Object value) throws Exception {
        Field mapperField = PolymorphicRedisSerializer.class.getDeclaredField("mapper");
        mapperField.setAccessible(true);
        ObjectMapper mapper = (ObjectMapper) mapperField.get(serializer);

        PolymorphicRedisSerializer.Payload payload =
                new PolymorphicRedisSerializer.Payload(type, elementType, mapper.valueToTree(value));
        return mapper.writeValueAsBytes(payload);
    }

    record SimplePojo(String name, int age) {}

    record Inner(String value) {}

    record Outer(Inner inner) {}

    record WithOptional(Optional<String> value) {}

    enum Color {RED, GREEN, BLUE}

    static class Node {
        public String name;
        public Node next;

        public Node() {
        }

        public Node(String name) {
            this.name = name;
        }
    }
}
