
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
import br.com.sawcunhaos.foundation.utils.exception.ScosException;
import com.querydsl.core.annotations.QueryEmbeddable;
import jakarta.persistence.Embeddable;
import lombok.Getter;
import org.jspecify.annotations.NonNull;

import static br.com.sawcunhaos.foundation.utils.enums.ScosExceptionCode.CNPJ_INVALID;

@Embeddable
@Getter
@QueryEmbeddable
public class Cnpj {

    private String cnpj;
    private String type;

    protected Cnpj(){}
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
