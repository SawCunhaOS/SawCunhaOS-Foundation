package br.com.sawcunhaos.foundation.utils.utils;

import br.com.sawcunhaos.foundation.utils.enums.StringTransformRule;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.lang.reflect.Field;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class StringFieldUtils {

    public static void applyTransformation(Object object, StringTransformRule rule) {
        if (object == null || rule == null) {
            return;
        }

        Class<?> clazz = object.getClass();
        Field[] fields = clazz.getDeclaredFields();

        for (Field field : fields) {
            if (field.getType().equals(String.class)) {
                field.setAccessible(true);
                try {
                    String originalValue = (String) field.get(object);
                    if (originalValue != null) {
                        String transformedValue = rule.apply(originalValue);
                        field.set(object, transformedValue);
                    }
                } catch (IllegalAccessException e) {
                    throw new RuntimeException("Erro ao acessar o campo: " + field.getName(), e);
                }
            }
        }
    }

    public static void applyUpperCase(Object object) {
        applyTransformation(object, StringTransformRule.UPPER_CASE);
    }

    public static void applyLowerCase(Object object) {
        applyTransformation(object, StringTransformRule.LOWER_CASE);
    }

    public static void applyCamelCase(Object object) {
        applyTransformation(object, StringTransformRule.CAMEL_CASE);
    }

    public static void applyCapitalize(Object object) {
        applyTransformation(object, StringTransformRule.CAPITALIZE);
    }

}
