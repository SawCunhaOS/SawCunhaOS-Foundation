
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

package br.com.sawcunhaos.foundation.jdempotent.core.callback;

/**
 * 
 * A callback interface that need to clear cache for custom error condition
 *
 */
public interface ErrorConditionalCallback {

    /**
     * a error state flag
     *
     * @param response
     * @return
     */
    boolean onErrorCondition(Object response);

    /**
     *
     * exception to throw when custom error occurs
     *
     * @return
     */
    RuntimeException onErrorCustomException();
    
}
