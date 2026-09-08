
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
import br.com.sawcunhaos.foundation.jdempotent.core.constant.CryptographyAlgorithm;
import br.com.sawcunhaos.foundation.jdempotent.core.model.IdempotencyKey;
import br.com.sawcunhaos.foundation.jdempotent.core.model.IdempotentRequestWrapper;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.security.MessageDigest;

/**
 * Story 3.12 (AC #2): the single point that turns an already-collected
 * {@link IdempotentRequestWrapper} into the final {@link IdempotencyKey}. Canonical field
 * composition (deterministic {@code TreeMap} ordering, {@code @JdempotentId} exclusion)
 * happens upstream, in the {@code annotationChain} that builds the wrapper — see
 * {@code IdempotentAspect#getIdempotentNonIgnorableWrapper} — so by the time a wrapper reaches
 * this class it is already canonical; this class' job is hashing it into a key.
 *
 * <p>Deliberately takes no {@code ProceedingJoinPoint}/AspectJ type — only plain data (a
 * request wrapper and a listener/prefix name) — so it is reusable, unchanged, by any future
 * entrypoint (HTTP via {@code IdempotentAspect} today, messaging tomorrow per Story 3.13)
 * without duplicating key-composition logic.</p>
 *
 * <p>Delegates the hash+prefix mechanics to the {@link KeyGenerator} supplied at construction
 * (a {@link DefaultKeyGenerator} in practice) so that already-tested mechanism keeps behaving
 * identically (NFR4) — this class additionally owns the digest/algorithm plumbing so callers
 * never have to manage a {@link MessageDigest} themselves.</p>
 *
 * <p>Story 3.13 (review patch): the Story 3.6 payload-mismatch check that
 * {@code IdempotentAspect#execute()} performs against a stored key ({@code payloadHash}) is
 * always computed from the annotated fields (the raw {@code requestObject}), never from the
 * header — this is independent of {@code keySource} and unaffected by this class, since that
 * hash is computed by the aspect directly from the request wrapper, not through this resolver.
 * A header-derived key can therefore still collide with a stored, field-derived payload hash
 * exactly like any other key would.</p>
 */
public class IdempotencyKeyResolver {

    private final KeyGenerator keyGenerator;

    /**
     * Story 3.12 (patch): mirrors {@code IdempotentAspect}'s own {@code StringBuilder} pool so
     * this resolver doesn't reallocate one per call — kept private to this class (rather than
     * shared with the aspect) so the resolver stays free of any dependency on {@code
     * IdempotentAspect}, per AC #2.
     */
    private static final ThreadLocal<StringBuilder> stringBuilders =
            new ThreadLocal<>() {
                @Override
                protected StringBuilder initialValue() {
                    return new StringBuilder();
                }

                @Override
                public StringBuilder get() {
                    StringBuilder builder = super.get();
                    builder.setLength(0);
                    return builder;
                }
            };

    public IdempotencyKeyResolver() {
        this(new DefaultKeyGenerator());
    }

    public IdempotencyKeyResolver(KeyGenerator keyGenerator) {
        this.keyGenerator = keyGenerator;
    }

    public IdempotencyKey resolve(IdempotentRequestWrapper requestObject, String listenerName) {
        return resolve(requestObject, listenerName, KeySource.FIELDS_ONLY, null);
    }

    /**
     * Story 3.13 (AC #1, #2): {@code keySource}/{@code headerName} let a caller opt into
     * deriving the key from an HTTP request header instead of the annotated fields. Precedence
     * is header (when present) &gt; annotated fields &gt; hash — the last two stages are exactly
     * what {@link #resolve(IdempotentRequestWrapper, String)} already did before this story,
     * reused unchanged here by simply hashing the header value the same way fields are hashed
     * (wrapping it in an {@link IdempotentRequestWrapper} and delegating to the same
     * {@link KeyGenerator}), rather than inventing a second key format. Never reads
     * {@code X-Request-ID} or any header other than the one named by {@code headerName}.
     *
     * @param requestObject the already-collected, canonical field composition (fallback source)
     * @param listenerName  cache-prefix/listener name, forwarded to the key generator unchanged
     * @param keySource     {@link KeySource#FIELDS_ONLY} skips header lookup entirely
     * @param headerName    header to read when {@code keySource} is
     *                      {@link KeySource#HEADER_THEN_FIELDS}; ignored otherwise
     */
    public IdempotencyKey resolve(IdempotentRequestWrapper requestObject, String listenerName,
                                   KeySource keySource, String headerName) {
        if (keySource == KeySource.HEADER_THEN_FIELDS) {
            requireUsableHeaderConfiguration(listenerName, headerName);
        }
        String headerValue = keySource == KeySource.HEADER_THEN_FIELDS ? readHeader(headerName) : null;
        IdempotentRequestWrapper effectiveRequest =
                headerValue != null ? new IdempotentRequestWrapper(headerValue) : requestObject;
        MessageDigest messageDigest = CryptographyAlgorithm.SHA256.newDigest();
        return keyGenerator.generateIdempotentKey(effectiveRequest, listenerName, stringBuilders.get(), messageDigest);
    }

