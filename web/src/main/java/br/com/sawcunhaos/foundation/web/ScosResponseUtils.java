
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

import br.com.sawcunhaos.foundation.web.dto.response.ScosResponseDTO;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * Atalhos para embrulhar o resultado de um endpoint em {@link ScosResponseDTO}.
 *
 * @since 1.2.0
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ScosResponseUtils {

    /**
     * Embrulha {@code data} sem informação de paginação ({@code scosPaginatedDTO} fica {@code null}).
     *
     * @param data conteúdo da resposta (aceita {@code null})
     * @return resposta com apenas {@code data}
     */
    public static <T> ScosResponseDTO<T> wrapResponse(final T data){
        return ScosResponseDTO.<T>builder()
                .data(data)
                .build();
    }

    /**
     * Embrulha {@code data} junto com os metadados de paginação, montados por
     * {@link PaginationUtils#createPaginated(int, long, long, int)}.
     *
     * @param data                  conteúdo da página
     * @param totalPages            total de páginas
     * @param totalElements         total de elementos em todas as páginas
     * @param totalElementsPerPage  quantidade de elementos efetivamente na página atual
     * @param sizePerPage           tamanho de página solicitado
     * @return resposta com {@code data} e {@code scosPaginatedDTO}
     */
    public static <T> ScosResponseDTO<T> wrapResponse(
            final T data,
            final int totalPages,
            final long totalElements,
            final long totalElementsPerPage,
            final int sizePerPage
    ){
        return ScosResponseDTO.<T>builder()
                .data(data)
                .scosPaginatedDTO(PaginationUtils.createPaginated(totalPages, totalElements, totalElementsPerPage, sizePerPage))
                .build();
    }

}
