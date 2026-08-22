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

package br.com.sawcunhaos.foundation.audit.service;

import br.com.sawcunhaos.foundation.audit.domain.entity.Country;
import br.com.sawcunhaos.foundation.audit.domain.repository.CountryRepository;
import br.com.sawcunhaos.foundation.audit.api.AuditAction;
import br.com.sawcunhaos.foundation.audit.api.Auditable;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CountryReadService {

    private final CountryRepository countryRepository;

    @Auditable(action = AuditAction.READ, entity = "SFA_COUNTRY", idEntitySpEL = "#id.toString()")
    public Optional<Country> findById(UUID id) {
        return countryRepository.findById(id);
    }

    public Optional<Country> findByIdWithoutAudit(UUID id) {
        return countryRepository.findById(id);
    }

}
