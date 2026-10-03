
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
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Saída de cada elo de {@code AnnotationChain}: nome e valor que o campo contribui para a chave.
 *
 * <p>Uma instância sem chave (criada por {@code new KeyValuePair()}) significa "campo excluído da
 * composição da chave".</p>
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class KeyValuePair {
    private String key;
    private Object value;
}