    /**
     * Story 3.13 (review patch, severities high/medium): {@code KeySource.HEADER_THEN_FIELDS}
     * removes the annotated fields from key composition whenever the header is present — the
     * cache-prefix ({@code listenerName}, from {@code @JdempotentResource#cachePrefix()}) is then
     * the <em>only</em> thing left distinguishing one method from another under the same header
     * value. Left unblank, two unrelated methods both using {@code HEADER_THEN_FIELDS} with no
     * explicit {@code cachePrefix()} (default {@code ""}) would collide on the exact same
     * {@link IdempotencyKey} for a client reusing the same header value — an unrelated-operation
     * lock/response reuse, not a false positive. Similarly, a blank {@code headerName()} would
     * make the header unreadable and silently degrade to {@code FIELDS_ONLY}, masking what is
     * almost certainly a forgotten configuration step. Both fail fast here instead of silently
     * misbehaving in production.
     */
    private static void requireUsableHeaderConfiguration(String listenerName, String headerName) {
        if (StringUtils.isBlank(listenerName)) {
            throw new IllegalStateException(
                    "@JdempotentResource(keySource=HEADER_THEN_FIELDS) requires a non-blank cachePrefix(): "
                            + "without it, the annotated fields no longer compose the key and every method "
                            + "using this key source with the default cachePrefix would collide on the same "
                            + "header value, regardless of which method actually received the request.");
        }
        if (StringUtils.isBlank(headerName)) {
            throw new IllegalStateException(
                    "@JdempotentResource(keySource=HEADER_THEN_FIELDS) requires a non-blank headerName(): "
                            + "it is blank/unset, so the header can never be read and the key would silently "
                            + "fall back to the annotated fields.");
        }
    }

    /**
     * Story 3.13 (AC #2): outside a web request (e.g. a messaging listener invoking the
     * annotated method directly, with no {@code DispatcherServlet} involved)
     * {@link RequestContextHolder} simply has nothing bound to the current thread — this returns
     * {@code null} cleanly in that case (and when the header itself is blank/absent) instead of
     * letting a {@code ClassCastException}/{@code NullPointerException} escape, so the caller
     * falls back to the next precedence stage (annotated fields) rather than failing the call.
     *
     * <p>Story 3.13 (review patch, severity medium): also treats an {@link IllegalStateException}
     * from {@code getHeader()} itself (a recycled/completed {@code HttpServletRequest}, e.g. an
     * async-dispatch edge case) the same way — AC #2's "never throws" is about this method as a
     * whole, not just the "no web context at all" case.</p>
     */
    private static String readHeader(String headerName) {
        if (StringUtils.isBlank(headerName)) {
            return null;
        }
        RequestAttributes attributes = RequestContextHolder.getRequestAttributes();
        if (!(attributes instanceof ServletRequestAttributes servletRequestAttributes)) {
            return null;
        }
        try {
            // Story 3.13 (review patch, severity low-medium): trimmed so a header sent with
            // incidental leading/trailing whitespace (e.g. " abc123") still dedupes against the
            // same value sent without it — untrimmed, the two would silently compose different
            // keys and defeat the very deduplication this key source exists for.
            String headerValue = StringUtils.trim(servletRequestAttributes.getRequest().getHeader(headerName));
            return StringUtils.isBlank(headerValue) ? null : headerValue;
        } catch (IllegalStateException e) {
            return null;
        }
    }
}
