
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

package br.com.sawcunhaos.foundation.utils.valueobjects;

import br.com.caelum.stella.validation.CNPJValidator;
import br.com.caelum.stella.validation.CPFValidator;
import br.com.sawcunhaos.foundation.utils.exception.ScosException;
import jakarta.persistence.Embeddable;
import lombok.Getter;
import org.jspecify.annotations.NonNull;

import static br.com.sawcunhaos.foundation.utils.enums.ScosExceptionCode.TAX_IDENTIFIER_INVALID;

@Embeddable
@Getter
public class TaxIdentifier {

    private String taxIdentifier;
    private String type;

    protected TaxIdentifier(){}
    public TaxIdentifier(@NonNull String taxIdentifier) {
        validate(taxIdentifier);
        this.taxIdentifier = taxIdentifier;
    }

    public void setTaxIdentifier(@NonNull String taxIdentifier) {
        validate(taxIdentifier);
        this.taxIdentifier = taxIdentifier;
    }

    private void validate(@NonNull String taxIdentifier) {
        final CNPJValidator cnpjValidator = new CNPJValidator();
        final CPFValidator cpfValidator = new CPFValidator();

        if (cnpjValidator.invalidMessagesFor(taxIdentifier).isEmpty()) {
            type = "CNPJ";
        } else if (cpfValidator.invalidMessagesFor(taxIdentifier).isEmpty()) {
            type = "CPF";
        } else {
            throw new ScosException(TAX_IDENTIFIER_INVALID);
        }
    }
}
