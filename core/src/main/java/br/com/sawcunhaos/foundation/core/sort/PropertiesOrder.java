
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

package br.com.sawcunhaos.foundation.core.sort;

/**
 * Translates a client-facing sort-field name into the real entity property to
 * order by, guarding against sorting by an arbitrary/unmapped field.
 *
 * <p>Expected to be implemented by an enum whose constants enumerate the
 * public, client-facing sortable fields, each carrying its mapped entity
 * property (see {@code web.PaginationUtils#createPageable}, which calls
 * {@link #value(String)} on a default instance to resolve the requested
 * order field).
 *
 * @since 1.2.0
 */
public interface PropertiesOrder {

    /**
     * The entity property this constant maps to.
     *
     * @return the entity property name
     */
    String properties();

    /**
     * Resolves {@code name} to the entity property to sort by: looks up the enum
     * constant matching {@code name} and returns its {@link #properties()},
     * falling back to this instance's own {@link #properties()} when {@code name}
     * does not match any known constant.
     *
     * @param name the client-supplied sort-field name to resolve
     * @return the entity property to sort by
     */
    String value(final String name);
}
