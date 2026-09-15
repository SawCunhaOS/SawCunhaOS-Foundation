package br.com.sawcunhaos.foundation.core.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Foundation-wide constant values shared across modules.
 *
 * <p>Modeled as a single-constant enum instead of a plain {@code String} so the
 * value has a stable, typed identity to depend on (rather than every caller
 * hard-coding the literal), and so more cross-cutting constants can be added
 * here later without changing the type consumers reference.
 *
 * @since 1.2.0
 */
@Getter
@RequiredArgsConstructor
public enum Constant {

    /**
     * Name of the HTTP header / MDC key used to correlate a request across logs
     * and audit records (e.g. {@code web.LoggingInitialFilter},
     * {@code web.ScosProblemDetails}, {@code audit.ScosHibernateAuditListener}).
     */
    REQUEST_ID_HEADER("X-Request-ID");

    private final String value;
}
