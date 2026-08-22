
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

package br.com.sawcunhaos.foundation.jdempotent.redis.test.app.controller;

import br.com.sawcunhaos.foundation.jdempotent.redis.test.app.model.PrimeNumberResponse;
import br.com.sawcunhaos.foundation.jdempotent.redis.test.app.service.PrimeNumberService;
import br.com.sawcunhaos.foundation.jdempotent.api.JdempotentResource;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.concurrent.TimeUnit;

@RestController
@RequiredArgsConstructor
public class PrimeNumbersController {

    private final PrimeNumberService primeNumberService;

    @GetMapping("/prime-number")
    @ResponseStatus(HttpStatus.OK)
    @JdempotentResource(cachePrefix = "PrimeNumber.generatePrimeNumber", ttl = 30, ttlTimeUnit = TimeUnit.SECONDS)
    public PrimeNumberResponse generatePrimeNumber(@RequestParam(required = false, defaultValue = "5", name = "quantityPrimeNumbers") long qtd){
        return primeNumberService.generatePrimeNumber(qtd);
    }
}
