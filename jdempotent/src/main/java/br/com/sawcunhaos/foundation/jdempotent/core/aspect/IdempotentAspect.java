
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

package br.com.sawcunhaos.foundation.jdempotent.core.aspect;


import br.com.sawcunhaos.foundation.jdempotent.core.callback.ErrorConditionalCallback;
import br.com.sawcunhaos.foundation.jdempotent.core.chain.AnnotationChain;
import br.com.sawcunhaos.foundation.jdempotent.core.chain.JdempotentDefaultChain;
import br.com.sawcunhaos.foundation.jdempotent.core.chain.JdempotentIdAnnotationChain;
import br.com.sawcunhaos.foundation.jdempotent.core.chain.JdempotentIgnoreAnnotationChain;
import br.com.sawcunhaos.foundation.jdempotent.core.chain.JdempotentNoAnnotationChain;
import br.com.sawcunhaos.foundation.jdempotent.core.chain.JdempotentPropertyAnnotationChain;
import br.com.sawcunhaos.foundation.jdempotent.core.constant.CryptographyAlgorithm;
import br.com.sawcunhaos.foundation.jdempotent.core.datasource.IdempotentRepository;
import br.com.sawcunhaos.foundation.jdempotent.core.datasource.InMemoryIdempotentRepository;
import br.com.sawcunhaos.foundation.jdempotent.core.exception.IdempotentInProgressException;
import br.com.sawcunhaos.foundation.jdempotent.core.exception.IdempotentPayloadMismatchException;
import br.com.sawcunhaos.foundation.jdempotent.core.exception.IdempotentReplayedFailureException;
import br.com.sawcunhaos.foundation.jdempotent.core.generator.DefaultKeyGenerator;
import br.com.sawcunhaos.foundation.jdempotent.core.generator.IdempotencyKeyResolver;
import br.com.sawcunhaos.foundation.jdempotent.core.generator.KeyGenerator;
import br.com.sawcunhaos.foundation.jdempotent.core.metrics.IdempotencyMetrics;
import br.com.sawcunhaos.foundation.jdempotent.core.metrics.NoOpIdempotencyMetrics;
import br.com.sawcunhaos.foundation.jdempotent.core.model.CachedBusinessFailure;
import br.com.sawcunhaos.foundation.jdempotent.core.model.ChainData;
import br.com.sawcunhaos.foundation.jdempotent.core.model.IdempotencyKey;
import br.com.sawcunhaos.foundation.jdempotent.core.model.IdempotentIgnorableWrapper;
import br.com.sawcunhaos.foundation.jdempotent.core.model.IdempotentRequestWrapper;
import br.com.sawcunhaos.foundation.jdempotent.core.model.IdempotentResponseWrapper;
import br.com.sawcunhaos.foundation.jdempotent.core.model.KeyValuePair;
import br.com.sawcunhaos.foundation.jdempotent.core.model.Lease;
import br.com.sawcunhaos.foundation.jdempotent.api.IdempotentFailurePolicy;
import br.com.sawcunhaos.foundation.jdempotent.api.JdempotentId;
import br.com.sawcunhaos.foundation.jdempotent.api.JdempotentRequestPayload;
import br.com.sawcunhaos.foundation.jdempotent.api.JdempotentResource;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.InaccessibleObjectException;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;


/**
 * An aspect that used along with the @IdempotentResource annotation
 */
@Aspect
@Order(Ordered.HIGHEST_PRECEDENCE)
@Slf4j
@SuppressFBWarnings(value = "EI_EXPOSE_REP2",
        justification = "The IdempotentRepository is a Spring-injected collaborator stored by reference by design; it is not a value object to be copied.")
