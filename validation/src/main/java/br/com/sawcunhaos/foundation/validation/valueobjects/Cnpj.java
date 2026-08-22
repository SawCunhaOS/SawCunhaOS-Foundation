
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

package br.com.sawcunhaos.foundation.validation.valueobjects;

import br.com.caelum.stella.validation.CNPJValidator;
import br.com.sawcunhaos.foundation.core.exception.ScosException;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import jakarta.persistence.Embeddable;
import lombok.Getter;
import org.jspecify.annotations.NonNull;
import jakarta.persistence.Transient;

import static br.com.sawcunhaos.foundation.core.enums.ScosExceptionCode.CNPJ_INVALID;

@Embeddable
@Getter
public class Cnpj {

    private String cnpj;
    @Transient
    private String type;

    protected Cnpj(){}

    @SuppressFBWarnings(value = "CT_CONSTRUCTOR_THROW",
            justification = "JPA @Embeddable cannot be final; the class declares no finalizer and holds no sensitive state, so the finalizer-attack vector does not apply. Fail-fast validation is intentional.")
    public Cnpj(@NonNull String cnpj) {
        validate(cnpj);
        this.cnpj = cnpj;
    }

    public void setTaxIdentifier(@NonNull String cnpj) {
        validate(cnpj);
        this.cnpj = cnpj;
    }

    private void validate(@NonNull String cnpj) {
        final CNPJValidator cnpjValidator = new CNPJValidator();

        if (cnpjValidator.invalidMessagesFor(cnpj).isEmpty()) {
            type = "CNPJ";
            return;
        }

        throw new ScosException(CNPJ_INVALID);
    }
}
