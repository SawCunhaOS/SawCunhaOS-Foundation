
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

/**
 * Value object para um CPF brasileiro (Cadastro de Pessoas Físicas).
 *
 * <p>Contrato fail-fast: o construtor público e o setter validam a entrada e lançam {@link
 * ScosException} com {@code ScosExceptionCode.CPF_INVALID} ({@code SCOS-007}) quando ela é
 * rejeitada, de modo que uma instância nunca guarda um valor inválido. Somente os 11 dígitos sem formatação são
 * aceitos; entrada com pontuação, como {@code 111.444.777-35}, letras, brancos, sequências de dígitos repetidos
 * ({@code 00000000000}) e um CNPJ são rejeitados. O algoritmo do dígito verificador é delegado
 * ao {@code CPFValidator} do {@code caelum-stella}. Um argumento {@code null} não é uma falha de validação:
 * ele quebra o contrato de {@code @NonNull} e falha com uma exceção não verificada que
 * não é uma {@code ScosException}. A classe <em>não</em> é imutável: o setter substitui o valor
 * (após revalidá-lo), e {@code getType()} é {@code "CPF"} assim que um valor é aceito.
 * Anotada com {@code @Embeddable} para JPA ({@code jakarta.persistence-api} é {@code provided} neste
 * módulo, então só consumidores que a persistem precisam de JPA no classpath).
 */
@Embeddable
@Getter
public class Cpf {

    private String cpf;
    @Transient
    private String type;

    /** Exigido pelo JPA; não é para código de aplicação, pois deixa o valor não definido (sem validação). */
    protected Cpf(){}

    /**
     * Cria o value object, validando a entrada.
     *
     * @param cpf o CPF de 11 dígitos
     * @throws ScosException com {@code ScosExceptionCode.CPF_INVALID} se o valor não for válido
     */
    @SuppressFBWarnings(value = "CT_CONSTRUCTOR_THROW",
            justification = "JPA @Embeddable cannot be final; the class declares no finalizer and holds no sensitive state, so the finalizer-attack vector does not apply. Fail-fast validation is intentional.")
    public Cpf(@NonNull String cpf) {
        validate(cpf);
        this.cpf = cpf;
    }

    /**
     * Substitui o valor, aplicando a mesma validação do construtor; em caso de falha, o
     * valor anterior é mantido.
     * Apesar do nome, este setter pertence a {@code Cpf} (o nome é um resquício de
     * copiar e colar mantido por compatibilidade de código-fonte; não é um setter de {@code TaxIdentifier}).
     *
     * @param cpf o novo CPF de 11 dígitos
     * @throws ScosException com {@code ScosExceptionCode.CPF_INVALID} se o valor não for válido
     */
    public void setTaxIdentifier(@NonNull String cpf) {
        validate(cpf);
        this.cpf = cpf;
    }

    private void validate(@NonNull String cpf) {
        final CPFValidator cpfValidator = new CPFValidator();

        // As regras do dígito verificador (dois dígitos módulo 11, sequências de dígitos repetidos rejeitadas) ficam no
        // caelum-stella; `type` só é definido em caso de sucesso, então uma chamada falha do setter mantém o estado antigo.
        if (cpfValidator.invalidMessagesFor(cpf).isEmpty()) {
            type = "CPF";
            return;
        }

        throw new ScosException(CPF_INVALID);
    }
}
