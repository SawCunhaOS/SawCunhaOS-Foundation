
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

import br.com.caelum.stella.validation.CPFValidator;
import br.com.sawcunhaos.foundation.utils.exception.ScosException;
import jakarta.persistence.Embeddable;
import lombok.Getter;
import org.jspecify.annotations.NonNull;

import static br.com.sawcunhaos.foundation.utils.enums.ScosExceptionCode.CPF_INVALID;

@Embeddable
@Getter
public class Cpf {

    private String cpf;
    private String type;

    protected Cpf(){}
    public Cpf(@NonNull String cpf) {
        validate(cpf);
        this.cpf = cpf;
    }

    public void setTaxIdentifier(@NonNull String cpf) {
        validate(cpf);
        this.cpf = cpf;
    }

    private void validate(@NonNull String cpf) {
        final CPFValidator cpfValidator = new CPFValidator();

        if (cpfValidator.invalidMessagesFor(cpf).isEmpty()) {
            type = "CPF";
            return;
        }

        throw new ScosException(CPF_INVALID);
    }
}
