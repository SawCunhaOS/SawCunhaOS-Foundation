
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

package br.com.sawcunhaos.foundation.utils.exception;

import br.com.sawcunhaos.foundation.utils.specification.ExceptionCode;
import lombok.Getter;
import lombok.ToString;

@Getter
@ToString
public class ScosException extends RuntimeException {
    private final String code;
	private final Object[] args;

	public ScosException() {
		super();
		this.code = null;
		this.args = null;
	}

	public ScosException(ExceptionCode code) {
		super();
		this.code = code.getCode();
		this.args = null;
	}

	public ScosException(ExceptionCode code, Object... args) {
		super();
		this.code = code.getCode();
		this.args = args;
	}
}