public class IdempotentAspect {
    private AnnotationChain annotationChain;
    private final KeyGenerator keyGenerator;
    /**
     * Story 3.12 (AC #2): the single point of idempotency-key composition, reusable outside
     * AOP. Wraps {@link #keyGenerator} rather than duplicating its hash+prefix logic, so the
     * already-configured namespace (Story 3.10) and already-tested behavior (NFR4) carry over
     * unchanged — see {@code IdempotencyKeyResolver}'s Javadoc.
     */
    private final IdempotencyKeyResolver keyResolver;
    @Setter
    @Getter
    private IdempotentRepository idempotentRepository;
    private ErrorConditionalCallback errorCallback;
    /**
     * Story 3.11 (FR7): defaults to the no-op implementation so every existing
     * constructor keeps working unchanged for consumers that don't wire metrics —
     * {@code ScosJdempotentConfig} overrides it via the setter with the resolved
     * {@code IdempotencyMetrics} bean (no-op or Micrometer-backed). {@code @Getter}
     * exists so the wiring itself is testable (confirms the instance set by
     * {@code ScosJdempotentConfig} is the one actually used).
     */
    @Setter
    @Getter
    private IdempotencyMetrics idempotencyMetrics = new NoOpIdempotencyMetrics();
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


    public IdempotentAspect() {
        this.idempotentRepository = new InMemoryIdempotentRepository();
        this.keyGenerator = new DefaultKeyGenerator();
        this.keyResolver = new IdempotencyKeyResolver(this.keyGenerator);
        this.annotationChain = fillChains();
    }

    public IdempotentAspect(ErrorConditionalCallback errorCallback) {
        this.errorCallback = errorCallback;
        this.idempotentRepository = new InMemoryIdempotentRepository();
        this.keyGenerator = new DefaultKeyGenerator();
        this.keyResolver = new IdempotencyKeyResolver(this.keyGenerator);
        this.annotationChain = fillChains();
    }

    public IdempotentAspect(IdempotentRepository idempotentRepository) {
        this.idempotentRepository = idempotentRepository;
        this.keyGenerator = new DefaultKeyGenerator();
        this.keyResolver = new IdempotencyKeyResolver(this.keyGenerator);
        this.annotationChain = fillChains();
    }

    public IdempotentAspect(IdempotentRepository idempotentRepository, ErrorConditionalCallback errorCallback) {
        this.idempotentRepository = idempotentRepository;
        this.errorCallback = errorCallback;
        this.keyGenerator = new DefaultKeyGenerator();
        this.keyResolver = new IdempotencyKeyResolver(this.keyGenerator);
        this.annotationChain = fillChains();
    }

    public IdempotentAspect(ErrorConditionalCallback errorCallback, DefaultKeyGenerator keyGenerator) {
        this.errorCallback = errorCallback;
        this.idempotentRepository = new InMemoryIdempotentRepository();
        this.keyGenerator = keyGenerator;
        this.keyResolver = new IdempotencyKeyResolver(this.keyGenerator);
        this.annotationChain = fillChains();
    }

    public IdempotentAspect(IdempotentRepository idempotentRepository, DefaultKeyGenerator keyGenerator) {
        this.idempotentRepository = idempotentRepository;
        this.keyGenerator = keyGenerator;
        this.keyResolver = new IdempotencyKeyResolver(this.keyGenerator);
        this.annotationChain = fillChains();
    }

    public IdempotentAspect(IdempotentRepository idempotentRepository, ErrorConditionalCallback errorCallback, DefaultKeyGenerator keyGenerator) {
        this.idempotentRepository = idempotentRepository;
        this.errorCallback = errorCallback;
        this.keyGenerator = keyGenerator;
        this.keyResolver = new IdempotencyKeyResolver(this.keyGenerator);
        this.annotationChain = fillChains();
    }

