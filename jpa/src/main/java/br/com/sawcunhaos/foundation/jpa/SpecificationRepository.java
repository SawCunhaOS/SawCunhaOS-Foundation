
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

package br.com.sawcunhaos.foundation.jpa;

import br.com.sawcunhaos.foundation.core.enums.SpecificationFunction;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Root;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * Fábrica estática de {@link Specification} do Spring Data JPA para consultas dinâmicas por nome
 * de atributo. Cada método devolve uma {@code Specification<T>} que o repositório da aplicação
 * (um {@code JpaSpecificationExecutor<T>}) executa, e que pode ser composta com
 * {@link Specification#and(Specification)} / {@link Specification#or(Specification)}.
 *
 * <p>Contrato comum:
 * <ul>
 *   <li>{@code field} é o nome do atributo na <b>entidade</b> (não da coluna); um nome inexistente
 *       só falha na execução da consulta, não na construção da {@code Specification}.</li>
 *   <li>Nenhum método valida argumentos {@code null} na construção; o efeito de {@code null}
 *       está descrito em cada método.</li>
 *   <li>Classe utilitária sem estado: use apenas os métodos estáticos.</li>
 * </ul>
 *
 * @since 1.2.0
 */
public class SpecificationRepository {

    /**
     * Intervalo fechado {@code startDate <= field <= endDate}.
     *
     * @param field     atributo da entidade, do tipo {@link LocalDate}
     * @param startDate limite inferior (inclusive)
     * @param endDate   limite superior (inclusive)
     * @param <T>       tipo da entidade
     * @return a especificação {@code BETWEEN}
     */
    public static <T> Specification<T> specificationBetween(final String field, final LocalDate startDate, final LocalDate endDate) {
        return (root, query, criteriaBuilder) -> criteriaBuilder.between(root.get(field), startDate, endDate);
    }

    /**
     * Filtra {@code field >= date}.
     *
     * @param field atributo da entidade, do tipo {@link LocalDate}
     * @param date  limite inferior (inclusive)
     * @param <T>   tipo da entidade
     * @return a especificação {@code >=}
     */
    public static <T> Specification<T> specificationGreaterThanOrEqualTo(final String field, final LocalDate date) {
        return (root, query, criteriaBuilder) -> criteriaBuilder.greaterThanOrEqualTo(root.get(field), date);
    }

    /**
     * Filtra {@code field <= date}.
     *
     * @param field atributo da entidade, do tipo {@link LocalDate}
     * @param date  limite superior (inclusive)
     * @param <T>   tipo da entidade
     * @return a especificação {@code <=}
     */
    public static <T> Specification<T> specificationLessThanOrEqualTo(final String field, final LocalDate date) {
        return (root, query, criteriaBuilder) -> criteriaBuilder.lessThanOrEqualTo(root.get(field), date);
    }

    /**
     * Filtra {@code field = value}.
     *
     * @param field atributo da entidade
     * @param value valor comparado; {@code null} gera {@code field = NULL} (nunca casa), não
     *              {@code IS NULL}
     * @param <T>   tipo da entidade
     * @param <O>   tipo do valor
     * @return a especificação de igualdade
     */
    public static <T, O> Specification<T> specificationEqual(final String field, final O value) {
        return (root, query, criteriaBuilder) -> criteriaBuilder.equal(root.get(field), value);
    }

    /**
     * Filtra {@code FUNÇÃO_SQL(field) = value}, p.ex. todos os registros de um mês/ano de uma data.
     * Sem {@link SpecificationFunction#getParam()} gera {@code DAY(field)}; com parâmetro gera
     * {@code DATE_PART('DAY', field)}. A função precisa existir no SQL do banco em uso (as de
     * parâmetro, como {@code DATE_PART}, são PostgreSQL).
     *
     * @param field    atributo da entidade, do tipo data
     * @param function função SQL a aplicar sobre o atributo
     * @param value    valor esperado; <b>não pode ser {@code null}</b>: seu {@code getClass()}
     *                 define o tipo de retorno da função (lança {@link NullPointerException} ao
     *                 executar a consulta)
     * @param <T>      tipo da entidade
     * @param <C>      tipo do valor (ex.: {@link Integer})
     * @return a especificação de igualdade sobre a função
     */
    public static <T, C> Specification<T> specificationEqual(
            final String field,
            final SpecificationFunction function,
            final C value
    ) {
        return (root, query, criteriaBuilder) -> {
            // Duas formas de chamada SQL: FUNC(campo) ou FUNC('param', campo) -- ver SpecificationFunction
            if(Objects.isNull(function.getParam())) {
                return criteriaBuilder.equal(criteriaBuilder.function(function.getFunction(), value.getClass(), root.get(field)), value);
            } else {
                return criteriaBuilder.equal(criteriaBuilder.function(function.getFunction(), value.getClass(), criteriaBuilder.literal(function.getParam()), root.get(field)), value);
            }
        };
    }

    /**
     * Filtra por igualdade num atributo aninhado, navegando por associações/embutidos:
     * {@code specificationEqual(1L, "cliente", "id")} equivale a {@code cliente.id = 1}.
     *
     * @param value  valor comparado
     * @param fields caminho de atributos, do primeiro nível ao final; <b>ao menos um</b>
     *               (vazio lança {@link IndexOutOfBoundsException} ao executar a consulta)
     * @param <Y>    tipo do atributo final
     * @param <T>    tipo da entidade raiz
     * @param <O>    tipo do valor
     * @return a especificação de igualdade sobre o caminho
     */
    public static <Y, T, O> Specification<T> specificationEqual(final O value, final String... fields) {
        return (root, query, criteriaBuilder) -> {
            Path<Y> path = getField(root, Arrays.stream(fields).toList());
            return criteriaBuilder.equal(path, value);
        };
    }

    // Primeiro nível sai do Root; os seguintes encadeiam path.get(...) (joins implícitos do JPA).
    private static <Y, T> Path<Y> getField(Root<T> root, List<String> fields) {
        Path<Y> path = root.get(fields.get(0));

        for(int index = 1; index < fields.size(); index++) {
            path = getFieldPath(path, fields.get(index));
        }

        return path;
    }

    private static <Y> Path<Y> getFieldPath(Path<Y> path, String field) {
        return path.get(field);
    }
}
