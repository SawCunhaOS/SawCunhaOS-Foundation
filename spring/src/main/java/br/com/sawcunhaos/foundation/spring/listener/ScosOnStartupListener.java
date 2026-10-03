
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

package br.com.sawcunhaos.foundation.spring.listener;

import br.com.sawcunhaos.foundation.spring.specification.ScosStartupListener;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;

/**
 * Despacha {@link ApplicationReadyEvent} para todo bean {@link ScosStartupListener} do contexto.
 *
 * <p>Este é o único {@code ApplicationListener} do Spring do módulo: o código consumidor implementa
 * {@link ScosStartupListener} em vez de registrar seu próprio listener, e esta classe distribui o
 * evento a todos eles, delimitado por uma linha de log {@code Init}/{@code Final}.
 *
 * <p>Registrado apenas via component scan — o módulo não traz auto-configuração, então a
 * aplicação consumidora deve escanear {@code br.com.sawcunhaos.foundation.spring}.
 */
@Component
@Order(1)
@Log4j2
public class ScosOnStartupListener implements ApplicationListener<ApplicationReadyEvent> {

    /**
     * Todos os beans {@link ScosStartupListener}. {@code required = false}: o Spring deixa este campo
     * {@code null} (e não uma lista vazia) quando a aplicação não declara nenhum, daí a verificação de nulo
     * em {@link #onApplicationEvent(ApplicationReadyEvent)}.
     */
    @Autowired(required = false)
    private List<ScosStartupListener> scosStartupListener;

    /**
     * Invoca {@link ScosStartupListener#onStartupSystem(ApplicationReadyEvent)} em cada
     * listener registrado; não faz nada se não houver nenhum.
     *
     * @param event o {@link ApplicationReadyEvent} a encaminhar
     */
    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        log.info("InsideSoftwaresOnStartupListener#onApplicationEvent() - Init");
        if(Objects.nonNull(scosStartupListener)) scosStartupListener.forEach(scosStartupListener1 -> scosStartupListener1.onStartupSystem(event));
        log.info("InsideSoftwaresOnStartupListener#onApplicationEvent() - Final");
    }

}
