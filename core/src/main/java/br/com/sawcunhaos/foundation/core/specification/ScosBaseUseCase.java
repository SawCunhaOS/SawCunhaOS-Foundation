
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

package br.com.sawcunhaos.foundation.core.specification;

/**
 * Contract for a single-input, single-output use case, so application layers
 * can depend on this instead of hand-rolling a service interface per use case.
 * No implementor in this reactor uses it yet.
 *
 * @param <P> the parameter type the use case receives
 * @param <R> the result type the use case returns
 * @since 1.2.0
 */
public interface ScosBaseUseCase<P,R> {

    /**
     * Executes the use case.
     *
     * @param parameter the input to the use case
     * @return the use case's result
     */
    R execute(P parameter);

}
