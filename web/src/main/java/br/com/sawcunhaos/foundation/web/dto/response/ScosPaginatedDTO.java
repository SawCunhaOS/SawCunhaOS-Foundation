
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

package br.com.sawcunhaos.foundation.web.dto.response;

import lombok.Builder;

import java.io.Serial;
import java.io.Serializable;

@Builder
public record ScosPaginatedDTO(
        int sizePerPage,
        int totalPages,
        long totalElements,
        long totalElementsPerPage
) implements Serializable {
    @Serial
    private static final long serialVersionUID = 1538642283241571187L;
}
