
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

package br.com.sawcunhaos.foundation.web;

import br.com.sawcunhaos.foundation.web.dto.request.ScosPaginationFilterDTO;
import br.com.sawcunhaos.foundation.web.dto.response.ScosPaginatedDTO;
import br.com.sawcunhaos.foundation.core.sort.PropertiesOrder;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.Objects;

/**
 * Converte os parâmetros de paginação do SCOS em {@link Pageable} do Spring Data e monta o
 * {@link ScosPaginatedDTO} de resposta. A API externa conta páginas a partir de <b>1</b>; o
 * {@code PageRequest} do Spring conta a partir de 0, por isso toda página recebida é decrementada em 1.
 *
 * <p>Não há validação: {@code page < 1} faz o {@code PageRequest} lançar
 * {@link IllegalArgumentException}.</p>
 *
 * @since 1.2.0
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class PaginationUtils {

    /**
     * Monta o DTO de paginação da resposta (apenas copia os valores).
     *
     * @return DTO de paginação
     */
    public static ScosPaginatedDTO createPaginated(
            final int totalPages,
            final long totalElements,
            final long totalElementsPerPage,
            final int sizePerPage){
        return ScosPaginatedDTO.builder()
                .totalPages(totalPages)
                .totalElements(totalElements)
                .totalElementsPerPage(totalElementsPerPage)
                .sizePerPage(sizePerPage)
                .build();
    }

    // API 1-based -> Spring 0-based.
    private static int calculatePage(final int page){
        return page - 1;
    }

    /**
     * Cria o {@link Pageable} a partir do filtro (que já aplica os padrões página 1, 10 por página,
     * {@code ASC}). O campo de ordenação passa por {@code orderDefault.value(order)}; se o resultado
     * for {@code null}, a consulta fica sem ordenação.
     *
     * @param scosPaginationFilterDTO filtro vindo do cliente
     * @param orderDefault            mapeia/valida o nome da ordenação (módulo {@code core})
     * @return paginação, com {@link Sort} quando houver campo de ordenação
     */
    public static Pageable createPageable(
            final ScosPaginationFilterDTO scosPaginationFilterDTO,
            final PropertiesOrder orderDefault
    ) {
        String order = orderDefault.value(scosPaginationFilterDTO.order());
        Sort sort = createSort(scosPaginationFilterDTO.getDirection(), order);
        return Objects.nonNull(sort) ?
                PageRequest.of(
                    calculatePage(scosPaginationFilterDTO.getPage()),
                    scosPaginationFilterDTO.getSizePerPage(), sort
                ) :
                PageRequest.of(
                        calculatePage(scosPaginationFilterDTO.getPage()),
                        scosPaginationFilterDTO.getSizePerPage()
                );
    }

    /**
     * Variante com parâmetros soltos; mesma regra de ordenação da variante com filtro.
     *
     * @param page         página, a partir de 1
     * @param sizePerPage  itens por página
     * @param direction    sentido da ordenação
     * @param order        nome da ordenação solicitada (passa por {@code orderDefault})
     * @param orderDefault mapeia/valida o nome da ordenação
     * @return paginação, com {@link Sort} quando houver campo de ordenação
     */
    public static Pageable createPageable(
            final int page,
            final int sizePerPage,
            final Sort.Direction direction,
            final String order,
            final PropertiesOrder orderDefault
    ) {
        String orderPage = orderDefault.value(order);
        Sort sort = createSort(direction, orderPage);
        return Objects.nonNull(sort) ?
                PageRequest.of( calculatePage(page), sizePerPage, sort) :
                PageRequest.of( calculatePage(page), sizePerPage);
    }

    /**
     * Variante simplificada: ordena sempre pela propriedade {@code id}.
     *
     * @param page        página, a partir de 1
     * @param sizePerPage itens por página
     * @param direction   {@code "asc"} ou {@code "desc"} (sem diferenciar maiúsculas); outro valor lança
     *                    {@link IllegalArgumentException}
     * @return paginação ordenada por {@code id}
     */
    public static Pageable createPageable(
            final int page,
            final int sizePerPage,
            final String direction
    ) {
        Sort.Direction directionSort = Sort.Direction.fromString(direction);

        Sort sort = createSort(directionSort, "id");
        return Objects.nonNull(sort) ?
                PageRequest.of( calculatePage(page), sizePerPage, sort) :
                PageRequest.of( calculatePage(page), sizePerPage);
    }

    private static Sort createSort(
            final Sort.Direction direction,
            final String order
    ){

        return Objects.nonNull(order) ?
                Sort.by(
                    direction,
                    order
                ) : null;
    }

}
