
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

@Getter
@ToString
public class ScosException extends RuntimeException {
    private final String code;
	private final Object[] args;
	private final int httpCode;
	private final String title;

	public ScosException() {
		super();
		this.code = null;
		this.args = null;
		this.httpCode = 400;
		this.title = "ERROR";
	}

	public ScosException(ExceptionCode code) {
		super();
		this.code = code.getCode();
		this.args = null;
		this.httpCode = code.getHttpCode();
		this.title = code.getTitle();
	}

	public ScosException(ExceptionCode code, Object... args) {
		super();
		this.code = code.getCode();
		this.args = args;
		this.httpCode = code.getHttpCode();
		this.title = code.getTitle();
	}
}
