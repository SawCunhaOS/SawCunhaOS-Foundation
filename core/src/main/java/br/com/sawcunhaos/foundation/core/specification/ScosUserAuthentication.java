
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

/**
 * Bridges the consuming application's authentication mechanism (e.g. Spring
 * Security) to {@code core}, which cannot depend on Spring itself. Implemented
 * by the consumer and injected into foundation code that needs to know "who is
 * doing this" — e.g. {@code audit.ScosHibernateAuditListener} and
 * {@code audit.ScosAuditServiceBean} call this to fill the {@code user} field
 * of every audit record.
 *
 * @since 1.2.0
 */
public interface ScosUserAuthentication {

    /**
     * The identifier of the currently authenticated user.
     *
     * @return the authenticated user's identifier; implementations should not
     *         return {@code null} while there is an authenticated principal
     */
    String findUserAuthentication();
}
