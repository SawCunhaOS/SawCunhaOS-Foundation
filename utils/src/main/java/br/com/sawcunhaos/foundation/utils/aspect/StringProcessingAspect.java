package br.com.sawcunhaos.foundation.utils.aspect;

import br.com.sawcunhaos.foundation.utils.annotation.normalizestrings.NormalizeStrings;
import br.com.sawcunhaos.foundation.utils.utils.StringFieldUtils;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.Arrays;

@Aspect
@Component
public class StringProcessingAspect {

    @Around("@annotation(br.com.sawcunhaos.foundation.utils.annotation.normalizestrings.NormalizeStrings)")
    public Object handleNormalizeStrings(ProceedingJoinPoint joinPoint) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        NormalizeStrings annotation = method.getAnnotation(NormalizeStrings.class);

        Arrays.stream(joinPoint.getArgs()).forEach(object -> {
            StringFieldUtils.applyTransformation(object, annotation.function());
        });

        return joinPoint.proceed();
    }

}
