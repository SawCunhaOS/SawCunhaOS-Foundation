
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

package br.com.sawcunhaos.foundation.exception.listener.sample;

import br.com.sawcunhaos.foundation.validation.api.CNPJ;
import br.com.sawcunhaos.foundation.validation.api.CPF;
import br.com.sawcunhaos.foundation.validation.api.TaxIdentifier;
import br.com.sawcunhaos.foundation.validation.api.ZipCode;

/**
 * Fixture for {@code ValidationAnnotationCountListenerTest}: one field for each of the 4
 * validation annotations, plus a plain field that should not be counted.
 */
public class SampleValidationUsage {

    @CPF
    private String cpf;

    @CNPJ
    private String cnpj;

    @TaxIdentifier
    private String taxIdentifier;

    @ZipCode
    private String zipCode;

    private String plainField;

}
