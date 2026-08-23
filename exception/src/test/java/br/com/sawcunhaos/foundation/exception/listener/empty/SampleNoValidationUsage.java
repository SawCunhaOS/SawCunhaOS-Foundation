
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

package br.com.sawcunhaos.foundation.exception.listener.empty;

/**
 * Fixture for {@code ValidationAnnotationCountListenerTest}: a plain class with no
 * {@code @CPF}/{@code @CNPJ}/{@code @TaxIdentifier}/{@code @ZipCode} usage, in its own package so
 * scanning it cannot accidentally pick up {@code sample}'s fixture.
 */
public class SampleNoValidationUsage {

    private String plainField;

}
