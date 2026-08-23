
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

import lombok.NoArgsConstructor;

// Story 2.8: moved here from `exception` alongside ScosException/ExceptionCode/ScosExceptionCode
// so the domain exception hierarchy has no Spring dependency (Story 2.8's own AC #1).
@NoArgsConstructor
public class ScosNoContentException extends ScosException {

}
