
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

package br.com.sawcunhaos.foundation.utils.utils;

import br.com.sawcunhaos.foundation.utils.dto.request.ScosPaginationFilterDTO;
import br.com.sawcunhaos.foundation.utils.dto.response.ScosPaginatedDTO;
import br.com.sawcunhaos.foundation.utils.sort.PropertiesOrder;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.Objects;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class PaginationUtils {

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

    private static int calculatePage(final int page){
        return page - 1;
    }

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
