package br.com.sawcunhaos.foundation.utils.enums;

import com.google.common.base.CaseFormat;
import lombok.Getter;
import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.stream.Collectors;

@Getter
public enum StringTransformRule {

    CAMEL_CASE {
        @Override
        public String apply(String value) {
            if (value == null || value.isEmpty()) {
                return value;
            }

            return CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_CAMEL, value.toLowerCase());
        }
    },
    UPPER_CASE {
        @Override
        public String apply(String value) {
            return value != null ? value.toUpperCase() : null;
        }
    },
    LOWER_CASE {
        @Override
        public String apply(String value) {
            return value != null ? value.toLowerCase() : null;
        }
    },
    CAPITALIZE {
        @Override
        public String apply(String value) {
            if (value == null || value.isEmpty()) {
                return value;
            }
            return Arrays.stream(value.split("\\s+"))
                    .map(word -> StringUtils.capitalize(word.toLowerCase()))
                    .collect(Collectors.joining(" "));
        }
    };

    public abstract String apply(String value);

}
