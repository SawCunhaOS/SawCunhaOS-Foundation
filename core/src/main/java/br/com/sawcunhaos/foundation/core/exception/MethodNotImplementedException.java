
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

package br.com.sawcunhaos.foundation.core.exception;

// Story 2.8: moved here from `exception`, same reasoning as ScosNoContentException.
/**
 * Signals that a route or feature is recognized but intentionally not yet
 * implemented. Consumers in {@code web} map this to HTTP 501.
 *
 * @since 1.2.0
 */
public class MethodNotImplementedException extends RuntimeException {

    /** Creates the exception with a fixed default message ({@code "Method not implemented"}). */
    public MethodNotImplementedException() {
        super("Method not implemented");
    }

    /**
     * Creates the exception with a custom message.
     *
     * @param message description of what is not implemented
     */
    public MethodNotImplementedException(String message) {
        super(message);
    }

    /**
     * Creates the exception with a custom message and an underlying cause.
     *
     * @param message description of what is not implemented
     * @param cause   the underlying cause
     */
    public MethodNotImplementedException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Creates the exception wrapping only an underlying cause.
     *
     * @param cause the underlying cause
     */
    public MethodNotImplementedException(Throwable cause) {
        super(cause);
    }

    /**
     * Full-control constructor mirroring {@link RuntimeException}'s protected constructor,
     * for subclasses that need to tune suppression or stack-trace capture.
     *
     * @param message            description of what is not implemented
     * @param cause              the underlying cause
     * @param enableSuppression  whether suppression is enabled
     * @param writableStackTrace whether the stack trace should be writable
     */
    protected MethodNotImplementedException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }

}
