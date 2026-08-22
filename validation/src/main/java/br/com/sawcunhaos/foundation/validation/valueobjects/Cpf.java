
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

import br.com.caelum.stella.validation.CPFValidator;
import br.com.sawcunhaos.foundation.core.exception.ScosException;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import jakarta.persistence.Embeddable;
import lombok.Getter;
import org.jspecify.annotations.NonNull;
import jakarta.persistence.Transient;

import static br.com.sawcunhaos.foundation.core.enums.ScosExceptionCode.CPF_INVALID;

@Embeddable
@Getter
public class Cpf {

    private String cpf;
    @Transient
    private String type;

    protected Cpf(){}

    @SuppressFBWarnings(value = "CT_CONSTRUCTOR_THROW",
            justification = "JPA @Embeddable cannot be final; the class declares no finalizer and holds no sensitive state, so the finalizer-attack vector does not apply. Fail-fast validation is intentional.")
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
