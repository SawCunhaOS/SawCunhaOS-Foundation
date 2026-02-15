
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

package br.com.sawcunhaos.foundation.utils.annotation.jdempotent;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.concurrent.TimeUnit;

/**
 * Add to the methods that need to be idempotent.
 *
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface JdempotentResource {

    /**
     * Prefix value to make hash value more collider
     *
     * @return
     */
    String cachePrefix() default "";

    /**
     * To add custom ttl
     *
     * @return
     */
    long ttl() default 0L;

    /**
     * To add custom time unit
     *
     * @return
     */
    TimeUnit ttlTimeUnit() default TimeUnit.HOURS;
}
