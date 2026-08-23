
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

package br.com.sawcunhaos.foundation.core.specification;

import java.util.List;
import java.util.Locale;

public interface LocaleService {
    Locale getLocale();
    String getMessage(String code, Object... args);
    String getMessage(String code, List<Object> args);

    /**
     * Resolves {@code code} against the message source, returning {@code defaultValue}
     * when no translation is found — natively, without throwing. Callers that need a
     * safe fallback (e.g. an RFC 9457 {@code title}) should use this instead of wrapping
     * {@link #getMessage(String, Object...)} in a try/catch.
     *
     * <p>Unlike {@link #getMessage(String, Object...)}, this method takes no interpolation
     * arguments — it is meant for fixed, non-parameterized lookups.</p>
     */
    String getMessageOrDefault(String code, String defaultValue);
}
