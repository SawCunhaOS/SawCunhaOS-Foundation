
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

package br.com.sawcunhaos.foundation.jdempotent.core.constant;

import java.util.Arrays;

/**
 *
 * Supported datasource types
 *
 */
public enum RepositoryType {

    /**
     *  Redis config value
     */
    REDIS("redis"),

    /**
     *  Hazelcast config value
     */
    HAZELCAST("hazelcast"),

    /**
     * Default config
     */
    INMEMORY("default");

    private String value;

    RepositoryType(String value) {
        this.value = value;
    }

    public String value() {
        return value;
    }

    /**
     * return 
     * 
     * @param repositoryName
     * @return
     */
    public static RepositoryType getRepositoryTypeByValue(String repositoryName){
        return Arrays.stream(values()).filter(repositoryType -> repositoryName.equalsIgnoreCase(repositoryType.value)).findAny().orElse(RepositoryType.INMEMORY);
    }
}
