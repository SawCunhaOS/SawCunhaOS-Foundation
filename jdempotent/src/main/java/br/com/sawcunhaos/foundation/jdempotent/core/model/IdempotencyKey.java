
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

package br.com.sawcunhaos.foundation.jdempotent.core.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 *
 * Wraps the combine of application name, listener name and incoming event value hash
 *
 */
@Getter
@Data
@NoArgsConstructor
@AllArgsConstructor
public class IdempotencyKey implements Serializable {

    private String keyValue;

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime + keyValue.hashCode();
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (!(obj instanceof IdempotencyKey)) {
            return false;
        }
        IdempotencyKey other = (IdempotencyKey) obj;
        if (keyValue == null) {
            if (other.keyValue != null) {
                return false;
            }
        } else if (!keyValue.equals(other.keyValue)) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return String.format("IdempotencyKey [keyValue=%s]", keyValue);
    }
}
