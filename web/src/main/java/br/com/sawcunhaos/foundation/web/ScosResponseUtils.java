
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

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ScosResponseUtils {

    public static <T> ScosResponseDTO<T> wrapResponse(final T data){
        return ScosResponseDTO.<T>builder()
                .data(data)
                .build();
    }

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
