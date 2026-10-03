
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

import br.com.sawcunhaos.foundation.core.exception.ScosException;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import jakarta.persistence.Embeddable;
import lombok.Getter;
import org.jspecify.annotations.NonNull;

import java.util.regex.Pattern;

import static br.com.sawcunhaos.foundation.core.enums.ScosExceptionCode.EMAIL_INVALID;

/**
 * Value object para um endereço de e-mail.
 *
 * <p>Contrato fail-fast: o construtor público e o setter validam a entrada e lançam {@link
 * ScosException} com {@code ScosExceptionCode.EMAIL_INVALID} ({@code SCOS-009}) quando ela é
 * rejeitada, de modo que uma instância nunca guarda um valor inválido. A validação é uma regex pragmática ({@code
 * local@domain.tld}, TLD com 2+ letras) mais a rejeição explícita de {@code ..}, {@code .@} e de
 * ponto inicial; não é uma verificação completa da RFC 5322. Um argumento {@code null} não é uma falha de validação:
 * ele quebra o contrato de {@code @NonNull} e falha com uma exceção não verificada que
 * não é uma {@code ScosException}. A classe <em>não</em> é imutável: o setter substitui o valor
 * após revalidá-lo. Anotada com {@code @Embeddable} para JPA ({@code jakarta.persistence-api} é
 * {@code provided} neste módulo, então só consumidores que a persistem precisam de JPA no classpath).
 */
@Embeddable
@Getter
public class Email {

    private String email;

    /** Exigido pelo JPA; não é para código de aplicação, pois deixa o valor não definido (sem validação). */
    protected Email() {}

    /**
     * Cria o value object, validando a entrada.
     *
     * @param email o endereço de e-mail
     * @throws ScosException com {@code ScosExceptionCode.EMAIL_INVALID} se o valor não for válido
     */
    @SuppressFBWarnings(value = "CT_CONSTRUCTOR_THROW",
            justification = "JPA @Embeddable cannot be final; the class declares no finalizer and holds no sensitive state, so the finalizer-attack vector does not apply. Fail-fast validation is intentional.")
    public Email(@NonNull String email) {
        validate(email);
        this.email = email;
    }

    /**
     * Substitui o valor, aplicando a mesma validação do construtor; em caso de falha, o
     * valor anterior é mantido.
     *
     * @param email o novo endereço de e-mail
     * @throws ScosException com {@code ScosExceptionCode.EMAIL_INVALID} se o valor não for válido
     */
    public void setEmail(@NonNull String email) {
        validate(email);
        this.email = email;
    }

    private void validate(@NonNull String email) {
        final String emailRegex = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$";
        final Pattern emailPattern = Pattern.compile(emailRegex);

        // A regex sozinha aceita "a..b@x.com", "a.@x.com" e ponto inicial, porque "." está na
        // classe permitida da parte local; as verificações explícitas abaixo fecham essas brechas.
        if (email.contains("..") || email.contains(".@") || email.startsWith(".") || !emailPattern.matcher(email).matches()) {
            throw new ScosException(EMAIL_INVALID);
        }
    }
}