    /**
     * An advice to make sure it returns at the same time for all subsequent calls
     *
     * @param pjp
     * @return
     * @throws Throwable
     */
    @Around("@annotation(br.com.sawcunhaos.foundation.jdempotent.api.JdempotentResource)")
    public Object execute(ProceedingJoinPoint pjp) throws Throwable {
        String classAndMethodName = generateLogPrefixForIncomingEvent(pjp);
        IdempotentRequestWrapper requestObject = findIdempotentRequestArg(pjp);
        JdempotentResource resourceAnnotation = ((MethodSignature) pjp.getSignature()).getMethod().getAnnotation(JdempotentResource.class);
        String listenerName = resourceAnnotation.cachePrefix();
        MessageDigest messageDigest = CryptographyAlgorithm.SHA256.newDigest();
        // Story 3.12 (AC #2): key composition goes exclusively through IdempotencyKeyResolver
        // now, not a direct call to keyGenerator — the resolver is the single reusable point
        // any future entrypoint (HTTP here, messaging per Story 3.13) would also go through.
        // Story 3.13 (AC #1, #2): keySource/headerName are forwarded as-is — the resolver owns
        // the header-vs-fields precedence and the no-web-context fallback, this call site does
        // not need to know about either.
        IdempotencyKey idempotencyKey = keyResolver.resolve(
                requestObject, listenerName, resourceAnnotation.keySource(), resourceAnnotation.headerName());
        Long customTtl = resourceAnnotation.ttl();
        TimeUnit timeUnit = resourceAnnotation.ttlTimeUnit();
        Duration ttl = Duration.of(customTtl, timeUnit.toChronoUnit());
        // Compared against the payload hash already stored under the key
        // (Lease#getExistingPayloadHash) to tell a genuine duplicate call apart from a
        // different payload colliding on the same idempotency key (409 vs 422, Story 3.6).
        String payloadHash = HexFormat.of().formatHex(messageDigest.digest(requestObject.toString().getBytes(StandardCharsets.UTF_8)));

        log.debug(classAndMethodName + "starting for {}", requestObject);

        Lease lease = idempotentRepository.tryAcquire(idempotencyKey, payloadHash, ttl);
        if (!lease.isAcquired()) {
            if (lease.isMismatch()) {
                // AC #2: mismatch takes precedence over both "already in progress" and a
                // cached response, even in the race window where the first call (different
                // payload) is still processing — checked before hasCachedResponse() below.
                emitMetricSafely(idempotencyMetrics::mismatch, "mismatch");
                log.debug(classAndMethodName + "payload mismatch for {}", requestObject);
                throw new IdempotentPayloadMismatchException(idempotencyKey);
            }
            if (lease.hasCachedResponse()) {
                emitMetricSafely(idempotencyMetrics::hit, "hit");
                Object cachedResponse = lease.getExistingResponse().getResponse();
                CachedBusinessFailure cachedFailure = CachedBusinessFailure.decodeIfPresent(cachedResponse);
                if (cachedFailure != null) {
                    // KEEP_FAILED (Story 3.8): the earlier call recorded its business
                    // exception instead of releasing the key, so a retry replays the
                    // same failure instead of re-executing the method. Only the class
                    // name/message survive (see CachedBusinessFailure) — not the
                    // original exception instance or type.
                    log.debug(classAndMethodName + "ended up reading a cached failure for {}", requestObject);
                    throw new IdempotentReplayedFailureException(cachedFailure);
                }
                log.debug(classAndMethodName + "ended up reading from cache for {}", requestObject);
                return cachedResponse;
            }
            emitMetricSafely(idempotencyMetrics::inProgress, "inProgress");
            log.debug(classAndMethodName + "already in progress for {}", requestObject);
            throw new IdempotentInProgressException(idempotencyKey);
        }

        emitMetricSafely(idempotencyMetrics::acquired, "acquired");
        log.debug(classAndMethodName + "saved to cache with {}", idempotencyKey);
        setJdempotentId(pjp.getArgs(),idempotencyKey.getKeyValue());
        IdempotentFailurePolicy failurePolicy = resourceAnnotation.onBusinessException();
        Object result;
        try {
            result = pjp.proceed();
        } catch (Exception e) {
            if (failurePolicy == IdempotentFailurePolicy.KEEP_FAILED) {
                // Story 3.8: record the exception as the cached result instead of
                // releasing the key, so a retry with the same key replays this same
                // failure instead of re-executing the method (avoids duplicating a
                // side effect that already ran before the exception was thrown).
                // The exception itself is not cached as-is, nor as a nested POJO:
                // IdempotentResponseWrapper#response is Object-typed, and
                // PolymorphicRedisSerializer only preserves the concrete type at the
                // root of what it serializes — any custom POJO nested under an
                // Object-typed field comes back from a real Redis round trip as a
                // generic LinkedHashMap, not its original type (pre-existing gap,
                // not specific to this class — see CachedBusinessFailure Javadoc).
                // Encoding the failure as a String sidesteps that entirely.
                log.debug(classAndMethodName + "kept as failed in cache with {} . Exception : {}", idempotencyKey, e);
                String encodedFailure = CachedBusinessFailure.of(e).encode();
                idempotentRepository.setResponse(idempotencyKey, requestObject, new IdempotentResponseWrapper(encodedFailure), customTtl, timeUnit);
                throw e;
            }
            log.debug(classAndMethodName + "deleted from cache with {} . Exception : {}", idempotencyKey, e);
            idempotentRepository.remove(idempotencyKey);
            throw e;
        }

        if (errorCallback != null && errorCallback.onErrorCondition(result)) {
            idempotentRepository.remove(idempotencyKey);
            throw errorCallback.onErrorCustomException();
        }


        idempotentRepository.setResponse(idempotencyKey, requestObject, new IdempotentResponseWrapper(result), customTtl, timeUnit);

        log.debug(classAndMethodName + "ended for {}", requestObject);
        return result;
    }

