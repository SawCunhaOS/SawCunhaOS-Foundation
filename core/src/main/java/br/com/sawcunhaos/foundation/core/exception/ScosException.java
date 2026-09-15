
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

/**
 * Base domain exception of the foundation, carrying the {@link ExceptionCode}
 * metadata that {@code web.ExceptionsHandler} needs to build an RFC 9457
 * problem-details response, without depending on Spring itself.
 *
 * @since 1.2.0
 */
@Getter
@ToString
public class ScosException extends RuntimeException {
    /** Stable error code (e.g. {@code "SCOS-001"}), copied from the {@link ExceptionCode} used to build this exception. */
    private final String code;
	/** Interpolation arguments for the message associated with {@link #code}, if any. */
	private final Object[] args;
	/** HTTP status to respond with, copied from {@link ExceptionCode#getHttpCode()}. */
	private final int httpCode;
	/** RFC 9457 {@code title}, copied from {@link ExceptionCode#getTitle()}. */
	private final String title;

	/**
	 * Creates the exception with no {@link ExceptionCode}, defaulting to HTTP 400
	 * and title {@code "ERROR"}.
	 *
	 * <p>Note the title here ({@code "ERROR"}, upper-case) is a separate hard-coded
	 * default from {@link ExceptionCode#getTitle()}'s own default ({@code "Error"});
	 * they are not kept in sync automatically.
	 */
	public ScosException() {
		super();
		this.code = null;
		this.args = null;
		this.httpCode = 400;
		this.title = "ERROR";
	}

	/**
	 * Creates the exception from an {@link ExceptionCode}, copying its code, HTTP
	 * status and title.
	 *
	 * @param code the error code describing this failure
	 */
	public ScosException(ExceptionCode code) {
		super();
		this.code = code.getCode();
		this.args = null;
		this.httpCode = code.getHttpCode();
		this.title = code.getTitle();
	}

	/**
	 * Creates the exception from an {@link ExceptionCode} plus interpolation
	 * arguments for its message.
	 *
	 * @param code the error code describing this failure
	 * @param args interpolation arguments for the resolved message
	 */
	public ScosException(ExceptionCode code, Object... args) {
		super();
		this.code = code.getCode();
		this.args = args;
		this.httpCode = code.getHttpCode();
		this.title = code.getTitle();
	}
}
