
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
import br.com.caelum.stella.validation.CPFValidator;
import br.com.sawcunhaos.foundation.core.exception.ScosException;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Transient;
import lombok.Getter;
import org.jspecify.annotations.NonNull;

import static br.com.sawcunhaos.foundation.core.enums.ScosExceptionCode.TAX_IDENTIFIER_INVALID;

/**
 * Value object para um identificador fiscal brasileiro que pode ser um CPF ou um CNPJ.
 *
 * <p>Contrato fail-fast: o construtor público e o setter validam a entrada e lançam {@link
 * ScosException} com {@code ScosExceptionCode.TAX_IDENTIFIER_INVALID} ({@code SCOS-006}) quando ela
 * é rejeitada, de modo que uma instância nunca guarda um valor inválido. Somente dígitos sem formatação são aceitos:
 * o valor é testado primeiro como CNPJ e depois como CPF, e rejeitado se nenhuma validação de dígito verificador
 * passar (os algoritmos são delegados ao {@code caelum-stella}). Um argumento {@code null}
 * não é uma falha de validação: ele quebra o contrato de {@code @NonNull} e falha com uma
 * exceção não verificada que não é uma {@code ScosException}. A classe <em>não</em> é imutável: o
 * setter revalida e substitui o valor. {@code getType()} informa qual documento o valor se revelou ({@code "CNPJ"} ou {@code
 * "CPF"}); é {@code @Transient} e recalculado a cada validação bem-sucedida, portanto não é
 * persistido. Anotada com {@code @Embeddable} para JPA ({@code jakarta.persistence-api} é {@code
 * provided} neste módulo, então só consumidores que a persistem precisam de JPA no classpath).
 */
@Embeddable
@Getter
public class TaxIdentifier {

    private String taxIdentifier;
    @Transient
    private String type;

    /** Exigido pelo JPA; não é para código de aplicação, pois deixa o valor não definido (sem validação). */
    protected TaxIdentifier(){}

    /**
     * Cria o value object, validando a entrada.
     *
     * @param taxIdentifier o CPF ou CNPJ (somente dígitos)
     * @throws ScosException com {@code ScosExceptionCode.TAX_IDENTIFIER_INVALID} se o valor não for válido
     */
    @SuppressFBWarnings(value = "CT_CONSTRUCTOR_THROW",
            justification = "JPA @Embeddable cannot be final; the class declares no finalizer and holds no sensitive state, so the finalizer-attack vector does not apply. Fail-fast validation is intentional.")
    public TaxIdentifier(@NonNull String taxIdentifier) {
        validate(taxIdentifier);
        this.taxIdentifier = taxIdentifier;
    }

    /**
     * Substitui o valor, aplicando a mesma validação do construtor; em caso de falha, o
     * valor anterior é mantido.
     *
     * @param taxIdentifier o novo CPF ou CNPJ (somente dígitos)
     * @throws ScosException com {@code ScosExceptionCode.TAX_IDENTIFIER_INVALID} se o valor não for válido
     */
    public void setTaxIdentifier(@NonNull String taxIdentifier) {
        validate(taxIdentifier);
        this.taxIdentifier = taxIdentifier;
    }

    private void validate(@NonNull String taxIdentifier) {
        final CNPJValidator cnpjValidator = new CNPJValidator();
        final CPFValidator cpfValidator = new CPFValidator();

        // Primeiro CNPJ, depois CPF: os dois documentos têm tamanhos diferentes (14 vs 11 dígitos), então um
        // valor que passa em uma validação de dígito verificador não passa na outra, e a ordem
        // só decide qual `type` é registrado.
        if (cnpjValidator.invalidMessagesFor(taxIdentifier).isEmpty()) {
            type = "CNPJ";
        } else if (cpfValidator.invalidMessagesFor(taxIdentifier).isEmpty()) {
            type = "CPF";
        } else {
            throw new ScosException(TAX_IDENTIFIER_INVALID);
        }
    }
}