    /**
     * Generates log prefix for the incoming event
     *
     * @param pjp
     * @return
     */
    private String generateLogPrefixForIncomingEvent(ProceedingJoinPoint pjp) {
        StringBuilder builder = stringBuilders.get();
        String className = pjp.getTarget().getClass().getSimpleName();
        String methodName = pjp.getSignature().getName();
        builder.append(className);
        builder.append(".");
        builder.append(methodName);
        builder.append("() ");
        return builder.toString();
    }

    /**
     * Finds the idempotent object
     *
     * @param pjp
     * @return
     */
    public IdempotentRequestWrapper findIdempotentRequestArg(ProceedingJoinPoint pjp) throws IllegalAccessException {
        Object[] args = pjp.getArgs();
        if (args.length == 0) {
            throw new IllegalStateException("Idempotent method not found");
        } else if (args.length == 1) {
            return new IdempotentRequestWrapper(getIdempotentNonIgnorableWrapper(Collections.singletonList(args[0])));
        } else {
            try {
                MethodSignature signature = (MethodSignature) pjp.getSignature();
                String methodName = signature.getMethod().getName();
                Class<?>[] parameterTypes = signature.getMethod().getParameterTypes();
                var method = pjp.getTarget().getClass().getMethod(methodName, parameterTypes);
                Annotation[][] annotations = method.getParameterAnnotations();
                List<Object> payloads = new ArrayList<>();
                for (int i = 0; i < args.length; i++) {
                    for (Annotation annotation : annotations[i]) {
                        if (annotation instanceof JdempotentRequestPayload) {
                            payloads.add(args[i]);
                        }
                    }
                }
                if(!payloads.isEmpty()) {
                    return new IdempotentRequestWrapper(getIdempotentNonIgnorableWrapper(payloads));
                }
            } catch (NoSuchMethodException | SecurityException e) {
                throw new IllegalStateException("Idempotent method not found", e);
            }
        }
        throw new IllegalStateException("Idempotent method not found");
    }

    /**
     * That function validate and set generated idempotency identifier into annotated field.
     *
     * @param args
     * @param idempotencyKey
     * @throws IllegalAccessException
     */
    public void setJdempotentId(Object[] args, String idempotencyKey) throws IllegalAccessException {
        for (Object arg: args) {
            if (!isTypePrimitive(arg)) {
                for (Field declaredField : arg.getClass().getDeclaredFields()) {
                    declaredField.setAccessible(true);
                    for (Annotation annotation : declaredField.getDeclaredAnnotations()) {
                        if (annotation instanceof JdempotentId) {
                            declaredField.set(arg, idempotencyKey);
                        }
                    }
                }
            }
        }
    }

