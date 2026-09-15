
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

import br.com.sawcunhaos.foundation.core.specification.ExceptionCode;
import lombok.Getter;
import lombok.ToString;

// Story 2.8: moved here from `exception`, same reasoning as ScosNoContentException.
/**
 * Marker subclass of {@link ScosException}, intended for security/authorization
 * failures (e.g. access denied, missing token) so callers can catch or route
 * security errors distinctly from generic {@link ScosException}s, while keeping
 * the same RFC 9457 mapping in {@code web.ExceptionsHandler}. No caller in this
 * reactor throws it yet — access-denied/unauthorized cases are currently handled
 * via Spring's own {@code AccessDeniedException}/{@code AuthorizationDeniedException}.
 *
 * @since 1.2.0
 */
@Getter
@ToString
public class ScosSecurityException extends ScosException {
	/**
	 * Creates the exception from an {@link ExceptionCode}, delegating to
	 * {@link ScosException#ScosException(ExceptionCode)} to copy its code, HTTP
	 * status and title.
	 *
	 * @param code the error code describing this failure
	 */
	public ScosSecurityException(ExceptionCode code) {
		super(code);
	}

	/**
	 * Creates the exception from an {@link ExceptionCode} plus interpolation
	 * arguments for its message, delegating to
	 * {@link ScosException#ScosException(ExceptionCode, Object...)}.
	 *
	 * @param code the error code describing this failure
	 * @param args interpolation arguments for the resolved message
	 */
	public ScosSecurityException(ExceptionCode code, Object... args) {
		super(code, args);
	}
}
