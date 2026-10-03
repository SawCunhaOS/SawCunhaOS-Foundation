
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

package br.com.sawcunhaos.foundation.spring.aspect;

import br.com.sawcunhaos.foundation.core.utils.StringFieldUtils;
import br.com.sawcunhaos.foundation.spring.annotation.normalizestrings.NormalizeStrings;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.Arrays;

/**
 * Aspecto por trás de {@link NormalizeStrings}: aplica a
 * {@link br.com.sawcunhaos.foundation.spring.enums.StringTransformRule} escolhida aos argumentos de um
 * método anotado e, em seguida, prossegue com a chamada.
 */
@Aspect
@Component
public class StringProcessingAspect {

    /**
     * Transforma os campos {@code String} de cada argumento no próprio objeto e, em seguida, prossegue.
     *
     * <p>Escopo da transformação (delegada a {@code StringFieldUtils.applyTransformation}):
     * apenas campos {@code String} declarados diretamente na classe do argumento são alterados —
     * campos herdados, objetos aninhados e coleções são ignorados, e um argumento que é
     * ele próprio uma {@code String} é imutável e, portanto, não é normalizado. Os campos são escritos
     * por reflexão, sem passar pelos setters.
     *
     * @param joinPoint a chamada interceptada a um método anotado com {@link NormalizeStrings}
     * @return o que quer que o método interceptado retorne
     * @throws Throwable qualquer exceção lançada pelo método interceptado
     */
    @Around("@annotation(br.com.sawcunhaos.foundation.spring.annotation.normalizestrings.NormalizeStrings)")
    public Object handleNormalizeStrings(ProceedingJoinPoint joinPoint) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        NormalizeStrings annotation = method.getAnnotation(NormalizeStrings.class);

        // Altera antes do proceed(): o método alvo deve enxergar os valores normalizados.
        // A anotação é lida do método resolvido, não do join point, para obter a regra escolhida pelo chamador.
        Arrays.stream(joinPoint.getArgs()).forEach(object -> {
            StringFieldUtils.applyTransformation(object, annotation.function()::apply);
        });

        return joinPoint.proceed();
    }

}
