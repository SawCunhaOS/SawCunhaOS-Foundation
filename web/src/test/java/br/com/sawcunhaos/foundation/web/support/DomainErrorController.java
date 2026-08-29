
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

package br.com.sawcunhaos.foundation.web.support;

import br.com.sawcunhaos.foundation.core.exception.MethodNotImplementedException;
import jakarta.validation.constraints.Min;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Fixture controller for {@code @WebMvcTest}: each route throws one of the domain
 * exceptions {@link br.com.sawcunhaos.foundation.web.ExceptionsHandler} maps to a
 * fixed status (403/400/501/500), so those paths can be exercised through real MVC
 * dispatch — content negotiation, {@code @ControllerAdvice} resolution and Jackson 3
 * serialization included — instead of only via direct, mocked method calls.
 */
@RestController
public class DomainErrorController {

	@GetMapping("/forbidden")
	public String forbidden() {
		throw new AccessDeniedException("denied");
	}

	@GetMapping("/legacy")
	public String legacy() {
		throw new MethodNotImplementedException();
	}

	@GetMapping("/boom")
	public String boom() {
		throw new IllegalStateException("kaboom");
	}

	@GetMapping("/paged")
	public String paged(@RequestParam(name = "page") @Min(1) int page) {
		return "page-" + page;
	}
}