    public IdempotentIgnorableWrapper getIdempotentNonIgnorableWrapper(List<Object> args) throws IllegalAccessException {
        var wrapper = new IdempotentIgnorableWrapper();
        for (Object arg: args) {
            if(isTypePrimitive(arg)){
                wrapper.getNonIgnoredFields().put(arg.toString(), arg);
            } else {
                for (Field declaredField : getAllFieldsInHierarchy(arg.getClass())) {
                    try {
                        declaredField.setAccessible(true);
                    } catch (InaccessibleObjectException e) {
                        log.debug("Skipping field {} of {}: not accessible", declaredField.getName(), arg.getClass(), e);
                        continue;
                    }
                    KeyValuePair keyValuePair = annotationChain.process(new ChainData(declaredField, arg));
                    if (!StringUtils.isBlank(keyValuePair.getKey())) {
                        wrapper.getNonIgnoredFields().put(keyValuePair.getKey(), keyValuePair.getValue());
                    }
                }
            }
        }
        return wrapper;
    }

    /**
     * Collects every non-static declared field from the given class up through its superclass
     * chain (stopping before {@link Object}), so inherited fields also compose the idempotency
     * key. When a subclass field shadows a superclass field (same name), only the subclass one
     * is kept, matching normal Java field-shadowing semantics. Iteration order here is not a
     * guarantee callers can rely on: the result is folded, keyed by field/property name, into a
     * {@code TreeMap} downstream (see {@link IdempotentIgnorableWrapper}), which is what
     * actually makes the composed key deterministic (Story 3.12), not this method's order.
     *
     * @param clazz the concrete class of the argument
     * @return non-static fields, without name duplicates
     */
    private List<Field> getAllFieldsInHierarchy(Class<?> clazz) {
        List<Field> fields = new ArrayList<>();
        Set<String> seenFieldNames = new HashSet<>();
        for (Class<?> current = clazz; current != null && current != Object.class; current = current.getSuperclass()) {
            for (Field field : current.getDeclaredFields()) {
                if (Modifier.isStatic(field.getModifiers())) {
                    continue;
                }
                if (seenFieldNames.add(field.getName())) {
                    fields.add(field);
                }
            }
        }
        return fields;
    }

    private AnnotationChain fillChains(){
        JdempotentNoAnnotationChain jdempotentNoAnnotationChain = new JdempotentNoAnnotationChain();
        JdempotentIgnoreAnnotationChain jdempotentIgnoreAnnotationChain = new JdempotentIgnoreAnnotationChain();
        // Story 3.12 (AC #1): checked right after JdempotentIgnore and before JdempotentProperty,
        // so a field carrying @JdempotentId is excluded from key composition even if it also
        // carries @JdempotentProperty.
        JdempotentIdAnnotationChain jdempotentIdAnnotationChain = new JdempotentIdAnnotationChain();
        JdempotentDefaultChain jdempotentDefaultChain = new JdempotentDefaultChain();
        JdempotentPropertyAnnotationChain jdempotentPropertyAnnotationChain = new JdempotentPropertyAnnotationChain();

        jdempotentNoAnnotationChain.next(jdempotentIgnoreAnnotationChain);
        jdempotentIgnoreAnnotationChain.next(jdempotentIdAnnotationChain);
        jdempotentIdAnnotationChain.next(jdempotentPropertyAnnotationChain);
        jdempotentPropertyAnnotationChain.next(jdempotentDefaultChain);
        return jdempotentIgnoreAnnotationChain;
    }

    private boolean isTypePrimitive(Object arg){
        if(arg instanceof CharSequence) return true;
        if(arg instanceof Boolean) return true;
        return arg instanceof Number;
    }

    /**
     * Story 3.11 (review finding #1): {@code IdempotencyMetrics} is a public interface a
     * consumer can implement and inject — a broken custom implementation must never fail
     * (or otherwise alter) the idempotency business flow. Every emission call goes through
     * here so a thrown exception is logged and swallowed, never propagated.
     */
    private void emitMetricSafely(Runnable metricCall, String eventName) {
        try {
            metricCall.run();
        } catch (Exception e) {
            log.warn("IdempotencyMetrics.{}() threw — ignoring, must never affect the idempotency business flow", eventName, e);
        }
    }
}
