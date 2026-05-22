
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
import br.com.sawcunhaos.foundation.jdempotent.core.chain.JdempotentIgnoreAnnotationChain;
import br.com.sawcunhaos.foundation.jdempotent.core.chain.JdempotentNoAnnotationChain;
import br.com.sawcunhaos.foundation.jdempotent.core.chain.JdempotentPropertyAnnotationChain;
import br.com.sawcunhaos.foundation.jdempotent.core.constant.CryptographyAlgorithm;
import br.com.sawcunhaos.foundation.jdempotent.core.datasource.IdempotentRepository;
import br.com.sawcunhaos.foundation.jdempotent.core.datasource.InMemoryIdempotentRepository;
import br.com.sawcunhaos.foundation.jdempotent.core.generator.DefaultKeyGenerator;
import br.com.sawcunhaos.foundation.jdempotent.core.generator.KeyGenerator;
import br.com.sawcunhaos.foundation.jdempotent.core.model.ChainData;
import br.com.sawcunhaos.foundation.jdempotent.core.model.IdempotencyKey;
import br.com.sawcunhaos.foundation.jdempotent.core.model.IdempotentIgnorableWrapper;
import br.com.sawcunhaos.foundation.jdempotent.core.model.IdempotentRequestWrapper;
import br.com.sawcunhaos.foundation.jdempotent.core.model.IdempotentResponseWrapper;
import br.com.sawcunhaos.foundation.jdempotent.core.model.KeyValuePair;
import br.com.sawcunhaos.foundation.utils.annotation.jdempotent.JdempotentId;
import br.com.sawcunhaos.foundation.utils.annotation.jdempotent.JdempotentRequestPayload;
import br.com.sawcunhaos.foundation.utils.annotation.jdempotent.JdempotentResource;
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

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;


/**
 * An aspect that used along with the @IdempotentResource annotation
 */
@Aspect
@Slf4j
public class IdempotentAspect {
    private AnnotationChain annotationChain;
    private KeyGenerator keyGenerator;
    @Setter
    @Getter
    private IdempotentRepository idempotentRepository;
    private ErrorConditionalCallback errorCallback;
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


    private static final ThreadLocal<MessageDigest> messageDigests =
            new ThreadLocal<>() {
                @Override
                protected MessageDigest initialValue() {
                    try {
                        return MessageDigest.getInstance(CryptographyAlgorithm.MD5.value());
                    } catch (NoSuchAlgorithmException e) {
                        log.warn("This algorithm not supported.", e);
                    }
                    return null;
                }

                @Override
                public MessageDigest get() {
                    MessageDigest messageDigest = super.get();
                    messageDigest.reset();
                    return messageDigest;
                }
            };

    public IdempotentAspect() {
        this.idempotentRepository = new InMemoryIdempotentRepository();
        this.keyGenerator = new DefaultKeyGenerator();
        this.annotationChain = fillChains();
    }

    public IdempotentAspect(ErrorConditionalCallback errorCallback) {
        this.errorCallback = errorCallback;
        this.idempotentRepository = new InMemoryIdempotentRepository();
        this.keyGenerator = new DefaultKeyGenerator();
        this.annotationChain = fillChains();
    }

    public IdempotentAspect(IdempotentRepository idempotentRepository) {
        this.idempotentRepository = idempotentRepository;
        this.keyGenerator = new DefaultKeyGenerator();
        this.annotationChain = fillChains();
    }

    public IdempotentAspect(IdempotentRepository idempotentRepository, ErrorConditionalCallback errorCallback) {
        this.idempotentRepository = idempotentRepository;
        this.errorCallback = errorCallback;
        this.keyGenerator = new DefaultKeyGenerator();
        this.annotationChain = fillChains();
    }

    public IdempotentAspect(ErrorConditionalCallback errorCallback, DefaultKeyGenerator keyGenerator) {
        this.errorCallback = errorCallback;
        this.idempotentRepository = new InMemoryIdempotentRepository();
        this.keyGenerator = keyGenerator;
        this.annotationChain = fillChains();
    }

    public IdempotentAspect(IdempotentRepository idempotentRepository, DefaultKeyGenerator keyGenerator) {
        this.idempotentRepository = idempotentRepository;
        this.keyGenerator = keyGenerator;
        this.annotationChain = fillChains();
    }

