
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

import br.com.sawcunhaos.foundation.utils.exception.ScosException;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import jakarta.persistence.Embeddable;
import lombok.Getter;
import org.jspecify.annotations.NonNull;

import java.util.regex.Pattern;

import static br.com.sawcunhaos.foundation.utils.enums.ScosExceptionCode.EMAIL_INVALID;

@Embeddable
@Getter
public class Email {

    private String email;

    protected Email() {}

    @SuppressFBWarnings(value = "CT_CONSTRUCTOR_THROW",
            justification = "JPA @Embeddable cannot be final; the class declares no finalizer and holds no sensitive state, so the finalizer-attack vector does not apply. Fail-fast validation is intentional.")
    public Email(@NonNull String email) {
        validate(email);
        this.email = email;
    }

    public void setEmail(@NonNull String email) {
        validate(email);
        this.email = email;
    }

    private void validate(@NonNull String email) {
        final String emailRegex = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$";
        final Pattern emailPattern = Pattern.compile(emailRegex);

        if (email.contains("..") || email.contains(".@") || email.startsWith(".") || !emailPattern.matcher(email).matches()) {
            throw new ScosException(EMAIL_INVALID);
        }
    }
}

