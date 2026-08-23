
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

package br.com.sawcunhaos.foundation.web.annotation;

import org.junit.jupiter.api.Test;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.core.annotation.AnnotatedElementUtils;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * AC #1 / Task 2: after moving the {@code ScosRequestGET/POST/PUT/DELETE/PATCH} annotations from
 * {@code utils.annotation.request} to {@code web.annotation}, Spring's {@code @AliasFor} merging
 * must still resolve {@code @Cacheable}/{@code @CacheEvict} correctly — package moves don't affect
 * annotation retention, but this is the one behavioral claim the story's AC makes explicitly, so it
 * gets a real check rather than trusting the move by inspection alone.
 */
class ScosRequestAnnotationCachingTest {

    @Test
    void scosRequestGetResolvesCacheableWithDefaults() throws NoSuchMethodException {
        Method method = Sample.class.getMethod("get");
        Cacheable cacheable = AnnotatedElementUtils.findMergedAnnotation(method, Cacheable.class);

        assertNotNull(cacheable, "@ScosRequestGET must resolve a merged @Cacheable");
        assertArrayEquals(new String[]{"DISABLE"}, cacheable.value());
        assertEquals("ScosCacheKeyGenerator", cacheable.keyGenerator());
        assertEquals("false", cacheable.condition());
    }

    @Test
    void scosRequestGetResolvesCacheableWithCustomAttributes() throws NoSuchMethodException {
        Method method = Sample.class.getMethod("getCustom");
        Cacheable cacheable = AnnotatedElementUtils.findMergedAnnotation(method, Cacheable.class);

        assertNotNull(cacheable);
        assertArrayEquals(new String[]{"pessoas"}, cacheable.value());
        assertEquals("true", cacheable.condition());
    }

    @Test
    void scosRequestPostResolvesCacheEvict() throws NoSuchMethodException {
        Method method = Sample.class.getMethod("post");
        CacheEvict cacheEvict = AnnotatedElementUtils.findMergedAnnotation(method, CacheEvict.class);

        assertNotNull(cacheEvict, "@ScosRequestPOST must resolve a merged @CacheEvict");
        assertTrue(cacheEvict.allEntries());
    }

    @Test
    void scosRequestDeleteResolvesCacheEvict() throws NoSuchMethodException {
        Method method = Sample.class.getMethod("delete");
        CacheEvict cacheEvict = AnnotatedElementUtils.findMergedAnnotation(method, CacheEvict.class);

        assertNotNull(cacheEvict, "@ScosRequestDELETE must resolve a merged @CacheEvict");
        assertTrue(cacheEvict.allEntries());
    }

    @Test
    void scosRequestPutResolvesCacheEvict() throws NoSuchMethodException {
        Method method = Sample.class.getMethod("put");
        CacheEvict cacheEvict = AnnotatedElementUtils.findMergedAnnotation(method, CacheEvict.class);

        assertNotNull(cacheEvict, "@ScosRequestPUT must resolve a merged @CacheEvict");
        assertTrue(cacheEvict.allEntries());
    }

    @Test
    void scosRequestPatchResolvesCacheEvict() throws NoSuchMethodException {
        Method method = Sample.class.getMethod("patch");
        CacheEvict cacheEvict = AnnotatedElementUtils.findMergedAnnotation(method, CacheEvict.class);

        assertNotNull(cacheEvict, "@ScosRequestPATCH must resolve a merged @CacheEvict");
        assertTrue(cacheEvict.allEntries());
    }

    private static final class Sample {
        @ScosRequestGET(uri = "/x", httpCode = org.springframework.http.HttpStatus.OK)
        public void get() {
        }

        @ScosRequestGET(uri = "/x", httpCode = org.springframework.http.HttpStatus.OK,
                nameCache = "pessoas", condition = "true")
        public void getCustom() {
        }

        @ScosRequestPOST(uri = "/x", httpCode = org.springframework.http.HttpStatus.CREATED)
        public void post() {
        }

        @ScosRequestDELETE(uri = "/x", httpCode = org.springframework.http.HttpStatus.NO_CONTENT)
        public void delete() {
        }

        @ScosRequestPUT(uri = "/x", httpCode = org.springframework.http.HttpStatus.OK)
        public void put() {
        }

        @ScosRequestPATCH(uri = "/x", httpCode = org.springframework.http.HttpStatus.OK)
        public void patch() {
        }
    }
}
