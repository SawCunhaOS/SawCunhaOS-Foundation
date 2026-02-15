
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

package br.com.sawcunhaos.foundation.utils.dto.request;

import lombok.Builder;
import org.springframework.data.domain.Sort;

import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;

@Builder
public record ScosPaginationFilterDTO(
        Integer page,
        Integer sizePerPage,
        Sort.Direction direction,
        String order
) implements Serializable {
    @Serial
    private static final long serialVersionUID = 7366806307844227277L;

    public int getPage() {
        return Objects.isNull(page) ? 1 : page;
    }

    public int getSizePerPage() {
        return Objects.isNull(sizePerPage) ? 10 : sizePerPage;
    }

    public Sort.Direction getDirection() {
        return Objects.isNull(direction) ? Sort.Direction.ASC : direction;
    }
}
