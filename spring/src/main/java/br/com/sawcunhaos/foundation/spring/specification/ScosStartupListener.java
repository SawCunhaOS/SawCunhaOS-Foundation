
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

package br.com.sawcunhaos.foundation.spring.specification;

import org.springframework.boot.context.event.ApplicationReadyEvent;

/**
 * Contrato para código da aplicação que deve executar assim que a aplicação estiver totalmente iniciada.
 *
 * <p>Implemente-o como um bean do Spring: {@link br.com.sawcunhaos.foundation.spring.listener.ScosOnStartupListener}
 * consome todo bean {@code ScosStartupListener} (como {@code List<ScosStartupListener>}) e
 * invoca cada um quando {@link ApplicationReadyEvent} é publicado. A ordem de invocação entre
 * as implementações segue a ordem dessa lista injetada (use {@code @Order} nas
 * implementações para controlá-la); uma exceção lançada por uma implementação se propaga e
 * impede que as demais executem.
 */
public interface ScosStartupListener {

    /**
     * Chamado uma vez, depois que a aplicação está pronta para atender requisições.
     *
     * @param event o {@link ApplicationReadyEvent} publicado pelo Spring Boot
     */
    void onStartupSystem(ApplicationReadyEvent event);
}
