
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

package br.com.sawcunhaos.foundation.jdempotent.redis.test.app.service;

import br.com.sawcunhaos.foundation.jdempotent.redis.test.app.model.PrimeNumberResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.LongStream;

@Service
@RequiredArgsConstructor
@Slf4j
public class PrimeNumberService {
    public PrimeNumberResponse generatePrimeNumber(long qtd) {

        return PrimeNumberResponse.builder()
                .primesNumber(primeNumbersTill(qtd))
                .build();

    }

    private List<Long> primeNumbersTill(long n) {
        return LongStream.rangeClosed(2, n)
                .filter(this::isPrime).boxed()
                .collect(Collectors.toList());
    }
    private boolean isPrime(long number) {
        return LongStream.rangeClosed(2, (long) (Math.sqrt(number)))
                .allMatch(n -> number % n != 0);
    }
}
