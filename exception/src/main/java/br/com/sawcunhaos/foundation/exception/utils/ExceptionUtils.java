package br.com.sawcunhaos.foundation.exception.utils;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.springframework.context.support.DefaultMessageSourceResolvable;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ExceptionUtils {

    public static List<String> findValuesAnnotation(Annotation annotation){
        List<String> values = new ArrayList<>();
        if(annotation instanceof Max max){
            values.add(String.valueOf(max.value()));
        }
        if(annotation instanceof Min min){
            values.add(String.valueOf(min.value()));
        }
        if(annotation instanceof Size size){
            values.add(String.valueOf(size.min()));
            values.add(String.valueOf(size.max()));
        }
        return values;
    }

    public static List<Object> getArgsValidation(Object[] arguments) {

        return Arrays.stream(arguments)
                .map(object -> {
                    if(object instanceof DefaultMessageSourceResolvable defaultMessageSourceResolvable) {
                        return defaultMessageSourceResolvable.getDefaultMessage();
                    }
                    return object;
                })
                .toList();

    }
}
