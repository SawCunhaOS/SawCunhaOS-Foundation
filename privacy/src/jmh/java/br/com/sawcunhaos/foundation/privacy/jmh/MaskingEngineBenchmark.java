
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

package br.com.sawcunhaos.foundation.privacy.jmh;

import br.com.sawcunhaos.foundation.privacy.config.PrivacyConfig;
import br.com.sawcunhaos.foundation.privacy.config.PrivacyConfigLoader;
import br.com.sawcunhaos.foundation.privacy.core.MaskingEngine;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;

import java.io.InputStream;
import java.util.concurrent.TimeUnit;

/**
 * JMH benchmarks for the hot path. Runs only under the {@code -Pperf} profile (separate {@code src/jmh}
 * source set) so the normal build stays fast. The performance gate compares results against the versioned
 * baseline in {@code etc/perf/baseline.json}: a regression beyond the agreed threshold fails the merge.
 *
 * <p>Run: {@code mvn -Pperf -pl privacy test-compile} then execute the generated JMH runner, or wire the
 * {@code org.openjdk.jmh} runner in a dedicated CI job.</p>
 */
@State(Scope.Benchmark)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Warmup(iterations = 3, time = 1)
@Measurement(iterations = 5, time = 1)
@Fork(1)
public class MaskingEngineBenchmark {

    private MaskingEngine engine;
    private final String lineWithoutPii = "GET /api/orders 200 in 12ms requestId=abc-123-def";
    private final String lineWithPii = "cliente cpf 529.982.247-25 email ana@example.com criado";
    private final String json = "{\"cpf\":\"52998224725\",\"email\":\"ana@example.com\",\"status\":\"ACTIVE\"}";

    @Setup
    public void setup() throws Exception {
        try (InputStream in = getClass().getClassLoader().getResourceAsStream("privacy-masking.yml")) {
            final PrivacyConfig config = PrivacyConfigLoader.parseAndValidate(in);
            engine = MaskingEngine.builder().config(config).build();
        }
    }

    @Benchmark
    public String maskTextFastPath() {
        return engine.maskText(lineWithoutPii);
    }

    @Benchmark
    public String maskTextWithPii() {
        return engine.maskText(lineWithPii);
    }

    @Benchmark
    public String maskStructured() {
        return engine.maskStructured("cpf", "52998224725");
    }
}