    public IdempotentAspect(IdempotentRepository idempotentRepository, ErrorConditionalCallback errorCallback, DefaultKeyGenerator keyGenerator) {
        this.idempotentRepository = idempotentRepository;
        this.errorCallback = errorCallback;
        this.keyGenerator = keyGenerator;
        this.annotationChain = fillChains();
    }

    /**
     * An advice to make sure it returns at the same time for all subsequent calls
     *
     * @param pjp
     * @return
     * @throws Throwable
     */
    @Around("@annotation(br.com.sawcunhaos.foundation.utils.annotation.jdempotent.JdempotentResource)")
    public Object execute(ProceedingJoinPoint pjp) throws Throwable {
        String classAndMethodName = generateLogPrefixForIncomingEvent(pjp);
        IdempotentRequestWrapper requestObject = findIdempotentRequestArg(pjp);
        String listenerName = ((MethodSignature) pjp.getSignature()).getMethod().getAnnotation(JdempotentResource.class).cachePrefix();
        IdempotencyKey idempotencyKey = keyGenerator.generateIdempotentKey(requestObject, listenerName, stringBuilders.get(), messageDigests.get());
        Long customTtl = ((MethodSignature) pjp.getSignature()).getMethod().getAnnotation(JdempotentResource.class).ttl();
        TimeUnit timeUnit = ((MethodSignature) pjp.getSignature()).getMethod().getAnnotation(JdempotentResource.class).ttlTimeUnit();

        log.debug(classAndMethodName + "starting for {}", requestObject);

        if (idempotentRepository.contains(idempotencyKey)) {
            Object response = retrieveResponse(idempotencyKey);
            log.debug(classAndMethodName + "ended up reading from cache for {}", requestObject);
            return response;
        }

        log.debug(classAndMethodName + "saved to cache with {}", idempotencyKey);
        setJdempotentId(pjp.getArgs(),idempotencyKey.getKeyValue());
        idempotentRepository.store(idempotencyKey, requestObject, customTtl, timeUnit);
        Object result;
        try {
            result = pjp.proceed();
        } catch (Exception e) {
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
     * Retrieve response from cache
     *
     * @param key
     * @return
     */
    private Object retrieveResponse(IdempotencyKey key) {
        IdempotentResponseWrapper response = idempotentRepository.getResponse(key);
        if (response != null) {
            return response.getResponse();
        }
        return null;
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
            Field[] declaredFields = arg.getClass().getDeclaredFields();
            if(isTypePrimitive(arg)){
                wrapper.getNonIgnoredFields().put(arg.toString(), arg);
            } else {
                for (Field declaredField : declaredFields) {
                    declaredField.setAccessible(true);
                    KeyValuePair keyValuePair = annotationChain.process(new ChainData(declaredField, arg));
                    if (!StringUtils.isBlank(keyValuePair.getKey())) {
                        wrapper.getNonIgnoredFields().put(keyValuePair.getKey(), keyValuePair.getValue());
                    }
                }
            }
        }
        return wrapper;
    }

    private AnnotationChain fillChains(){
        JdempotentNoAnnotationChain jdempotentNoAnnotationChain = new JdempotentNoAnnotationChain();
        JdempotentIgnoreAnnotationChain jdempotentIgnoreAnnotationChain = new JdempotentIgnoreAnnotationChain();
        JdempotentDefaultChain jdempotentDefaultChain = new JdempotentDefaultChain();
        JdempotentPropertyAnnotationChain jdempotentPropertyAnnotationChain = new JdempotentPropertyAnnotationChain();

        jdempotentNoAnnotationChain.next(jdempotentIgnoreAnnotationChain);
        jdempotentIgnoreAnnotationChain.next(jdempotentPropertyAnnotationChain);
        jdempotentPropertyAnnotationChain.next(jdempotentDefaultChain);
        return jdempotentIgnoreAnnotationChain;
    }

    private boolean isTypePrimitive(Object arg){
        if(arg instanceof CharSequence) return true;
        if(arg instanceof Boolean) return true;
        return arg instanceof Number;
    }
}
