
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

package br.com.sawcunhaos.foundation.jdempotent.core.generator;

import br.com.sawcunhaos.foundation.jdempotent.api.KeySource;
import br.com.sawcunhaos.foundation.jdempotent.core.aspect.IdempotentAspect;
import br.com.sawcunhaos.foundation.jdempotent.core.model.IdempotencyKey;
import br.com.sawcunhaos.foundation.jdempotent.core.model.IdempotentRequestWrapper;
import br.com.sawcunhaos.foundation.jdempotent.core.utils.IdempotentTestPayload;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Story 3.13 (Task 5): {@link IdempotencyKeyResolver}'s header-then-fields precedence, tested at
 * the resolver level directly — same pattern {@code IdempotencyKeyResolverTest} (Story 3.12)
 * already uses for the AOP-free proof that this class is usable outside {@code IdempotentAspect}.
 * A web request context is simulated via {@link RequestContextHolder}/{@link MockHttpServletRequest}
 * (a standard Spring test technique), rather than a full {@code MockMvc}/AOP round trip, since
 * {@code IdempotentAspect} only forwards {@code keySource}/{@code headerName} to this class
 * unchanged (covered separately, at the wiring level, by
 * {@code IdempotentAspectTest#given_idempotency_key_header_present_...}).
 */
class IdempotencyKeyResolverHeaderSourceTest {

    private static final String HEADER_NAME = "Idempotency-Key";

    private final IdempotentAspect idempotentAspect = new IdempotentAspect();
    private final IdempotencyKeyResolver resolver = new IdempotencyKeyResolver();

    @AfterEach
    void clearRequestContext() {
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    void should_derive_the_key_from_the_header_when_present_regardless_of_the_payload_fields() throws IllegalAccessException {
        //given: a bound web request carrying the header, and two payloads with DIFFERENT field
        //values (would produce different keys under FIELDS_ONLY)
        bindRequestWithHeader(HEADER_NAME, "client-supplied-key");
        IdempotentTestPayload payloadA = new IdempotentTestPayload("bodyA");
        IdempotentTestPayload payloadB = new IdempotentTestPayload("bodyB");

        //when
        IdempotencyKey keyFromA = resolveWithHeaderSource(payloadA);
        IdempotencyKey keyFromB = resolveWithHeaderSource(payloadB);

        //then: AC #1 — same header wins over different body fields, key is derived from it
        assertEquals(keyFromA.getKeyValue(), keyFromB.getKeyValue());
        assertNotEquals(resolveFieldsOnly(payloadA).getKeyValue(), keyFromA.getKeyValue());
    }

    @Test
    void should_fall_back_to_annotated_fields_when_header_is_absent() throws IllegalAccessException {
        //given: a bound web request with no Idempotency-Key header at all
        bindRequestWithHeader(null, null);
        IdempotentTestPayload payload = new IdempotentTestPayload("body");

        //when
        IdempotencyKey headerSourceKey = resolveWithHeaderSource(payload);
        IdempotencyKey fieldsOnlyKey = resolveFieldsOnly(payload);

        //then: precedence falls through to the annotated fields (Story 3.12 behavior, unchanged)
        assertEquals(fieldsOnlyKey.getKeyValue(), headerSourceKey.getKeyValue());
    }

    @Test
    void should_fall_back_to_annotated_fields_without_throwing_when_there_is_no_web_request_context() throws IllegalAccessException {
        //given: no RequestContextHolder attributes bound at all — simulates a messaging listener
        //invoking the annotated method outside a DispatcherServlet (AC #2)
        RequestContextHolder.resetRequestAttributes();
        IdempotentTestPayload payload = new IdempotentTestPayload("body");

        //when: must not throw, and must fall back to the same key FIELDS_ONLY would produce
        IdempotencyKey headerSourceKey = resolveWithHeaderSource(payload);
        IdempotencyKey fieldsOnlyKey = resolveFieldsOnly(payload);

        //then
        assertEquals(fieldsOnlyKey.getKeyValue(), headerSourceKey.getKeyValue());
    }

    @Test
    void should_never_use_x_request_id_as_the_idempotency_key_source() throws IllegalAccessException {
        //given: only X-Request-ID is present — the configured header (Idempotency-Key) is not
        bindRequestWithHeader("X-Request-ID", "some-correlation-id");
        IdempotentTestPayload payload = new IdempotentTestPayload("body");

        //when
        IdempotencyKey headerSourceKey = resolveWithHeaderSource(payload);
        IdempotencyKey fieldsOnlyKey = resolveFieldsOnly(payload);

        //then: X-Request-ID is never read as a fallback, precedence still falls through to fields
        assertEquals(fieldsOnlyKey.getKeyValue(), headerSourceKey.getKeyValue());
    }

    @Test
    void should_throw_when_key_source_is_header_then_fields_but_cache_prefix_is_blank() throws IllegalAccessException {
        // Review patch (severity high): a blank cachePrefix under HEADER_THEN_FIELDS means the
        // only remaining differentiator between unrelated methods disappears — fail fast instead
        // of silently colliding two unrelated operations on the same header value.
        bindRequestWithHeader(HEADER_NAME, "client-supplied-key");
        IdempotentRequestWrapper wrapped = wrap(new IdempotentTestPayload("body"));

        assertThrows(IllegalStateException.class,
                () -> resolver.resolve(wrapped, "", KeySource.HEADER_THEN_FIELDS, HEADER_NAME));
    }

    @Test
    void should_throw_when_key_source_is_header_then_fields_but_header_name_is_blank() throws IllegalAccessException {
        // Review patch (severity medium): a blank headerName under HEADER_THEN_FIELDS means the
        // header can never be read — fail fast instead of silently degrading to FIELDS_ONLY.
        bindRequestWithHeader(HEADER_NAME, "client-supplied-key");
        IdempotentRequestWrapper wrapped = wrap(new IdempotentTestPayload("body"));

        assertThrows(IllegalStateException.class,
                () -> resolver.resolve(wrapped, "listener", KeySource.HEADER_THEN_FIELDS, ""));
    }

    @Test
    void should_trim_the_header_value_before_using_it_as_the_key_source() throws IllegalAccessException {
        // Review patch (severity low-medium): incidental leading/trailing whitespace must not
        // defeat deduplication.
        IdempotentTestPayload payload = new IdempotentTestPayload("body");

        bindRequestWithHeader(HEADER_NAME, "  client-supplied-key  ");
        IdempotencyKey keyWithWhitespace = resolveWithHeaderSource(payload);

        bindRequestWithHeader(HEADER_NAME, "client-supplied-key");
        IdempotencyKey keyWithoutWhitespace = resolveWithHeaderSource(payload);

        assertEquals(keyWithoutWhitespace.getKeyValue(), keyWithWhitespace.getKeyValue());
    }

    @Test
    void should_fall_back_to_annotated_fields_without_throwing_when_getHeader_itself_throws() throws IllegalAccessException {
        // Review patch (severity medium): a recycled/completed HttpServletRequest (e.g. an
        // async-dispatch edge case) can make getHeader() itself throw IllegalStateException — AC
        // #2's "never throws" covers this too, not just the no-web-context case.
        HttpServletRequest recycledRequest = mock(HttpServletRequest.class);
        when(recycledRequest.getHeader(HEADER_NAME)).thenThrow(new IllegalStateException("recycled request"));
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(recycledRequest));
        IdempotentTestPayload payload = new IdempotentTestPayload("body");

        //when: must not throw, falls back to the same key FIELDS_ONLY would produce
        IdempotencyKey headerSourceKey = resolveWithHeaderSource(payload);
        IdempotencyKey fieldsOnlyKey = resolveFieldsOnly(payload);

        //then
        assertEquals(fieldsOnlyKey.getKeyValue(), headerSourceKey.getKeyValue());
    }

    private void bindRequestWithHeader(String headerName, String headerValue) {
        MockHttpServletRequest httpRequest = new MockHttpServletRequest();
        if (headerName != null) {
            httpRequest.addHeader(headerName, headerValue);
        }
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(httpRequest));
    }

    private IdempotencyKey resolveWithHeaderSource(Object payload) throws IllegalAccessException {
        return resolver.resolve(wrap(payload), "listener", KeySource.HEADER_THEN_FIELDS, HEADER_NAME);
    }

    private IdempotencyKey resolveFieldsOnly(Object payload) throws IllegalAccessException {
        return resolver.resolve(wrap(payload), "listener");
    }

    private IdempotentRequestWrapper wrap(Object payload) throws IllegalAccessException {
        return new IdempotentRequestWrapper(idempotentAspect.getIdempotentNonIgnorableWrapper(List.of(payload)));
    }
}
