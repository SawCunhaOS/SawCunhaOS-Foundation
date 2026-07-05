
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

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.dataformat.xml.XmlMapper;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class JacksonXmlUtils {

    private static final XmlMapper XML_MAPPER = XmlMapper.xmlBuilder()
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            .disable(DeserializationFeature.FAIL_ON_IGNORED_PROPERTIES)
            .disable(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES)
            .build();

    @SuppressFBWarnings(value = "MS_EXPOSE_REP",
            justification = "tools.jackson XmlMapper (Jackson 3) is immutable and thread-safe; the shared instance is meant to be reused")
    public static XmlMapper getInstance() {
        return XML_MAPPER;
    }

}
