
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

// Story 2.8: moved here from `exception`, same reasoning as ScosNoContentException.
/**
 * Marker subclass of {@link ScosException} meant to be named in a caller's
 * {@code @Transactional(noRollbackFor = ScosNoRollbackException.class)} — for
 * business failures that must still return an RFC 9457 error response (same
 * mapping as {@link ScosException}) without rolling back the current
 * transaction. Because {@code core} cannot depend on Spring, only the marker
 * type lives here; the {@code @Transactional} wiring is the caller's own.
 *
 * @since 1.2.0
 */
@Getter
public class ScosNoRollbackException extends ScosException {

	/**
	 * Creates the exception from an {@link ExceptionCode}, delegating to
	 * {@link ScosException#ScosException(ExceptionCode)} to copy its code, HTTP
	 * status and title.
	 *
	 * @param code the error code describing this failure
	 */
	public ScosNoRollbackException(ExceptionCode code) {
		super(code);
	}
}
