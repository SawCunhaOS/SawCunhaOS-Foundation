
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

/**
 * Contract for resolving locale-aware messages, decoupling {@code core} (and
 * anything built on it, such as {@link ExceptionCode}-based error messages)
 * from Spring's {@code MessageSource}.
 *
 * @since 1.2.0
 */
public interface LocaleService {

    /**
     * The locale to resolve messages against (typically the current request's).
     *
     * @return the active locale
     */
    Locale getLocale();

    /**
     * Resolves {@code code} against the message source, interpolating {@code args}.
     *
     * @param code the message code to resolve
     * @param args interpolation arguments for the resolved message
     * @return the resolved, interpolated message
     */
    String getMessage(String code, Object... args);

    /**
     * Same as {@link #getMessage(String, Object...)}, taking the interpolation
     * arguments as a {@link List} instead of varargs.
     *
     * @param code the message code to resolve
     * @param args interpolation arguments for the resolved message
     * @return the resolved, interpolated message
     */
    String getMessage(String code, List<Object> args);

    /**
     * Resolves {@code code} against the message source, returning {@code defaultValue}
     * when no translation is found — natively, without throwing. Callers that need a
     * safe fallback (e.g. an RFC 9457 {@code title}) should use this instead of wrapping
     * {@link #getMessage(String, Object...)} in a try/catch.
     *
     * <p>Unlike {@link #getMessage(String, Object...)}, this method takes no interpolation
     * arguments — it is meant for fixed, non-parameterized lookups.</p>
     *
     * @param code         the message code to resolve
     * @param defaultValue the value to return when {@code code} has no translation
     * @return the resolved message, or {@code defaultValue} if not found
     */
    String getMessageOrDefault(String code, String defaultValue);
}
