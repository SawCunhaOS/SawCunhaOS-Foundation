
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

package br.com.sawcunhaos.foundation.web;

import org.springframework.boot.SpringBootConfiguration;

/**
 * Marker configuration for {@code @WebMvcTest} in this module: this is a library,
 * not an application, so there is no {@code @SpringBootApplication} entry point for
 * Boot's test bootstrapper to find by searching packages upward from the test class.
 * An empty {@code @SpringBootConfiguration} in the same package is enough — the
 * test slice itself (via {@code @WebMvcTest}) supplies the relevant autoconfiguration.
 */
@SpringBootConfiguration
class TestBootConfiguration {
}
