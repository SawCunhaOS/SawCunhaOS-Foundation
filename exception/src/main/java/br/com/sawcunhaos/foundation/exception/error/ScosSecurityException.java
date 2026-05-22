
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

package br.com.sawcunhaos.foundation.exception.error;

import br.com.sawcunhaos.foundation.utils.exception.ScosException;
import br.com.sawcunhaos.foundation.utils.specification.ExceptionCode;
import lombok.Getter;
import lombok.ToString;

@Getter
@ToString
public class ScosSecurityException extends ScosException {
	public ScosSecurityException(ExceptionCode code) {
		super(code);
	}
	public ScosSecurityException(ExceptionCode code, Object... args) {
		super(code, args);
	}
}
