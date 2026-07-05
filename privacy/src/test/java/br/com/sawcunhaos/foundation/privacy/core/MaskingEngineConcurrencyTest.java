
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

package br.com.sawcunhaos.foundation.privacy.core;

import br.com.sawcunhaos.foundation.privacy.config.PrivacyConfig;
import br.com.sawcunhaos.foundation.privacy.config.PrivacyConfigLoader;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MaskingEngineConcurrencyTest {

    @Test
    void parallelMaskingMatchesSequential() throws Exception {
        final MaskingEngine engine;
        try (InputStream in = getClass().getClassLoader().getResourceAsStream("test-masking-basic.yml")) {
            final PrivacyConfig config = PrivacyConfigLoader.parseAndValidate(in);
            engine = MaskingEngine.builder().config(config).build();
        }

        final List<String> inputs = new ArrayList<>();
        for (int i = 0; i < 2000; i++) {
            inputs.add("linha " + i + " cpf 529.982.247-25 e topsecret token");
        }

        // Sequential baseline.
        final List<String> expected = inputs.stream().map(engine::maskText).toList();

        final int threads = Math.max(4, Runtime.getRuntime().availableProcessors());
        final ExecutorService pool = Executors.newFixedThreadPool(threads);
        try {
            final List<Callable<List<String>>> tasks = new ArrayList<>();
            for (int t = 0; t < threads; t++) {
                tasks.add(() -> inputs.stream().map(engine::maskText).toList());
            }
            for (final Future<List<String>> f : pool.invokeAll(tasks)) {
                assertEquals(expected, f.get(), "parallel result must equal sequential");
            }
        } finally {
            pool.shutdownNow();
        }
    }
}
